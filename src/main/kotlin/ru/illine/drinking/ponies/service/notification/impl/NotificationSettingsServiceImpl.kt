package ru.illine.drinking.ponies.service.notification.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.illine.drinking.ponies.builder.SettingBuilder
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.IntervalNotificationType
import ru.illine.drinking.ponies.model.dto.SettingDto
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.model.dto.response.PauseStateResponse
import ru.illine.drinking.ponies.service.notification.NotificationSettingsService
import ru.illine.drinking.ponies.service.notification.NotificationTimeService
import ru.illine.drinking.ponies.util.telegram.TelegramDailyGoalConstants
import java.time.*

@Service
class NotificationSettingsServiceImpl(
    private val notificationAccessService: NotificationAccessService,
    private val notificationTimeService: NotificationTimeService,
    private val clock: Clock,
) : NotificationSettingsService {

    private val logger = LoggerFactory.getLogger("SERVICE")

    override fun getNextNotificationAt(telegramUserId: Long): Instant {
        logger.info("Getting next notification time for telegram user [$telegramUserId]")
        val settings = notificationAccessService.findNotificationSettingByTelegramUserId(telegramUserId)
        return notificationTimeService.calculateNextNotificationAt(settings)
    }

    override fun getAllSettings(telegramUserId: Long): SettingDto {
        logger.info("Getting all notification settings for telegram user [$telegramUserId]")

        val active = notificationAccessService.isEnabledNotifications(telegramUserId)
        if (!active) {
            logger.debug("Not enabled notifications for telegramUserId [$telegramUserId], returning empty settings")
            return SettingDto(notificationActive = false)
        }

        logger.debug("Settings for telegramUserId [$telegramUserId] has been enabled")
        return getNotificationSettings(telegramUserId).let { SettingBuilder.toDto(it) }
    }

    override fun getNotificationSettings(telegramUserId: Long): NotificationSettingDto {
        logger.info("Getting notification settings for telegram user [$telegramUserId]")
        return notificationAccessService.findNotificationSettingByTelegramUserId(telegramUserId)
    }

    override fun getAllNotificationSettings(): Collection<NotificationSettingDto> {
        logger.info("Getting all notification settings")
        return notificationAccessService.findAllNotificationSettings()
    }

    override fun getQuietMode(telegramUserId: Long): Pair<LocalTime, LocalTime> {
        logger.info("Getting quiet mode for telegram user [$telegramUserId]")
        val settings = notificationAccessService.findNotificationSettingByTelegramUserId(telegramUserId)
        val start =
            checkNotNull(settings.quietModeStart) {
                "Quiet mode start is not configured for user [$telegramUserId]"
            }
        val end =
            checkNotNull(settings.quietModeEnd) {
                "Quiet mode end is not configured for user [$telegramUserId]"
            }
        return start to end
    }

    override fun resetNotificationTimer(telegramUserId: Long, time: LocalDateTime): NotificationSettingDto {
        logger.info("Resetting notification timer for telegram user [$telegramUserId] to [$time]")
        return notificationAccessService.updateTimeOfLastNotification(telegramUserId, time)
    }

    override fun changeInterval(
        telegramUserId: Long,
        notificationInterval: IntervalNotificationType
    ): NotificationSettingDto {
        logger.info("Changing notification interval for telegram user [$telegramUserId] to [$notificationInterval]")
        return notificationAccessService.updateNotificationSettings(telegramUserId, notificationInterval)
    }

    override fun changeQuietMode(userId: Long, start: LocalTime, end: LocalTime) {
        logger.info("Change time of quiet mode for telegram user [$userId], start: [$start], end: [$end]")
        require(start != end) { "Start must be before end" }
        notificationAccessService.changeQuietMode(userId, start, end)
    }

    override fun disableQuietMode(userId: Long) {
        logger.info("Disabling quiet mode for user [$userId]")
        notificationAccessService.disableQuietMode(userId)
    }

    override fun isEnabledNotifications(telegramUserId: Long): Boolean {
        logger.info("Checking if notifications are enabled for telegram user [$telegramUserId]")
        return notificationAccessService.isEnabledNotifications(telegramUserId)
    }

    override fun changeNotificationStatus(telegramUserId: Long, active: Boolean) {
        logger.info("Changing notification status for telegram user [$telegramUserId] to [$active]")
        if (active) {
            notificationAccessService.enableNotifications(telegramUserId)
        } else {
            notificationAccessService.disableNotifications(telegramUserId)
        }
    }

    override fun changeTimezone(telegramUserId: Long, timezone: String) {
        logger.info("Changing timezone for telegram user [$telegramUserId] to [$timezone]")
        try {
            ZoneId.of(timezone)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid timezone: $timezone", e)
        }
        notificationAccessService.changeTimezone(telegramUserId, timezone)
    }

    override fun pauseNotifications(telegramUserId: Long, minutes: Long) {
        logger.info("Pausing notifications for telegram user [$telegramUserId] for [$minutes] minutes")
        require(minutes > 0) { "Pause duration must be positive: $minutes" }
        val pauseUntil = LocalDateTime.now(clock).plusMinutes(minutes)
        notificationAccessService.setPause(telegramUserId, pauseUntil)
    }

    override fun cancelPause(telegramUserId: Long) {
        logger.info("Cancelling pause for telegram user [$telegramUserId]")
        notificationAccessService.setPause(telegramUserId, null)
    }

    override fun changeDailyGoal(telegramUserId: Long, goalMl: Int) {
        logger.info("Changing daily goal for telegram user [$telegramUserId] to [$goalMl] ml")
        require(goalMl in TelegramDailyGoalConstants.ALLOWED_VALUES_ML) {
            "Daily goal must be one of ${TelegramDailyGoalConstants.ALLOWED_VALUES_ML} ml, got: $goalMl"
        }
        notificationAccessService.updateDailyGoal(telegramUserId, goalMl)
    }

    override fun getPauseState(telegramUserId: Long): PauseStateResponse {
        logger.info("Getting pause state for telegram user [$telegramUserId]")
        val settings = notificationAccessService.findNotificationSettingByTelegramUserId(telegramUserId)
        val now = LocalDateTime.now(clock)
        val activePauseUntil = settings.pauseUntil
            ?.takeIf { it.isAfter(now) }
            ?.toInstant(ZoneOffset.UTC)
        return PauseStateResponse(
            paused = activePauseUntil != null,
            pauseUntil = activePauseUntil
        )
    }
}

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
import ru.illine.drinking.ponies.util.statistics.toUtcInstant
import ru.illine.drinking.ponies.util.telegram.TelegramDailyGoalConstants
import java.time.*

@Service
class NotificationSettingsServiceImpl(
    private val notificationAccessService: NotificationAccessService,
    private val notificationTimeService: NotificationTimeService,
    private val clock: Clock,
) : NotificationSettingsService {

    private val logger = LoggerFactory.getLogger("SERVICE")

    override fun getNextNotificationAt(externalUserId: Long): Instant {
        logger.info("Getting next notification time for telegram user [$externalUserId]")
        val settings = notificationAccessService.findNotificationSettingByExternalUserId(externalUserId)
        return notificationTimeService.calculateNextNotificationAt(settings)
    }

    override fun getAllSettings(externalUserId: Long): SettingDto {
        logger.info("Getting all notification settings for telegram user [$externalUserId]")

        val active = notificationAccessService.isEnabledNotifications(externalUserId)
        if (!active) {
            logger.debug("Not enabled notifications for externalUserId [$externalUserId], returning empty settings")
            return SettingDto(notificationActive = false)
        }

        logger.debug("Settings for externalUserId [$externalUserId] has been enabled")
        return getNotificationSettings(externalUserId).let { SettingBuilder.toDto(it) }
    }

    override fun getNotificationSettings(externalUserId: Long): NotificationSettingDto {
        logger.info("Getting notification settings for telegram user [$externalUserId]")
        return notificationAccessService.findNotificationSettingByExternalUserId(externalUserId)
    }

    override fun getAllNotificationSettings(): Collection<NotificationSettingDto> {
        logger.info("Getting all notification settings")
        return notificationAccessService.findAllNotificationSettings()
    }

    override fun getQuietMode(externalUserId: Long): Pair<LocalTime, LocalTime> {
        logger.info("Getting quiet mode for telegram user [$externalUserId]")
        val settings = notificationAccessService.findNotificationSettingByExternalUserId(externalUserId)
        val start =
            checkNotNull(settings.quietModeStart) {
                "Quiet mode start is not configured for user [$externalUserId]"
            }
        val end =
            checkNotNull(settings.quietModeEnd) {
                "Quiet mode end is not configured for user [$externalUserId]"
            }
        return start to end
    }

    override fun resetNotificationTimer(externalUserId: Long, time: LocalDateTime): NotificationSettingDto {
        logger.info("Resetting notification timer for telegram user [$externalUserId] to [$time]")
        return notificationAccessService.updateTimeOfLastNotification(externalUserId, time)
    }

    override fun changeInterval(
        externalUserId: Long,
        notificationInterval: IntervalNotificationType
    ): NotificationSettingDto {
        logger.info("Changing notification interval for telegram user [$externalUserId] to [$notificationInterval]")
        return notificationAccessService.updateNotificationSettings(externalUserId, notificationInterval)
    }

    override fun changeQuietMode(externalUserId: Long, start: LocalTime, end: LocalTime) {
        logger.info("Change time of quiet mode for telegram user [$externalUserId], start: [$start], end: [$end]")
        require(start != end) { "Start must be before end" }
        notificationAccessService.changeQuietMode(externalUserId, start, end)
    }

    override fun disableQuietMode(externalUserId: Long) {
        logger.info("Disabling quiet mode for user [$externalUserId]")
        notificationAccessService.disableQuietMode(externalUserId)
    }

    override fun isEnabledNotifications(externalUserId: Long): Boolean {
        logger.info("Checking if notifications are enabled for telegram user [$externalUserId]")
        return notificationAccessService.isEnabledNotifications(externalUserId)
    }

    override fun changeNotificationStatus(externalUserId: Long, active: Boolean) {
        logger.info("Changing notification status for telegram user [$externalUserId] to [$active]")
        if (active) {
            notificationAccessService.enableNotifications(externalUserId)
        } else {
            notificationAccessService.disableNotifications(externalUserId)
        }
    }

    override fun changeTimezone(externalUserId: Long, timezone: String) {
        logger.info("Changing timezone for telegram user [$externalUserId] to [$timezone]")
        try {
            ZoneId.of(timezone)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid timezone: $timezone", e)
        }
        notificationAccessService.changeTimezone(externalUserId, timezone)
    }

    override fun pauseNotifications(externalUserId: Long, minutes: Long) {
        logger.info("Pausing notifications for telegram user [$externalUserId] for [$minutes] minutes")
        require(minutes > 0) { "Pause duration must be positive: $minutes" }
        val pauseUntil = LocalDateTime.now(clock).plusMinutes(minutes)
        notificationAccessService.setPause(externalUserId, pauseUntil)
    }

    override fun cancelPause(externalUserId: Long) {
        logger.info("Cancelling pause for telegram user [$externalUserId]")
        notificationAccessService.setPause(externalUserId, null)
    }

    override fun changeDailyGoal(externalUserId: Long, goalMl: Int) {
        logger.info("Changing daily goal for telegram user [$externalUserId] to [$goalMl] ml")
        require(goalMl in TelegramDailyGoalConstants.ALLOWED_VALUES_ML) {
            "Daily goal must be one of ${TelegramDailyGoalConstants.ALLOWED_VALUES_ML} ml, got: $goalMl"
        }
        notificationAccessService.updateDailyGoal(externalUserId, goalMl)
    }

    override fun getPauseState(externalUserId: Long): PauseStateResponse {
        logger.info("Getting pause state for telegram user [$externalUserId]")
        val settings = notificationAccessService.findNotificationSettingByExternalUserId(externalUserId)
        val now = LocalDateTime.now(clock)
        val activePauseUntil = settings.pauseUntil
            ?.takeIf { it.isAfter(now) }
            ?.toUtcInstant()
        return PauseStateResponse(
            paused = activePauseUntil != null,
            pauseUntil = activePauseUntil
        )
    }
}

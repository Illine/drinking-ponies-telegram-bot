package ru.illine.drinking.ponies.dao.access.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.illine.drinking.ponies.builder.NotificationSettingBuilder
import ru.illine.drinking.ponies.builder.TelegramChatBuilder
import ru.illine.drinking.ponies.builder.TelegramUserBuilder
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.dao.repository.NotificationSettingRepository
import ru.illine.drinking.ponies.dao.repository.TelegramChatRepository
import ru.illine.drinking.ponies.dao.repository.TelegramUserRepository
import ru.illine.drinking.ponies.exception.NotificationSettingsNotFoundException
import ru.illine.drinking.ponies.model.base.IntervalNotificationType
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramChatDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserDto
import ru.illine.drinking.ponies.model.entity.NotificationSettingEntity
import java.time.Clock
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class NotificationAccessServiceImpl(
    private val settingRepository: NotificationSettingRepository,
    private val userRepository: TelegramUserRepository,
    private val chatRepository: TelegramChatRepository,
    private val clock: Clock,
) : NotificationAccessService {

    private val logger = LoggerFactory.getLogger("ACCESS-SERVICE")

    @Transactional(readOnly = true)
    override fun findAllNotificationSettings(): Set<NotificationSettingDto> {
        logger.debug("Finding all notification setting records")

        return settingRepository.findAll()
            .map {
                val user = TelegramUserBuilder.toDto(it.telegramUser)
                val chat = TelegramChatBuilder.toDto(it.telegramChat, user)
                NotificationSettingBuilder.toDto(it, user, chat)
            }
            .toSet()
    }

    @Transactional(readOnly = true)
    override fun findNotificationSettingByTelegramUserId(telegramUserId: Long): NotificationSettingDto {
        logger.debug("Finding a Notification by telegramUserId [$telegramUserId]")

        return requireSettings(telegramUserId).let {
            val user = TelegramUserBuilder.toDto(it.telegramUser)
            val chat = TelegramChatBuilder.toDto(it.telegramChat, user)
            NotificationSettingBuilder.toDto(it, user, chat)
        }
    }

    @Transactional(readOnly = true)
    override fun existsByTelegramUserId(telegramUserId: Long): Boolean {
        logger.debug("Does a notification exist for the telegramUserId: [$telegramUserId]?")
        return userRepository.existsByExternalUserId(telegramUserId)
    }

    @Transactional
    override fun save(
        user: TelegramUserDto,
        chat: TelegramChatDto,
        setting: NotificationSettingDto
    ): TelegramUserDto {
        logger.debug("Saving a new user, chat and setting for a telegram user: [${user.externalUserId}]")

        val externalUserId = user.externalUserId
        val externalChatId = chat.externalChatId

        val userEntity =
            userRepository.findByExternalUserId(
                externalUserId
            ) ?: TelegramUserBuilder.toEntity(user)

        val chatEntity =
            chatRepository.findByExternalChatId(externalChatId) ?: TelegramChatBuilder.toEntity(chat, userEntity)

        val settingEntity =
            settingRepository.findByTelegramUser_ExternalUserId(
                externalUserId
            ) ?: NotificationSettingBuilder.toEntity(setting, userEntity, chatEntity)

        userEntity.addTelegramChat(chatEntity)
        userEntity.notificationSettings = settingEntity

        return userRepository.save(userEntity).let { TelegramUserBuilder.toDto(it) }
    }

    @Transactional
    override fun updateNotificationSettings(
        telegramUserId: Long, notificationInterval: IntervalNotificationType
    ): NotificationSettingDto {
        logger.debug("The Notification Settings will be updated for an existed entity by id: [${telegramUserId}]")

        val settings = requireSettings(telegramUserId)

        if (settings.notificationInterval != notificationInterval) {
            // Reset timer so the new interval counts from now, not from the last notification time.
            // Also clear active pause - changing interval implicitly cancels the pause.
            settings.apply {
                this.notificationInterval = notificationInterval
                this.timeOfLastNotification = LocalDateTime.now(clock)
                this.notificationAttempts = 0
                this.pauseUntil = null
            }.let { settingRepository.save(it) }
        }

        return settings.let {
            val user = TelegramUserBuilder.toDto(it.telegramUser)
            val chat = TelegramChatBuilder.toDto(it.telegramChat, user)
            NotificationSettingBuilder.toDto(it, user, chat)
        }
    }

    @Transactional
    override fun enableNotifications(telegramUserId: Long) {
        logger.debug("A notification settings will be enabled (enabled = true) by telegramUserId [$telegramUserId]")
        settingRepository.switchEnabled(telegramUserId, true)
    }

    @Transactional
    override fun disableNotifications(telegramUserId: Long) {
        logger.debug("A notification settings will be disabled (enabled = false) by telegramUserId [$telegramUserId]")
        settingRepository.clearPause(telegramUserId)
        settingRepository.switchEnabled(telegramUserId, false)
    }

    @Transactional
    override fun updateNotificationSettings(settings: Collection<NotificationSettingDto>): Set<NotificationSettingDto> {
        logger.debug("Updating of notification settings...")

        return settings
            .map {
                val user = TelegramUserBuilder.toEntity(it.telegramUser)
                val chat = TelegramChatBuilder.toEntity(it.telegramChat, user)
                NotificationSettingBuilder.toEntity(it, user, chat)
            }
            .apply { settingRepository.saveAll(this) }
            .map {
                val user = TelegramUserBuilder.toDto(it.telegramUser)
                val chat = TelegramChatBuilder.toDto(it.telegramChat, user)
                NotificationSettingBuilder.toDto(it, user, chat)
            }
            .toSet()
    }

    @Transactional
    override fun updateTimeOfLastNotification(telegramUserId: Long, time: LocalDateTime): NotificationSettingDto {
        logger.debug("Updating a time of last notification by telegramUserId [$telegramUserId]")

        val setting = requireSettings(telegramUserId)

        return setting
            .apply {
                timeOfLastNotification = time
                notificationAttempts = 0
            }.let {
                settingRepository.save(it)
            }.let {
                val user = TelegramUserBuilder.toDto(it.telegramUser)
                val chat = TelegramChatBuilder.toDto(it.telegramChat, user)
                NotificationSettingBuilder.toDto(it, user, chat)
            }
    }

    @Transactional(readOnly = true)
    override fun isEnabledNotifications(telegramUserId: Long): Boolean {
        logger.debug("Checking if notifications are active by telegramUserId: [$telegramUserId]")
        return settingRepository.isEnabledByTelegramUserId(telegramUserId)
    }

    @Transactional
    override fun changeQuietMode(userId: Long, start: LocalTime, end: LocalTime) {
        logger.debug("The quiet mod will be updated for a user [{}], start time: [{}], end time: [{}]", userId, start, end)
        settingRepository.clearPause(userId)
        settingRepository.updateQuietMode(userId, start, end)
    }

    @Transactional
    override fun disableQuietMode(userId: Long) {
        logger.debug("The quiet mod will be disabled for a user [$userId]")
        settingRepository.clearPause(userId)
        settingRepository.updateQuietMode(userId)
    }

    @Transactional
    override fun changeTimezone(telegramUserId: Long, timezone: String) {
        logger.debug("Changing timezone for user [$telegramUserId] to [$timezone]")
        val user = requireNotNull(
            userRepository.findByExternalUserId(telegramUserId),
            { "Not found a Telegram User by telegramUserId [$telegramUserId]" }
        )
        user.userTimeZone = timezone
        userRepository.save(user)
    }

    @Transactional
    override fun setPause(telegramUserId: Long, pauseUntil: LocalDateTime?): NotificationSettingDto {
        logger.debug("Setting pause for telegramUserId [$telegramUserId] to [$pauseUntil]")

        val setting = requireSettings(telegramUserId)
        val now = LocalDateTime.now(clock)

        return setting
            .apply {
                if (pauseUntil != null) {
                    // Pause: shift timeOfLastNotification so scheduler stays silent until pauseUntil.
                    this.pauseUntil = pauseUntil
                    this.timeOfLastNotification = pauseUntil.minusMinutes(setting.notificationInterval.minutes)
                } else {
                    // Cancel: reset timer only if there was an active pause to cancel.
                    val hadActivePause = this.pauseUntil?.isAfter(now) == true
                    this.pauseUntil = null
                    if (hadActivePause) {
                        this.timeOfLastNotification = now
                    }
                }
            }.let {
                settingRepository.save(it)
            }.let {
                val user = TelegramUserBuilder.toDto(it.telegramUser)
                val chat = TelegramChatBuilder.toDto(it.telegramChat, user)
                NotificationSettingBuilder.toDto(it, user, chat)
            }
    }

    @Transactional
    override fun updateDailyGoal(telegramUserId: Long, goalMl: Int) {
        logger.debug("Updating daily goal for telegramUserId [$telegramUserId] to [$goalMl] ml")
        settingRepository.updateDailyGoal(telegramUserId, goalMl)
    }

    private fun requireSettings(telegramUserId: Long): NotificationSettingEntity =
        settingRepository.findByTelegramUser_ExternalUserId(telegramUserId)
            ?: throw NotificationSettingsNotFoundException(
                "Not found a Notification Setting by telegramUserId [$telegramUserId]"
            )
}

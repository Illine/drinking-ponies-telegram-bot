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
    override fun findNotificationSettingByExternalUserId(externalUserId: Long): NotificationSettingDto {
        logger.debug("Finding a Notification by externalUserId [$externalUserId]")

        return requireSettings(externalUserId).let {
            val user = TelegramUserBuilder.toDto(it.telegramUser)
            val chat = TelegramChatBuilder.toDto(it.telegramChat, user)
            NotificationSettingBuilder.toDto(it, user, chat)
        }
    }

    @Transactional(readOnly = true)
    override fun existsByExternalUserId(externalUserId: Long): Boolean {
        logger.debug("Does a notification exist for the externalUserId: [$externalUserId]?")
        return userRepository.existsByExternalUserId(externalUserId)
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
        externalUserId: Long, notificationInterval: IntervalNotificationType
    ): NotificationSettingDto {
        logger.debug("The Notification Settings will be updated for an existed entity by id: [${externalUserId}]")

        val settings = requireSettings(externalUserId)

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
    override fun updateNotificationsEnabled(externalUserId: Long) {
        logger.debug("A notification settings will be enabled (enabled = true) by externalUserId [$externalUserId]")
        settingRepository.switchEnabled(externalUserId, true)
    }

    @Transactional
    override fun updateNotificationsDisabled(externalUserId: Long) {
        logger.debug("A notification settings will be disabled (enabled = false) by externalUserId [$externalUserId]")
        settingRepository.clearPause(externalUserId)
        settingRepository.switchEnabled(externalUserId, false)
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
    override fun updateTimeOfLastNotification(externalUserId: Long, time: LocalDateTime): NotificationSettingDto {
        logger.debug("Updating a time of last notification by externalUserId [$externalUserId]")

        val setting = requireSettings(externalUserId)

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
    override fun findIsEnabledNotificationsByExternalUserId(externalUserId: Long): Boolean {
        logger.debug("Checking if notifications are active by externalUserId: [$externalUserId]")
        return settingRepository.isEnabledByExternalUserId(externalUserId)
    }

    @Transactional
    override fun updateQuietMode(externalUserId: Long, start: LocalTime, end: LocalTime) {
        logger.debug("The quiet mod will be updated for a user [{}], start time: [{}], end time: [{}]", externalUserId, start, end)
        settingRepository.clearPause(externalUserId)
        settingRepository.updateQuietMode(externalUserId, start, end)
    }

    @Transactional
    override fun updateQuietModeDisabled(externalUserId: Long) {
        logger.debug("The quiet mod will be disabled for a user [$externalUserId]")
        settingRepository.clearPause(externalUserId)
        settingRepository.updateQuietMode(externalUserId)
    }

    @Transactional
    override fun updateTimezone(externalUserId: Long, timezone: String) {
        logger.debug("Changing timezone for user [$externalUserId] to [$timezone]")
        val user = requireNotNull(
            userRepository.findByExternalUserId(externalUserId),
            { "Not found a Telegram User by externalUserId [$externalUserId]" }
        )
        user.userTimeZone = timezone
        userRepository.save(user)
    }

    @Transactional
    override fun updatePause(externalUserId: Long, pauseUntil: LocalDateTime?): NotificationSettingDto {
        logger.debug("Setting pause for externalUserId [$externalUserId] to [$pauseUntil]")

        val setting = requireSettings(externalUserId)
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
    override fun updateDailyGoal(externalUserId: Long, goalMl: Int) {
        logger.debug("Updating daily goal for externalUserId [$externalUserId] to [$goalMl] ml")
        settingRepository.updateDailyGoal(externalUserId, goalMl)
    }

    private fun requireSettings(externalUserId: Long): NotificationSettingEntity =
        settingRepository.findByTelegramUser_ExternalUserId(externalUserId)
            ?: throw NotificationSettingsNotFoundException(
                "Not found a Notification Setting by externalUserId [$externalUserId]"
            )
}

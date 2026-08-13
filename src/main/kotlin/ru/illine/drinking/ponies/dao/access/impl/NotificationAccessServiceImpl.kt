package ru.illine.drinking.ponies.dao.access.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.dao.repository.NotificationSettingRepository
import ru.illine.drinking.ponies.dao.repository.TelegramChatRepository
import ru.illine.drinking.ponies.dao.repository.TelegramUserRepository
import ru.illine.drinking.ponies.exception.NotificationSettingsNotFoundException
import ru.illine.drinking.ponies.mapper.NotificationSettingMapper
import ru.illine.drinking.ponies.mapper.TelegramChatMapper
import ru.illine.drinking.ponies.mapper.TelegramUserMapper
import ru.illine.drinking.ponies.model.base.AppLogger
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
    private val logger = AppLogger.ACCESS_SERVICE.logger

    @Transactional(readOnly = true)
    override fun findAllNotificationSettings(): Set<NotificationSettingDto> {
        logger.debug("Finding all notification setting records")

        return settingRepository
            .findAllNotBannedWithUserAndChat()
            .map {
                val user = TelegramUserMapper.toDto(it.telegramUser)
                val chat = TelegramChatMapper.toDto(it.telegramChat, user)
                NotificationSettingMapper.toDto(it, user, chat)
            }.toSet()
    }

    @Transactional(readOnly = true)
    override fun findNotificationSettingByExternalUserId(externalUserId: Long): NotificationSettingDto {
        logger.debug("Finding a Notification by externalUserId [$externalUserId]")

        return requireSettings(externalUserId).let {
            val user = TelegramUserMapper.toDto(it.telegramUser)
            val chat = TelegramChatMapper.toDto(it.telegramChat, user)
            NotificationSettingMapper.toDto(it, user, chat)
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
        setting: NotificationSettingDto,
    ): TelegramUserDto {
        logger.debug("Saving a new user, chat and setting for a telegram user: [${user.externalUserId}]")

        val externalUserId = user.externalUserId
        val externalChatId = chat.externalChatId

        val userEntity =
            userRepository.findByExternalUserId(
                externalUserId,
            ) ?: TelegramUserMapper.toNewEntity(user)

        val chatEntity =
            chatRepository.findByExternalChatId(externalChatId) ?: TelegramChatMapper.toNewEntity(chat, userEntity)

        val settingEntity =
            settingRepository.findByTelegramUser_ExternalUserId(
                externalUserId,
            ) ?: NotificationSettingMapper.toNewEntity(setting, userEntity, chatEntity)

        userEntity.addTelegramChat(chatEntity)
        userEntity.notificationSettings = settingEntity

        return userRepository.save(userEntity).let { TelegramUserMapper.toDto(it) }
    }

    @Transactional
    override fun updateNotificationSettings(
        externalUserId: Long,
        notificationInterval: IntervalNotificationType,
    ): NotificationSettingDto {
        logger.debug("The Notification Settings will be updated for an existed entity by id: [$externalUserId]")

        val settings = requireSettings(externalUserId)

        if (settings.notificationInterval != notificationInterval) {
            settings
                .apply {
                    this.notificationInterval = notificationInterval
                    this.timeOfLastNotification = LocalDateTime.now(clock)
                    this.notificationAttempts = 0
                    this.pauseUntil = null
                }.let { settingRepository.save(it) }
        }

        return settings.let {
            val user = TelegramUserMapper.toDto(it.telegramUser)
            val chat = TelegramChatMapper.toDto(it.telegramChat, user)
            NotificationSettingMapper.toDto(it, user, chat)
        }
    }

    @Transactional
    override fun updateNotificationsEnabled(externalUserId: Long) {
        logger.debug("A notification settings will be enabled (enabled = true) by externalUserId [$externalUserId]")
        settingRepository.clearPause(externalUserId, LocalDateTime.now(clock))
        settingRepository.switchEnabled(externalUserId, true)
    }

    @Transactional
    override fun updateNotificationsDisabled(externalUserId: Long) {
        logger.debug("A notification settings will be disabled (enabled = false) by externalUserId [$externalUserId]")
        settingRepository.clearPause(externalUserId, LocalDateTime.now(clock))
        settingRepository.switchEnabled(externalUserId, false)
    }

    @Transactional
    override fun recordMailingResults(settings: Collection<NotificationSettingDto>) {
        if (settings.isEmpty()) return

        logger.debug("Recording the results of a mailing round for [{}] users", settings.size)

        val byExternalUserId =
            settingRepository
                .findAllWithUserAndChatByExternalUserIdIn(settings.map { it.telegramUser.externalUserId })
                .associateBy { it.telegramUser.externalUserId }

        settings.forEach { dto ->
            val stored = byExternalUserId[dto.telegramUser.externalUserId]
            if (stored == null) {
                logger.warn("Nothing to record: settings of [{}] are gone", dto.telegramUser.externalUserId)
                return@forEach
            }

            stored.timeOfLastNotification = dto.timeOfLastNotification
            stored.notificationAttempts = dto.notificationAttempts
            stored.telegramChat.previousNotificationMessageId = dto.telegramChat.previousNotificationMessageId
        }
    }

    @Transactional
    override fun updateTimeOfLastNotification(
        externalUserId: Long,
        time: LocalDateTime,
    ): NotificationSettingDto {
        logger.debug("Updating a time of last notification by externalUserId [$externalUserId]")

        val setting = requireSettings(externalUserId)

        return setting
            .apply {
                timeOfLastNotification = time
                notificationAttempts = 0
            }.let {
                settingRepository.save(it)
            }.let {
                val user = TelegramUserMapper.toDto(it.telegramUser)
                val chat = TelegramChatMapper.toDto(it.telegramChat, user)
                NotificationSettingMapper.toDto(it, user, chat)
            }
    }

    @Transactional(readOnly = true)
    override fun findIsEnabledNotificationsByExternalUserId(externalUserId: Long): Boolean {
        logger.debug("Checking if notifications are active by externalUserId: [$externalUserId]")
        return settingRepository.isEnabledByExternalUserId(externalUserId)
    }

    @Transactional
    override fun updateQuietMode(
        externalUserId: Long,
        start: LocalTime,
        end: LocalTime,
    ) {
        logger.debug(
            "The quiet mod will be updated for a user [{}], start time: [{}], end time: [{}]",
            externalUserId,
            start,
            end,
        )
        settingRepository.clearPause(externalUserId, LocalDateTime.now(clock))
        settingRepository.updateQuietMode(externalUserId, start, end)
    }

    @Transactional
    override fun updateQuietModeDisabled(externalUserId: Long) {
        logger.debug("The quiet mod will be disabled for a user [$externalUserId]")
        settingRepository.clearPause(externalUserId, LocalDateTime.now(clock))
        settingRepository.updateQuietMode(externalUserId)
    }

    @Transactional
    override fun updateTimezone(
        externalUserId: Long,
        timezone: String,
    ) {
        logger.debug("Changing timezone for user [$externalUserId] to [$timezone]")
        val user =
            requireNotNull(
                userRepository.findByExternalUserId(externalUserId),
                { "Not found a Telegram User by externalUserId [$externalUserId]" },
            )
        user.userTimeZone = timezone
        userRepository.save(user)
    }

    @Transactional
    override fun updatePause(
        externalUserId: Long,
        pauseUntil: LocalDateTime?,
    ): NotificationSettingDto {
        logger.debug("Setting pause for externalUserId [$externalUserId] to [$pauseUntil]")

        val setting = requireSettings(externalUserId)
        val now = LocalDateTime.now(clock)

        return setting
            .apply {
                if (pauseUntil != null) {
                    this.pauseUntil = pauseUntil
                    this.timeOfLastNotification = pauseUntil.minusMinutes(setting.notificationInterval.minutes)
                } else {
                    val hadActivePause = this.pauseUntil?.isAfter(now) == true
                    this.pauseUntil = null
                    if (hadActivePause) {
                        this.timeOfLastNotification = now
                    }
                }
            }.let {
                settingRepository.save(it)
            }.let {
                val user = TelegramUserMapper.toDto(it.telegramUser)
                val chat = TelegramChatMapper.toDto(it.telegramChat, user)
                NotificationSettingMapper.toDto(it, user, chat)
            }
    }

    @Transactional
    override fun updateDailyGoal(
        externalUserId: Long,
        goalMl: Int,
    ) {
        logger.debug("Updating daily goal for externalUserId [$externalUserId] to [$goalMl] ml")
        settingRepository.updateDailyGoal(externalUserId, goalMl)
    }

    private fun requireSettings(externalUserId: Long): NotificationSettingEntity =
        settingRepository.findByTelegramUser_ExternalUserId(externalUserId)
            ?: throw NotificationSettingsNotFoundException(
                "Not found a Notification Setting by externalUserId [$externalUserId]",
            )
}

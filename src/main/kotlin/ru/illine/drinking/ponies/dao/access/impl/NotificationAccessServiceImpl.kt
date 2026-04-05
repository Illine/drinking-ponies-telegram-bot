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
import ru.illine.drinking.ponies.model.base.IntervalNotificationType
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramChatDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserDto
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class NotificationAccessServiceImpl(
    private val settingRepository: NotificationSettingRepository,
    private val userRepository: TelegramUserRepository,
    private val chatRepository: TelegramChatRepository,
) : NotificationAccessService {

    private val logger = LoggerFactory.getLogger("ACCESS-SERVICE")

    @Transactional
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

    @Transactional
    override fun findNotificationSettingByTelegramUserId(telegramUserId: Long): NotificationSettingDto {
        logger.debug("Finding a Notification by telegramUserId [$telegramUserId]")

        return requireNotNull(
            settingRepository.findByTelegramUser_ExternalUserId(telegramUserId),
            { "Not found a Notification Setting by telegramUserId [$telegramUserId]" }
        ).let {
            val user = TelegramUserBuilder.toDto(it.telegramUser)
            val chat = TelegramChatBuilder.toDto(it.telegramChat, user)
            NotificationSettingBuilder.toDto(it, user, chat)
        }
    }

    @Transactional
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
        telegramUserId: Long,
        telegramChatId: Long,
        notificationInterval: IntervalNotificationType
    ): NotificationSettingDto {
        logger.debug("The Notification Settings will be updated for an existed entity by id: [${telegramUserId}]")

        val setting = requireNotNull(
            settingRepository.findByTelegramUser_ExternalUserId(telegramUserId),
            { "Not found a Notification Settings by telegramUserId: [$telegramUserId], telegramChatId: [$telegramChatId]" }
        )

        if (setting.notificationInterval != notificationInterval) {
            setting.notificationInterval = notificationInterval
            settingRepository.save(setting)
        }

        return setting.let {
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

        val setting = requireNotNull(
            settingRepository.findByTelegramUser_ExternalUserId(telegramUserId),
            { "Not found a Notification Setting by telegramUserId [$telegramUserId]" }
        )

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

    @Transactional
    override fun isEnabledNotifications(telegramUserId: Long): Boolean {
        logger.debug("Checking if notifications are active by telegramUserId: [$telegramUserId]")
        return settingRepository.isEnabledByTelegramUserId(telegramUserId)
    }

    @Transactional
    override fun changeQuietMode(userId: Long, start: LocalTime, end: LocalTime) {
        logger.debug("The quiet mod will be updated for a user [{}], start time: [{}], end time: [{}]", userId, start, end)
        settingRepository.updateQuietMode(userId, start, end)
    }

    @Transactional
    override fun disableQuietMode(userId: Long) {
        logger.debug("The quiet mod will be disabled for a user [$userId]")
        settingRepository.updateQuietMode(userId)
    }
}
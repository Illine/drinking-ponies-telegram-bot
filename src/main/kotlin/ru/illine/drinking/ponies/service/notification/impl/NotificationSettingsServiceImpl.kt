package ru.illine.drinking.ponies.service.notification.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.IntervalNotificationType
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.service.notification.NotificationSettingsService
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.util.telegram.TelegramMessageConstants
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class NotificationSettingsServiceImpl(
    private val notificationAccessService: NotificationAccessService,
    private val messageEditorService: MessageEditorService,
    private val sender: TelegramClient
) : NotificationSettingsService {

    private val logger = LoggerFactory.getLogger("SERVICE")

    override fun getNotificationSettings(telegramUserId: Long): NotificationSettingDto {
        logger.info("Getting notification settings for telegram user [$telegramUserId]")
        return notificationAccessService.findNotificationSettingByTelegramUserId(telegramUserId)
    }

    override fun getAllNotificationSettings(): Collection<NotificationSettingDto> {
        logger.info("Getting all notification settings")
        return notificationAccessService.findAllNotificationSettings()
    }

    override fun resetNotificationTimer(telegramUserId: Long, time: LocalDateTime): NotificationSettingDto {
        logger.info("Resetting notification timer for telegram user [$telegramUserId] to [$time]")
        return notificationAccessService.updateTimeOfLastNotification(telegramUserId, time)
    }

    override fun changeInterval(
        telegramUserId: Long,
        telegramChatId: Long,
        notificationInterval: IntervalNotificationType
    ): NotificationSettingDto {
        logger.info("Changing notification interval for telegram user [$telegramUserId] to [$notificationInterval]")
        return notificationAccessService.updateNotificationSettings(telegramUserId, telegramChatId, notificationInterval)
    }

    override fun changeQuietMode(userId: Long, messageId: Int, start: LocalTime, end: LocalTime) {
        logger.info("Change time of quiet mode for telegram user [$userId], start: [$start], end: [$end]")
        require(start != end) { "Start must be before end" }

        notificationAccessService.findNotificationSettingByTelegramUserId(userId)
            .apply {
                notificationAccessService.changeQuietMode(userId, start, end)
                messageEditorService.deleteReplyMarkup(this.telegramChat.externalChatId, messageId)
                SendMessage(
                    this.telegramChat.externalChatId.toString(),
                    TelegramMessageConstants.SETTINGS_QUIET_MODE_TIME_NOTIFICATION_CHANGING.format(start, end)
                ).apply {
                    enableMarkdown(true)
                }.apply { sender.execute(this) }
            }
    }

    override fun disableQuietMode(userId: Long) {
        logger.info("Disabling quiet mode for user [$userId]")
        notificationAccessService.disableQuietMode(userId)
    }
}

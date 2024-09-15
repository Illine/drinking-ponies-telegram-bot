package ru.illine.drinking.ponies.service.app.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.service.MessageEditorService
import ru.illine.drinking.ponies.service.app.NotificationSettingsService
import ru.illine.drinking.ponies.util.TelegramConstants
import java.time.LocalTime

@Service
class NotificationSettingsServiceImpl(
    private val notificationAccessService: NotificationAccessService,
    private val messageEditorService: MessageEditorService,
    private val sender: TelegramClient
) : NotificationSettingsService {

    private val logger = LoggerFactory.getLogger("SERVICE")

    override fun changeQuiteMode(userId: Long, messageId: Int, start: LocalTime, end: LocalTime) {
        logger.info("Change time of quite mode for user [$userId], start: [$start], end: [$end]")
        validateTime(start, end)

        notificationAccessService.findByUserId(userId)
            .apply {
                notificationAccessService.changeQuiteMode(userId, start, end)
                messageEditorService.deleteReplyMarkup(chatId, messageId)
                SendMessage(
                    chatId.toString(),
                    TelegramConstants.SETTINGS_QUIET_MODE_TIME_NOTIFICATION_CHANGING.format(start, end)
                ).apply {
                    enableMarkdown(true)
                }.apply { sender.execute(this) }
            }
    }

    override fun disableQuiteMode(userId: Long) {
        logger.info("Disabling quite mode for user [$userId]")
        notificationAccessService.disableQuietMode(userId)
    }

    private fun validateTime(start: LocalTime, end: LocalTime) {
        logger.debug("Validating start and end times")
        logger.debug("Start [{}] must be after [{}]", start, end)

        require(start.isBefore(end) || start == end) { "Start must be before end" }
    }
}
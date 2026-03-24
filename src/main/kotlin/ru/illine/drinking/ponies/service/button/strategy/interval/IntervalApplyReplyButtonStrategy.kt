package ru.illine.drinking.ponies.service.button.strategy.interval

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.IntervalNotificationType
import ru.illine.drinking.ponies.service.button.ReplyButtonStrategy
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.util.telegram.TelegramMessageConstants

@Service
class IntervalApplyReplyButtonStrategy(
    private val sender: TelegramClient,
    private val notificationAccessService: NotificationAccessService,
    private val messageEditorService: MessageEditorService
) : ReplyButtonStrategy {

    private val log = LoggerFactory.getLogger("REPLY-STRATEGY")

    override fun reply(callbackQuery: CallbackQuery) {
        deleteOldReplyMarkup(callbackQuery)

        val userId = callbackQuery.from.id
        val chatId = callbackQuery.message.chatId
        val queryData = callbackQuery.data

        val notificationInterval = IntervalNotificationType.typeOf(queryData) ?: IntervalNotificationType.TWO_HOURS

        log.info(
            "A telegram user [{}] for telegram chat [{}] with interval setting [{}] will be stored to a database",
            userId,
            chatId,
            notificationInterval
        )

        log.info("A notification settings for user [{}] with interval setting [{}] will be saved", userId, notificationInterval)
        val updatedNotificationSettings =
            notificationAccessService.updateNotificationSettings(userId, chatId, notificationInterval)
        log.info("The notification settings (id: [{}]) has updated", updatedNotificationSettings.id)

        SendMessage(
            chatId.toString(),
            TelegramMessageConstants.TIME_BUTTON_RESULT_MESSAGE.format(notificationInterval.displayName)
        ).apply { sender.execute(this) }
    }

    override fun isQueryData(queryData: String): Boolean = IntervalNotificationType.typeOf(queryData) != null

    private fun deleteOldReplyMarkup(callbackQuery: CallbackQuery) {
        messageEditorService.deleteReplyMarkup(
            callbackQuery.message.chatId,
            callbackQuery.message.messageId,
        )
    }
}

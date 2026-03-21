package ru.illine.drinking.ponies.service.button.strategy

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.DelayTimeNotificationType
import ru.illine.drinking.ponies.service.MessageEditorService
import ru.illine.drinking.ponies.service.button.ReplyButtonStrategy
import ru.illine.drinking.ponies.util.TelegramMessageConstants

@Service
class DelayNotificationReplayButtonStrategy(
    private val sender: TelegramClient,
    private val notificationAccessService: NotificationAccessService,
    private val messageEditorService: MessageEditorService
) : ReplyButtonStrategy {

    private val log = LoggerFactory.getLogger("REPLAY-STRATEGY")

    override fun reply(callbackQuery: CallbackQuery) {
        deleteOldReplayMarkup(callbackQuery)

        val userId = callbackQuery.from.id
        val chatId = callbackQuery.message.chatId
        val queryData = callbackQuery.data

        val delayNotification = DelayTimeNotificationType.typeOf(queryData) ?: DelayTimeNotificationType.TWO_HOURS

        log.info(
            "A telegram user [{}] for telegram chat [{}] with delay setting [{}] will be stored to a database",
            userId,
            chatId,
            delayNotification
        )

        log.info("A notification settings for user [{}] with delay setting [{}] will be saved", userId, delayNotification)
        val updatedNotificationSettings =
            notificationAccessService.updateNotificationSettings(userId, chatId, delayNotification)
        log.info("The notification settings (id: [{}]) has updated", updatedNotificationSettings.id)

        SendMessage(
            chatId.toString(),
            TelegramMessageConstants.TIME_BUTTON_RESULT_MESSAGE.format(delayNotification.displayName)
        ).apply { sender.execute(this) }
    }

    override fun isQueryData(queryData: String): Boolean = DelayTimeNotificationType.typeOf(queryData) != null

    private fun deleteOldReplayMarkup(callbackQuery: CallbackQuery) {
        messageEditorService.deleteReplyMarkup(
            callbackQuery.message.chatId,
            callbackQuery.message.messageId,
        )
    }
}
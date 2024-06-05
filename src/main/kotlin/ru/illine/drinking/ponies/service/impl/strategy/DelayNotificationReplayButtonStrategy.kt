package ru.illine.drinking.ponies.service.impl.strategy

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.DelayNotificationType
import ru.illine.drinking.ponies.model.dto.NotificationDto
import ru.illine.drinking.ponies.service.ButtonEditorService
import ru.illine.drinking.ponies.service.ReplyButtonStrategy
import ru.illine.drinking.ponies.util.MessageHelper

@Service
class DelayNotificationReplayButtonStrategy(
    private val sender: TelegramClient,
    private val notificationAccessService: NotificationAccessService,
    private val buttonEditorService: ButtonEditorService
) : ReplyButtonStrategy {

    private val log = LoggerFactory.getLogger("REPLAY-STRATEGY")

    override fun reply(callbackQuery: CallbackQuery) {
        deleteOldReplayMarkup(callbackQuery)

        val userId = callbackQuery.from.id
        val chatId = callbackQuery.message.chatId
        val queryData = callbackQuery.data

        val delayNotification = DelayNotificationType.typeOf(queryData) ?: DelayNotificationType.TWO_HOURS

        log.info(
            "A user [{}] for chat [{}] with delay setting [{}] will be stored to a database",
            userId,
            chatId,
            delayNotification
        )

        log.info("A notification for user [{}] with delay setting [{}] will be saved", userId, delayNotification)
        val notification = NotificationDto.create(userId, chatId, delayNotification)
        val savedNotification = notificationAccessService.save(notification)
        log.info("The notification (id: [{}]) has saved", savedNotification.id)

        SendMessage(
            chatId.toString(),
            MessageHelper.TIME_BUTTON_RESULT_MESSAGE.format(delayNotification.displayName)
        ).apply { sender.execute(this) }
    }

    override fun isQueryData(queryData: String): Boolean = DelayNotificationType.typeOf(queryData) != null

    private fun deleteOldReplayMarkup(callbackQuery: CallbackQuery) {
        buttonEditorService.deleteReplyMarkup(
            callbackQuery.message.chatId,
            callbackQuery.message.messageId,
        )
    }
}
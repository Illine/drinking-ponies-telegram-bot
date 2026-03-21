package ru.illine.drinking.ponies.service.button.strategy

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.SnoozeNotificationType
import ru.illine.drinking.ponies.service.MessageEditorService
import ru.illine.drinking.ponies.service.button.ReplyButtonStrategy
import ru.illine.drinking.ponies.util.TelegramMessageConstants

@Service
class SnoozeNotificationReplayButtonStrategy(
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

        val snoozeType = SnoozeNotificationType.typeOf(queryData) ?: SnoozeNotificationType.TEN_MINUTES

        log.info(
            "A telegram user [{}] for telegram chat [{}] with snooze setting [{}] will delay notification",
            userId,
            chatId,
            snoozeType
        )

        val notificationSetting = notificationAccessService.findNotificationSettingByTelegramUserId(userId)
        notificationAccessService.updateTimeOfLastNotification(
            userId,
            notificationSetting.timeOfLastNotification.plusMinutes(snoozeType.minutes)
        )

        log.info("The notification for user [{}] has been snoozed for [{}] minutes", userId, snoozeType.minutes)

        SendMessage(
            chatId.toString(),
            TelegramMessageConstants.NOTIFICATION_SNOOZE_RESULT_MESSAGE.format(snoozeType.displayName)
        ).apply { sender.execute(this) }
    }

    override fun isQueryData(queryData: String): Boolean = SnoozeNotificationType.typeOf(queryData) != null

    private fun deleteOldReplayMarkup(callbackQuery: CallbackQuery) {
        messageEditorService.deleteReplyMarkup(
            callbackQuery.message.chatId,
            callbackQuery.message.messageId,
        )
    }
}

package ru.illine.drinking.ponies.service.button.strategy.snooze

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.SnoozeNotificationType
import ru.illine.drinking.ponies.service.button.ReplyButtonStrategy
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.util.TimeHelper
import ru.illine.drinking.ponies.util.telegram.TelegramMessageConstants
import java.time.Clock

@Service
class SnoozeApplyReplyButtonStrategy(
    private val sender: TelegramClient,
    private val notificationAccessService: NotificationAccessService,
    private val messageEditorService: MessageEditorService,
    private val clock: Clock
) : ReplyButtonStrategy {

    private val logger = LoggerFactory.getLogger("REPLY-STRATEGY")

    override fun reply(callbackQuery: CallbackQuery) {
        messageEditorService.deleteReplyMarkup(
            callbackQuery.message.chatId,
            callbackQuery.message.messageId
        )

        val userId = callbackQuery.from.id
        val chatId = callbackQuery.message.chatId
        val queryData = callbackQuery.data

        val snoozeType = SnoozeNotificationType.typeOf(queryData) ?: SnoozeNotificationType.TEN_MINS

        logger.info(
            "A telegram user [{}] for telegram chat [{}] will snooze notification for [{}] minutes",
            userId,
            chatId,
            snoozeType.minutes
        )

        val notificationSetting = notificationAccessService.findNotificationSettingByTelegramUserId(userId)
        val nextNotificationTime =
            TimeHelper.nextNotificationTimeByNow(
                clock,
                notificationSetting.notificationInterval.minutes,
                snoozeType.minutes
            )
        notificationAccessService.updateTimeOfLastNotification(userId, nextNotificationTime)

        SendMessage(
            chatId.toString(),
            TelegramMessageConstants.NOTIFICATION_SUSPEND_MESSAGE.format(snoozeType.displayName)
        ).apply { sender.execute(this) }
    }

    override fun isQueryData(queryData: String): Boolean = SnoozeNotificationType.typeOf(queryData) != null

}

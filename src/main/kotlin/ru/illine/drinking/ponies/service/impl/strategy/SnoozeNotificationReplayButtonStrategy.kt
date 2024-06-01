package ru.illine.drinking.ponies.service.impl.strategy

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.abilitybots.api.sender.MessageSender
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.SnoozeNotificationType
import ru.illine.drinking.ponies.model.dto.NotificationDto
import ru.illine.drinking.ponies.service.ButtonEditorService
import ru.illine.drinking.ponies.service.ReplyButtonStrategy
import ru.illine.drinking.ponies.util.MessageHelper
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class SnoozeNotificationReplayButtonStrategy(
    private val sender: MessageSender,
    private val notificationAccessService: NotificationAccessService,
    private val buttonEditorService: ButtonEditorService
) : ReplyButtonStrategy {

    private val log = LoggerFactory.getLogger("REPLAY-STRATEGY")

    override fun reply(callbackQuery: CallbackQuery) {
        deleteOldReplayMarkup(callbackQuery)

        val userId = callbackQuery.from.id
        val chatId = callbackQuery.message.chatId
        val queryData = callbackQuery.data

        val snoozeNotification = SnoozeNotificationType.typeOf(queryData)!!

        log.info(
            "A notification will be delayed to [{}] delayNotification for a user [{}]",
            snoozeNotification,
            userId
        )

        val savedNotification = notificationAccessService.findByUserId(userId)
        val delayedNotificationTime = getDelayedNotificationTime(savedNotification, snoozeNotification)
        log.info("The new notification will be at [{}]", delayedNotificationTime)

        notificationAccessService.updateTimeOfLastNotification(userId, delayedNotificationTime)

        SendMessage().apply {
            text = MessageHelper.SNOOZE_BUTTON_RESULT_MESSAGE.format(snoozeNotification.displayName)
            setChatId(chatId)
        }.apply { sender.execute(this) }
    }

    private fun getDelayedNotificationTime(
        savedNotification: NotificationDto,
        snoozeNotification: SnoozeNotificationType
    ): LocalDateTime {
        val userZoneId = ZoneId.of(savedNotification.userTimeZone)
        val userDateTime = ZonedDateTime.now(userZoneId)
        return userDateTime.plusMinutes(snoozeNotification.minutes).toLocalDateTime()
    }

    override fun isQueryData(queryData: String): Boolean = SnoozeNotificationType.typeOf(queryData) != null

    private fun deleteOldReplayMarkup(callbackQuery: CallbackQuery) {
        buttonEditorService.deleteReplyMarkup(
            callbackQuery.message.chatId,
            callbackQuery.message.messageId,
        )
    }
}
package ru.illine.drinking.ponies.service.impl.strategy

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.PauseNotificationType
import ru.illine.drinking.ponies.model.dto.NotificationDto
import ru.illine.drinking.ponies.service.ButtonEditorService
import ru.illine.drinking.ponies.service.ReplyButtonStrategy
import ru.illine.drinking.ponies.util.MessageHelper
import ru.illine.drinking.ponies.util.TimeMessageHelper
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class PauseNotificationReplayButtonStrategy(
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

        val pauseNotification = PauseNotificationType.typeOf(queryData)!!

        if (pauseNotification != PauseNotificationType.RESET) {
            pause(pauseNotification, userId, chatId)
        } else {
            cancelPause(userId, chatId)
        }
    }

    private fun pause(
        pauseNotification: PauseNotificationType,
        userId: Long,
        chatId: Long
    ) {
        val savedNotification = notificationAccessService.findByUserId(userId)
        val delayedNotificationTime = getDelayedNotificationTime(savedNotification, pauseNotification)
        log.info(
            "A notification will be delayed to [{}] delayNotification for a user [{}]",
            pauseNotification,
            userId
        )
        log.info("The new notification will be at [{}]", delayedNotificationTime)

        notificationAccessService.updateTimeOfLastNotification(userId, delayedNotificationTime)

        SendMessage(
            chatId.toString(),
            MessageHelper.PAUSE_BUTTON_RESULT_MESSAGE.format(pauseNotification.displayName)
        ).apply { sender.execute(this) }
    }

    private fun cancelPause(
        userId: Long,
        chatId: Long
    ) {
        log.info("A notification will be reset to user's delayNotification for a user [{}]", userId)
        val savedNotification = notificationAccessService.findByUserId(userId)
        val delayNotification = savedNotification.delayNotification
        log.info("User's delayNotification: [{}]", delayNotification)
        val delayedNotificationTime =
            getDelayedNotificationTime(savedNotification, delayNotification.minutes)
        log.info("The new notification will be at [{}]", delayedNotificationTime)

        notificationAccessService.updateTimeOfLastNotification(userId, delayedNotificationTime)

        val timeNextNotification = TimeMessageHelper.timeToString(delayedNotificationTime)
        val message =
            MessageHelper.PAUSE_RESET_BUTTON_RESULT_MESSAGE.format(
                delayNotification.displayName,
                timeNextNotification
            )

        SendMessage(
            chatId.toString(),
            message
        ).apply { sender.execute(this) }
    }

    private fun getDelayedNotificationTime(
        savedNotification: NotificationDto,
        pauseNotification: PauseNotificationType
    ): LocalDateTime = getDelayedNotificationTime(savedNotification, pauseNotification.minutes)

    private fun getDelayedNotificationTime(
        savedNotification: NotificationDto,
        minutes: Long
    ): LocalDateTime {
        val userZoneId = ZoneId.of(savedNotification.userTimeZone)
        val userDateTime = ZonedDateTime.now(userZoneId)
        return userDateTime.plusMinutes(minutes).toLocalDateTime()
    }

    override fun isQueryData(queryData: String): Boolean = PauseNotificationType.typeOf(queryData) != null

    private fun deleteOldReplayMarkup(callbackQuery: CallbackQuery) {
        buttonEditorService.deleteReplyMarkup(
            callbackQuery.message.chatId,
            callbackQuery.message.messageId,
        )
    }
}
package ru.illine.drinking.ponies.service.button.strategy

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.TimeNotificationType
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.service.MessageEditorService
import ru.illine.drinking.ponies.service.button.ReplyButtonStrategy
import ru.illine.drinking.ponies.util.TelegramMessageConstants
import ru.illine.drinking.ponies.util.TimeMessageHelper
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class PauseNotificationReplayButtonStrategy(
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

        val pauseNotification = TimeNotificationType.typeOf(queryData)!!

        if (pauseNotification != TimeNotificationType.RESET) {
            pause(pauseNotification, userId, chatId)
        } else {
            cancelPause(userId, chatId)
        }
    }

    private fun pause(
        pauseNotification: TimeNotificationType,
        userId: Long,
        chatId: Long
    ) {
        val savedNotificationSetting =
            notificationAccessService.findNotificationSettingByTelegramUserId(userId)
        val delayedNotificationTime = getDelayedNotificationTime(savedNotificationSetting, pauseNotification)
        log.info(
            "A notification will be delayed to [{}] delayNotification for a user [{}]",
            pauseNotification,
            userId
        )
        log.info("The new notification will be at [{}]", delayedNotificationTime)

        notificationAccessService.updateTimeOfLastNotification(userId, delayedNotificationTime)

        SendMessage(
            chatId.toString(),
            TelegramMessageConstants.PAUSE_BUTTON_RESULT_MESSAGE.format(pauseNotification.displayName)
        ).apply { sender.execute(this) }
    }

    private fun cancelPause(
        userId: Long,
        chatId: Long
    ) {
        log.info("A notification will be reset to user's delayNotification for a user [{}]", userId)
        val savedNotificationSetting =
            notificationAccessService.findNotificationSettingByTelegramUserId(userId)
        val delayNotification = savedNotificationSetting.delayNotification
        log.info("User's delayNotification: [{}]", delayNotification)
        val delayedNotificationTime =
            getDelayedNotificationTime(savedNotificationSetting, delayNotification.minutes)
        log.info("The new notification will be at [{}]", delayedNotificationTime)

        notificationAccessService.updateTimeOfLastNotification(userId, delayedNotificationTime)

        val timeNextNotification = TimeMessageHelper.timeToString(delayedNotificationTime)
        val message =
            TelegramMessageConstants.PAUSE_RESET_BUTTON_RESULT_MESSAGE.format(
                delayNotification.displayName,
                timeNextNotification
            )

        SendMessage(
            chatId.toString(),
            message
        ).apply { sender.execute(this) }
    }

    private fun getDelayedNotificationTime(
        savedNotificationSetting: NotificationSettingDto,
        pauseNotification: TimeNotificationType
    ): LocalDateTime = getDelayedNotificationTime(savedNotificationSetting, pauseNotification.minutes)

    private fun getDelayedNotificationTime(
        savedNotificationSetting: NotificationSettingDto,
        minutes: Long
    ): LocalDateTime {
        val userZoneId = ZoneId.of(savedNotificationSetting.telegramUser.userTimeZone)
        val userDateTime = ZonedDateTime.now(userZoneId)
        return userDateTime.plusMinutes(minutes).toLocalDateTime()
    }

    override fun isQueryData(queryData: String): Boolean = TimeNotificationType.typeOf(queryData) != null

    private fun deleteOldReplayMarkup(callbackQuery: CallbackQuery) {
        messageEditorService.deleteReplyMarkup(
            callbackQuery.message.chatId,
            callbackQuery.message.messageId,
        )
    }
}
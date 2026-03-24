package ru.illine.drinking.ponies.service.button.strategy.pause

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.PauseNotificationType
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.service.button.ReplyButtonStrategy
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.util.TimeMessageHelper
import ru.illine.drinking.ponies.util.telegram.TelegramMessageConstants
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class PauseNotificationReplyButtonStrategy(
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
        val savedNotificationSetting =
            notificationAccessService.findNotificationSettingByTelegramUserId(userId)
        val nextNotificationTime = calculateNextNotificationTime(savedNotificationSetting, pauseNotification)
        log.info(
            "A notification will be postponed to [{}] for a user [{}]",
            pauseNotification,
            userId
        )
        log.info("The new notification will be at [{}]", nextNotificationTime)

        notificationAccessService.updateTimeOfLastNotification(userId, nextNotificationTime)

        SendMessage(
            chatId.toString(),
            TelegramMessageConstants.PAUSE_BUTTON_RESULT_MESSAGE.format(pauseNotification.displayName)
        ).apply { sender.execute(this) }
    }

    private fun cancelPause(
        userId: Long,
        chatId: Long
    ) {
        log.info("A notification will be reset to user's notification interval for a user [{}]", userId)
        val savedNotificationSetting =
            notificationAccessService.findNotificationSettingByTelegramUserId(userId)
        val notificationInterval = savedNotificationSetting.notificationInterval
        log.info("User's notification interval: [{}]", notificationInterval)
        val nextNotificationTime =
            calculateNextNotificationTime(savedNotificationSetting, notificationInterval.minutes)
        log.info("The new notification will be at [{}]", nextNotificationTime)

        notificationAccessService.updateTimeOfLastNotification(userId, nextNotificationTime)

        val timeNextNotification = TimeMessageHelper.timeToString(nextNotificationTime)
        val message =
            TelegramMessageConstants.PAUSE_RESET_BUTTON_RESULT_MESSAGE.format(
                notificationInterval.displayName,
                timeNextNotification
            )

        SendMessage(
            chatId.toString(),
            message
        ).apply { sender.execute(this) }
    }

    private fun calculateNextNotificationTime(
        savedNotificationSetting: NotificationSettingDto,
        pauseNotification: PauseNotificationType
    ): LocalDateTime = calculateNextNotificationTime(savedNotificationSetting, pauseNotification.minutes)

    private fun calculateNextNotificationTime(
        savedNotificationSetting: NotificationSettingDto,
        minutes: Long
    ): LocalDateTime {
        val userZoneId = ZoneId.of(savedNotificationSetting.telegramUser.userTimeZone)
        val userDateTime = ZonedDateTime.now(userZoneId)
        return userDateTime.plusMinutes(minutes).toLocalDateTime()
    }

    override fun isQueryData(queryData: String): Boolean = PauseNotificationType.typeOf(queryData) != null

    private fun deleteOldReplyMarkup(callbackQuery: CallbackQuery) {
        messageEditorService.deleteReplyMarkup(
            callbackQuery.message.chatId,
            callbackQuery.message.messageId,
        )
    }
}
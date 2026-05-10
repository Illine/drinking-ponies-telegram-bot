package ru.illine.drinking.ponies.service.notification.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.config.property.TelegramBotProperties
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.service.notification.NotificationSenderService
import ru.illine.drinking.ponies.service.statistic.WaterStatisticService
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.util.TimeHelper
import ru.illine.drinking.ponies.util.telegram.TelegramBotKeyboardHelper
import ru.illine.drinking.ponies.util.telegram.TelegramMessageConstants
import java.time.Clock
import java.time.LocalDateTime

@Service
class NotificationSenderServiceImpl(
    private val sender: TelegramClient,
    private val messageEditorService: MessageEditorService,
    private val notificationAccessService: NotificationAccessService,
    private val telegramBotProperties: TelegramBotProperties,
    private val waterStatisticService: WaterStatisticService,
    private val clock: Clock,
) : NotificationSenderService {

    private val logger = LoggerFactory.getLogger("SERVICE")

    override fun sendNotifications(notifications: Collection<NotificationSettingDto>) {
        if (notifications.isEmpty()) {
            logger.debug("There are no notifications to send")
            return
        }

        deletePreviousNotificationMessages(notifications)

        val sent = notifications.filter {
            sendOrDisableOnBlock(it) {
                ++it.notificationAttempts
                it.timeOfLastNotification = TimeHelper.nextNotificationTimeByNow(
                    clock, it.notificationInterval.minutes, telegramBotProperties.notification.retryIntervalMinutes
                )
                it.telegramChat.previousNotificationMessageId =
                    SendMessage(
                        it.telegramChat.externalChatId.toString(),
                        TelegramMessageConstants.NOTIFICATION_QUESTION_MESSAGE
                    ).apply {
                        replyMarkup = TelegramBotKeyboardHelper.notifyButtons()
                    }.let { sender.execute(it) }.messageId
            }
        }

        notificationAccessService.updateNotificationSettings(sent)
    }

    override fun suspendNotifications(notifications: Collection<NotificationSettingDto>) {
        if (notifications.isEmpty()) {
            logger.debug("There are no notifications to send")
            return
        }

        deletePreviousNotificationMessages(notifications)

        val sent = notifications.filter {
            sendOrDisableOnBlock(it) {
                SendMessage(
                    it.telegramChat.externalChatId.toString(),
                    TelegramMessageConstants.NOTIFICATION_SUSPEND_MESSAGE.format(it.notificationInterval.displayName)
                ).apply {
                    disableNotification = true
                }.apply { sender.execute(this) }

                it.notificationAttempts = 0
                it.timeOfLastNotification = LocalDateTime.now(clock)
                it.telegramChat.previousNotificationMessageId = null
            }
        }

        notificationAccessService.updateNotificationSettings(sent)
        waterStatisticService.recordEvents(
            sent.map { it.telegramUser },
            AnswerNotificationType.CANCEL
        )
    }

    private fun sendOrDisableOnBlock(notification: NotificationSettingDto, send: () -> Unit): Boolean {
        return try {
            send()
            true
        } catch (e: TelegramApiRequestException) {
            if (e.errorCode == 403) {
                logger.warn(
                    "User (externalUserId: [{}]) blocked the bot, disabling notifications",
                    notification.telegramUser.externalUserId
                )
                notificationAccessService.disableNotifications(notification.telegramUser.externalUserId)
                false
            } else {
                throw e
            }
        }
    }

    private fun deletePreviousNotificationMessages(settings: Collection<NotificationSettingDto>) {
        logger.info("Deleting all old notifications messages...")

        val messageInfo = settings
            .filter { it.telegramChat.previousNotificationMessageId != null }
            .map { Pair(it.telegramChat.externalChatId, it.telegramChat.previousNotificationMessageId!!) }
            .toList()

        logger.info("Found [${messageInfo.size}] the old notification messages")
        logger.debug("The old messages: \n{}", messageInfo)

        messageEditorService.deleteMessages(messageInfo)
    }
}

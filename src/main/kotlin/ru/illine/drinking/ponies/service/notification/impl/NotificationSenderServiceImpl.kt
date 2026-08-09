package ru.illine.drinking.ponies.service.notification.impl

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.config.property.TelegramBotProperties
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.dto.internal.NoContext
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.model.dto.internal.NotificationSuspendContext
import ru.illine.drinking.ponies.service.message.MessageProvider
import ru.illine.drinking.ponies.service.notification.NotificationSenderService
import ru.illine.drinking.ponies.service.statistic.WaterStatisticService
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.util.TimeHelper
import ru.illine.drinking.ponies.util.message.MessageSpec
import ru.illine.drinking.ponies.util.telegram.TelegramBotKeyboardHelper
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
    private val messageProvider: MessageProvider,
) : NotificationSenderService {
    private val logger = LoggerFactory.getLogger("SERVICE")

    override fun sendNotifications(notifications: Collection<NotificationSettingDto>) {
        if (notifications.isEmpty()) {
            logger.debug("There are no notifications to send")
            return
        }

        deletePreviousNotificationMessages(notifications)

        val sent =
            notifications.filter {
                trySend(it) {
                    ++it.notificationAttempts
                    it.timeOfLastNotification =
                        TimeHelper.nextNotificationTimeByNow(
                            clock,
                            it.notificationInterval.minutes,
                            telegramBotProperties.notification.retryIntervalMinutes,
                        )
                    it.telegramChat.previousNotificationMessageId =
                        SendMessage(
                            it.telegramChat.externalChatId.toString(),
                            messageProvider.getMessage(MessageSpec.NotificationQuestion, NoContext).text,
                        ).apply {
                            replyMarkup = TelegramBotKeyboardHelper.notifyButtons()
                        }.let { sender.execute(it) }
                            .messageId
                }
            }

        notificationAccessService.recordMailingResults(sent)
    }

    override fun suspendNotifications(notifications: Collection<NotificationSettingDto>) {
        if (notifications.isEmpty()) {
            logger.debug("There are no notifications to send")
            return
        }

        deletePreviousNotificationMessages(notifications)

        val sent =
            notifications.filter {
                trySend(it) {
                    SendMessage(
                        it.telegramChat.externalChatId.toString(),
                        messageProvider
                            .getMessage(
                                MessageSpec.NotificationSuspend,
                                NotificationSuspendContext(it.notificationInterval.displayName),
                            ).text,
                    ).apply {
                        disableNotification = true
                    }.apply { sender.execute(this) }

                    it.notificationAttempts = 0
                    it.timeOfLastNotification = LocalDateTime.now(clock)
                    it.telegramChat.previousNotificationMessageId = null
                }
            }

        notificationAccessService.recordMailingResults(sent)
        waterStatisticService.recordEvents(
            sent.map { it.telegramUser },
            AnswerNotificationType.CANCEL,
        )
    }

    private fun trySend(
        notification: NotificationSettingDto,
        send: () -> Unit,
    ): Boolean =
        try {
            send()
            true
        } catch (e: TelegramApiRequestException) {
            if (e.errorCode == HttpStatus.FORBIDDEN.value()) {
                logger.info(
                    "User (externalUserId: [{}]) blocked the bot, disabling notifications",
                    notification.telegramUser.externalUserId,
                )
                notificationAccessService.updateNotificationsDisabled(notification.telegramUser.externalUserId)
                false
            } else if (e.errorCode == HttpStatus.BAD_REQUEST.value()) {
                logger.info("Bad request: [{}]", e.message)
                false
            } else {
                throw e
            }
        }

    private fun deletePreviousNotificationMessages(settings: Collection<NotificationSettingDto>) {
        logger.info("Deleting all old notifications messages...")

        val messageInfo =
            settings
                .filter { it.telegramChat.previousNotificationMessageId != null }
                .map { Pair(it.telegramChat.externalChatId, it.telegramChat.previousNotificationMessageId!!) }
                .toList()

        logger.info("Found [${messageInfo.size}] the old notification messages")
        logger.debug("The old messages: \n{}", messageInfo)

        messageEditorService.deleteMessages(messageInfo)
    }
}

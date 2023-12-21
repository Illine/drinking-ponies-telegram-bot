package ru.illine.drinking.ponies.service.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.abilitybots.api.objects.MessageContext
import org.telegram.abilitybots.api.sender.MessageSender
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.dto.NotificationDto
import ru.illine.drinking.ponies.service.ButtonEditorService
import ru.illine.drinking.ponies.service.NotificationService
import ru.illine.drinking.ponies.util.FunctionHelper.check
import ru.illine.drinking.ponies.util.MessageHelper
import ru.illine.drinking.ponies.util.TelegramBotKeyboardHelper
import java.time.OffsetDateTime

@Service
class NotificationServiceImpl(
    private val sender: MessageSender,
    private val buttonEditorService: ButtonEditorService,
    private val notificationAccessService: NotificationAccessService
) : NotificationService {

    private val log = LoggerFactory.getLogger("SERVICE")

    override fun start(messageContext: MessageContext) {
        SendMessage().apply {
            text = MessageHelper.START_GREETING_MESSAGE.format(messageContext.user().userName)
            setChatId(messageContext.chatId())
        }.apply { sender.execute(this) }

        val userId = messageContext.user().id
        val chatId = messageContext.chatId()

        val notification = notificationAccessService.existsByUserId(userId).check(
            ifTrue = {
                notificationAccessService.findByUserId(userId)
            },
            ifFalse = {
                val notification = NotificationDto.create(userId, chatId)
                notificationAccessService.save(notification)
            }
        )

        SendMessage().apply {
            text = MessageHelper.START_DEFAULT_SETTINGS_MESSAGE.format(notification.delayNotification.displayName)
            setChatId(messageContext.chatId())
        }.apply { sender.execute(this) }
    }

    override fun stop(messageContext: MessageContext) {
        notificationAccessService.disableByUserId(messageContext.user().id)

        SendMessage().apply {
            text = MessageHelper.STOP_GREETING_MESSAGE.format(messageContext.user().userName)
            setChatId(messageContext.chatId())
        }.apply { sender.execute(this) }
    }

    override fun resume(messageContext: MessageContext) {
        notificationAccessService.enableByUserId(messageContext.user().id)

        SendMessage().apply {
            text = MessageHelper.RESUME_GREETING_MESSAGE.format(messageContext.user().userName)
            setChatId(messageContext.chatId())
        }.apply { sender.execute(this) }
    }

    override fun settings(messageContext: MessageContext) {
        SendMessage().apply {
            text = MessageHelper.SETTINGS_GREETING_MESSAGE
            setChatId(messageContext.chatId())
            replyMarkup = TelegramBotKeyboardHelper.settingsButtons()
        }.apply { sender.execute(this) }
    }

    override fun sendNotifications(notifications: Collection<NotificationDto>) {
        deletePreviousNotificationMessages(notifications)

        notifications
            .forEach {
                it.notificationAttempts += 1
                it.previousNotificationMessageId =
                    SendMessage().apply {
                        text = MessageHelper.NOTIFICATION_QUESTION_MESSAGE
                        setChatId(it.chatId)
                        replyMarkup = TelegramBotKeyboardHelper.notifyButtons()
                    }.let { sender.execute(it) }.messageId
            }

        notificationAccessService.updateNotifications(notifications)
    }

    override fun suspendNotifications(notifications: Collection<NotificationDto>) {
        deletePreviousNotificationMessages(notifications)

        val now = OffsetDateTime.now()
        notifications
            .forEach {
                SendMessage().apply {
                    text = MessageHelper.NOTIFICATION_SUSPEND_MESSAGE.format(it.delayNotification.displayName)
                    setChatId(it.chatId)
                    disableNotification = true
                }.apply { sender.execute(this) }

                it.notificationAttempts = 0
                it.timeOfLastNotification = now
                it.previousNotificationMessageId = null

            }

        notificationAccessService.updateNotifications(notifications)
    }

    private fun deletePreviousNotificationMessages(notifications: Collection<NotificationDto>) {
        log.info("Deleting all old notifications messages...")

        val messageInfo = notifications.stream()
            .filter { it.previousNotificationMessageId != null }
            .map { Pair(it.chatId, it.previousNotificationMessageId!!) }
            .toList()

        log.info("Found [${messageInfo.size}] the old notification messages")
        log.debug("The old messages: \n{}", messageInfo)

        buttonEditorService.deleteMessages(messageInfo)
    }
}
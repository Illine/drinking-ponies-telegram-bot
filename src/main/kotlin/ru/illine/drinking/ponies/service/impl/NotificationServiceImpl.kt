package ru.illine.drinking.ponies.service.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.abilitybots.api.objects.MessageContext
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.SettingsType
import ru.illine.drinking.ponies.model.dto.NotificationDto
import ru.illine.drinking.ponies.service.MessageEditorService
import ru.illine.drinking.ponies.service.NotificationService
import ru.illine.drinking.ponies.service.button.ButtonDataService
import ru.illine.drinking.ponies.util.FunctionHelper.check
import ru.illine.drinking.ponies.util.TelegramBotKeyboardHelper
import ru.illine.drinking.ponies.util.TelegramConstants
import java.time.LocalDateTime

@Service
class NotificationServiceImpl(
    private val sender: TelegramClient,
    private val messageEditorService: MessageEditorService,
    private val notificationAccessService: NotificationAccessService,
    private val settingsButtonDataService: ButtonDataService<SettingsType>
) : NotificationService {

    private val log = LoggerFactory.getLogger("SERVICE")

    override fun start(messageContext: MessageContext) {
        SendMessage(
            messageContext.chatId().toString(),
            TelegramConstants.START_GREETING_MESSAGE.format(messageContext.user().userName)
        ).apply { sender.execute(this) }

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

        SendMessage(
            messageContext.chatId().toString(),
            TelegramConstants.START_DEFAULT_SETTINGS_MESSAGE.format(notification.delayNotification.displayName)
        ).apply { sender.execute(this) }
    }

    override fun stop(messageContext: MessageContext) {
        notificationAccessService.disableByUserId(messageContext.user().id)

        SendMessage(
            messageContext.chatId().toString(),
            TelegramConstants.STOP_GREETING_MESSAGE.format(messageContext.user().userName)
        ).apply { sender.execute(this) }
    }

    override fun resume(messageContext: MessageContext) {
        notificationAccessService.enableByUserId(messageContext.user().id)

        SendMessage(
            messageContext.chatId().toString(),
            TelegramConstants.RESUME_GREETING_MESSAGE.format(messageContext.user().userName)
        ).apply { sender.execute(this) }
    }

    override fun pause(messageContext: MessageContext) {
        val userId = messageContext.user().id
        val chantId = messageContext.chatId()
        val delayNotification = notificationAccessService.findByUserId(userId).delayNotification

        val sendMessageFunction: () -> Unit = {
            SendMessage(
                chantId.toString(),
                TelegramConstants.PAUSE_GREETING_MESSAGE
            ).apply {
                replyMarkup = TelegramBotKeyboardHelper.pauseTimeButtons(delayNotification)
            }.apply { sender.execute(this) }
        }

        sendIfNotificationActive(
            userId, chantId, sendMessageFunction
        )
    }

    override fun settings(messageContext: MessageContext) {
        val chantId = messageContext.chatId()
        val sendMessageFunction: () -> Unit = {
            SendMessage(
                chantId.toString(),
                TelegramConstants.SETTINGS_GREETING_MESSAGE
            ).apply {
                replyMarkup = TelegramBotKeyboardHelper.settingsButtons(settingsButtonDataService)
            }.let {
                sender.execute(it)
            }.let {
                messageEditorService.editReplyMarkup(
                    newText = it.text,
                    chatId = it.chatId,
                    messageId = it.messageId,
                    replayKeyboard = TelegramBotKeyboardHelper.settingsButtons(
                        settingsButtonDataService,
                        it.messageId
                    )
                )
            }
        }

        sendIfNotificationActive(
            messageContext.user().id, messageContext.chatId(), sendMessageFunction
        )
    }

    override fun sendNotifications(notifications: Collection<NotificationDto>) {
        deletePreviousNotificationMessages(notifications)

        notifications
            .forEach {
                ++it.notificationAttempts
                it.previousNotificationMessageId =
                    SendMessage(
                        it.chatId.toString(),
                        TelegramConstants.NOTIFICATION_QUESTION_MESSAGE
                    ).apply {
                        replyMarkup = TelegramBotKeyboardHelper.notifyButtons()
                    }.let { sender.execute(it) }.messageId
            }

        notificationAccessService.updateNotifications(notifications)
    }

    override fun suspendNotifications(notifications: Collection<NotificationDto>) {
        deletePreviousNotificationMessages(notifications)

        val now = LocalDateTime.now()
        notifications
            .forEach {
                SendMessage(
                    it.chatId.toString(),
                    TelegramConstants.NOTIFICATION_SUSPEND_MESSAGE.format(it.delayNotification.displayName)
                ).apply {
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

        messageEditorService.deleteMessages(messageInfo)
    }

    fun sendIfNotificationActive(userId: Long, chatId: Long, sendMessage: () -> Unit) {
        notificationAccessService.isActiveNotification(userId)
            .check(
                ifTrue = {
                    log.debug("A notification is disabled for user (userId: [$userId]), send a disabled message")
                    SendMessage(
                        chatId.toString(),
                        TelegramConstants.NOTIFICATION_NOT_ACTIVE_MESSAGE
                    ).apply { sender.execute(this) }
                },
                ifFalse = {
                    log.debug("A notification is enabled for user (userId: [$userId]), send a standard message")
                    sendMessage()
                }
            )
    }
}
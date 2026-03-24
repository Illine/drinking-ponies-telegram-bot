package ru.illine.drinking.ponies.service.notification.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.abilitybots.api.objects.MessageContext
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.SettingsType
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramChatDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserDto
import ru.illine.drinking.ponies.service.button.ButtonDataService
import ru.illine.drinking.ponies.service.notification.NotificationService
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.util.FunctionHelper.check
import ru.illine.drinking.ponies.util.telegram.TelegramBotKeyboardHelper
import ru.illine.drinking.ponies.util.telegram.TelegramMessageConstants
import java.time.Clock
import java.time.LocalDateTime

@Service
class NotificationServiceImpl(
    private val sender: TelegramClient,
    private val messageEditorService: MessageEditorService,
    private val notificationAccessService: NotificationAccessService,
    private val settingsButtonDataService: ButtonDataService<SettingsType>,
    private val clock: Clock,
) : NotificationService {

    private val log = LoggerFactory.getLogger("SERVICE")

    override fun start(messageContext: MessageContext) {
        SendMessage(
            messageContext.chatId().toString(),
            TelegramMessageConstants.START_GREETING_MESSAGE.format(messageContext.user().userName)
        ).apply { sender.execute(this) }

        val userId = messageContext.user().id
        val chatId = messageContext.chatId()

        val setting = notificationAccessService.existsByTelegramUserId(userId).check(
            ifTrue = {
                notificationAccessService.findNotificationSettingByTelegramUserId(userId)
            },
            ifFalse = {
                createNewUser(userId, chatId)
            }
        )

        SendMessage(
            messageContext.chatId().toString(),
            TelegramMessageConstants.START_DEFAULT_SETTINGS_MESSAGE.format(setting.notificationInterval.displayName)
        ).apply { sender.execute(this) }
    }

    override fun stop(messageContext: MessageContext) {
        notificationAccessService.disableNotifications(messageContext.user().id)

        SendMessage(
            messageContext.chatId().toString(),
            TelegramMessageConstants.STOP_GREETING_MESSAGE.format(messageContext.user().userName)
        ).apply { sender.execute(this) }
    }

    override fun resume(messageContext: MessageContext) {
        notificationAccessService.enableNotifications(messageContext.user().id)

        SendMessage(
            messageContext.chatId().toString(),
            TelegramMessageConstants.RESUME_GREETING_MESSAGE.format(messageContext.user().userName)
        ).apply { sender.execute(this) }
    }

    override fun pause(messageContext: MessageContext) {
        val userId = messageContext.user().id
        val chatId = messageContext.chatId()
        val notificationInterval = notificationAccessService.findNotificationSettingByTelegramUserId(userId).notificationInterval

        val sendMessageFunction: () -> Unit = {
            SendMessage(
                chatId.toString(),
                TelegramMessageConstants.PAUSE_GREETING_MESSAGE
            ).apply {
                replyMarkup = TelegramBotKeyboardHelper.pauseTimeButtons(notificationInterval)
            }.apply { sender.execute(this) }
        }

        sendIfNotificationEnabled(
            userId, chatId, sendMessageFunction
        )
    }

    override fun settings(messageContext: MessageContext) {
        val chatId = messageContext.chatId()
        val sendMessageFunction: () -> Unit = {
            SendMessage(
                chatId.toString(),
                TelegramMessageConstants.SETTINGS_GREETING_MESSAGE
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

        sendIfNotificationEnabled(
            messageContext.user().id, messageContext.chatId(), sendMessageFunction
        )
    }

    override fun sendNotifications(notifications: Collection<NotificationSettingDto>) {
        if (notifications.isEmpty()) {
            log.debug("There are no notifications to send")
            return
        }

        deletePreviousNotificationMessages(notifications)

        val sent = notifications.filter {
            sendOrDisableOnBlock(it) {
                ++it.notificationAttempts
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
            log.debug("There are no notifications to send")
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
    }

    private fun sendOrDisableOnBlock(notification: NotificationSettingDto, send: () -> Unit): Boolean {
        return try {
            send()
            true
        } catch (e: TelegramApiRequestException) {
            if (e.errorCode == 403) {
                log.warn("User (externalUserId: [{}]) blocked the bot, disabling notifications", notification.telegramUser.externalUserId)
                notificationAccessService.disableNotifications(notification.telegramUser.externalUserId)
                false
            } else {
                throw e
            }
        }
    }

    private fun deletePreviousNotificationMessages(settings: Collection<NotificationSettingDto>) {
        log.info("Deleting all old notifications messages...")

        val messageInfo = settings
            .filter { it.telegramChat.previousNotificationMessageId != null }
            .map { Pair(it.telegramChat.externalChatId, it.telegramChat.previousNotificationMessageId!!) }
            .toList()

        log.info("Found [${messageInfo.size}] the old notification messages")
        log.debug("The old messages: \n{}", messageInfo)

        messageEditorService.deleteMessages(messageInfo)
    }

    private fun sendIfNotificationEnabled(userId: Long, chatId: Long, sendMessage: () -> Unit) {
        notificationAccessService.isEnabledNotifications(userId)
            .check(
                ifTrue = {
                    log.debug("A notification is enabled for user (userId: [$userId]), send a standard message")
                    sendMessage()
                },
                ifFalse = {
                    log.debug("A notification is disabled for user (userId: [$userId]), send a disabled message")
                    SendMessage(
                        chatId.toString(),
                        TelegramMessageConstants.NOTIFICATION_NOT_ACTIVE_MESSAGE
                    ).apply { sender.execute(this) }
                }
            )
    }

    private fun createNewUser(
        userId: Long,
        chatId: Long
    ): NotificationSettingDto {
        val user = TelegramUserDto.create(userId)
        val chat = TelegramChatDto.create(chatId, user)
        val setting = NotificationSettingDto.create(user, chat)

        notificationAccessService.save(user, chat, setting)
        return notificationAccessService.findNotificationSettingByTelegramUserId(userId)
    }
}

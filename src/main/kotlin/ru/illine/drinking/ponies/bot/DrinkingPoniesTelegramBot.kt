package ru.illine.drinking.ponies.bot

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.abilitybots.api.bot.AbilityBot
import org.telegram.abilitybots.api.bot.BaseAbilityBot
import org.telegram.abilitybots.api.db.MapDBContext.offlineInstance
import org.telegram.abilitybots.api.objects.*
import org.telegram.abilitybots.api.toggle.BareboneToggle
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllPrivateChats
import ru.illine.drinking.ponies.config.property.TelegramBotProperties
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.DelayNotificationType
import ru.illine.drinking.ponies.model.base.ReplayType
import ru.illine.drinking.ponies.model.base.TelegramCommandType
import ru.illine.drinking.ponies.model.dto.NotificationDto
import ru.illine.drinking.ponies.util.MessageHelper
import ru.illine.drinking.ponies.util.TelegramBotHelper
import ru.illine.drinking.ponies.util.TelegramBotKeyboardHelper
import java.time.OffsetDateTime
import java.util.*
import java.util.function.BiConsumer


@Component
class DrinkingPoniesTelegramBot(
    private val telegramBotProperties: TelegramBotProperties,
    private val notificationAccessService: NotificationAccessService
) : AbilityBot(
    telegramBotProperties.token,
    telegramBotProperties.username,
    offlineInstance(telegramBotProperties.username),
    BareboneToggle()
) {

    private val log = LoggerFactory.getLogger("BOT")

    override fun creatorId() = telegramBotProperties.creatorId

    override fun onRegister() {
        super.onRegister()
        registerCommands()
    }

    fun startCommand() =
        Ability
            .builder()
            .name(TelegramCommandType.START.command)
            .info(TelegramCommandType.START.info)
            .locality(Locality.USER)
            .privacy(Privacy.PUBLIC)
            .action {
                SendMessage().apply {
                    text = MessageHelper.START_GREETING_MESSAGE.format(it.user().userName)
                    setChatId(it.chatId())
                }.apply { sender.execute(this) }

                SendMessage().apply {
                    text = MessageHelper.START_BUTTON_MESSAGE
                    setChatId(it.chatId())
                    replyMarkup = TelegramBotKeyboardHelper.delayTimeButtons()
                }.apply { sender.execute(this) }
            }
            .build()

    fun resumeCommand() =
        Ability
            .builder()
            .name(TelegramCommandType.RESUME.command)
            .info(TelegramCommandType.RESUME.info)
            .locality(Locality.USER)
            .privacy(Privacy.PUBLIC)
            .action {
                notificationAccessService.enableByUserId(it.user().id)

                SendMessage().apply {
                    text = MessageHelper.RESUME_GREETING_MESSAGE.format(it.user().userName)
                    setChatId(it.chatId())
                }.apply { sender.execute(this) }
            }
            .build()

    fun stopCommand() =
        Ability
            .builder()
            .name(TelegramCommandType.STOP.command)
            .info(TelegramCommandType.STOP.info)
            .locality(Locality.USER)
            .privacy(Privacy.PUBLIC)
            .action {
                notificationAccessService.disableByUserId(it.user().id)

                SendMessage().apply {
                    text = MessageHelper.STOP_GREETING_MESSAGE.format(it.user().userName)
                    setChatId(it.chatId())
                }.apply { sender.execute(this) }
            }
            .build()

    fun settingsCommand() =
        Ability
            .builder()
            .name(TelegramCommandType.SETTINGS.command)
            .info(TelegramCommandType.SETTINGS.info)
            .locality(Locality.USER)
            .privacy(Privacy.PUBLIC)
            .action {
                val currentNotification =
                    notificationAccessService.findByUserId(it.user().id).delayNotification

                SendMessage().apply {
                    text = MessageHelper.SETTINGS_GREETING_MESSAGE.format(currentNotification)
                    setChatId(it.chatId())
                }.apply { sender.execute(this) }

                SendMessage().apply {
                    text = MessageHelper.SETTINGS_BUTTON_MESSAGE
                    setChatId(it.chatId())
                    replyMarkup = TelegramBotKeyboardHelper.delayTimeButtons(currentNotification)
                }.apply { sender.execute(this) }
            }
            .build()

    fun replyButtons(): Reply {
        val action = BiConsumer<BaseAbilityBot, Update> { _, update ->
            val userId = update.callbackQuery.from.id
            val chatId = update.callbackQuery.message.chatId
            val messageId = update.callbackQuery.message.messageId
            val queryData = update.callbackQuery.data

            val replayType = TelegramBotHelper.getReplayType(queryData)

            when (replayType) {
                ReplayType.TIME_BUTTON -> sendReplayOnTimeButtons(queryData, userId, chatId, messageId)
                ReplayType.ANSWER_NOTIFICATION_BUTTON -> sendReplayOnNotificationAnswerButtons(
                    queryData,
                    userId,
                    chatId,
                    messageId
                )
            }
        }

        return Reply.of(action, Flag.CALLBACK_QUERY)
    }

    fun sendNotifications(notifications: Collection<NotificationDto>) {
        deletePreviousNotificationMessage(notifications)

        notifications
            .forEach {
                it.notificationAttempts += 1
                it.previousNotificationMessageId =
                    SendMessage().apply {
                        text = MessageHelper.NOTIFICATION_MESSAGE
                        setChatId(it.chatId)
                        replyMarkup = TelegramBotKeyboardHelper.notifyButtons()
                    }.let { sender.execute(it) }.messageId
            }

        notificationAccessService.updateNotifications(notifications)
    }

    fun suspendNotifications(notifications: Collection<NotificationDto>) {
        deletePreviousNotificationMessage(notifications)

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

    private fun deletePreviousNotificationMessage(notifications: Collection<NotificationDto>) {
        notifications.stream()
            .filter { it.previousNotificationMessageId != null }
            .forEach {
                DeleteMessage()
                    .apply {
                        setChatId(it.chatId)
                        messageId = it.previousNotificationMessageId!!
                    }.apply { sender.execute(this) }
            }
    }

    private fun sendReplayOnTimeButtons(queryData: String, userId: Long, chatId: Long, messageId: Int) {
        val delayNotification = DelayNotificationType.valueOf(queryData)

        log.info(
            "A user [{}] for chat [{}] with delay setting [{}] will be stored to a database",
            userId,
            chatId,
            delayNotification
        )

        log.info("A notification for user [{}] with delay setting [{}] will be saved", userId, delayNotification)
        val notification = NotificationDto.create(userId, chatId, delayNotification)
        val savedNotification = notificationAccessService.save(notification)
        log.info("The notification (id: [{}]) has saved", savedNotification.id)

        SendMessage().apply {
            text = MessageHelper.TIME_BUTTON_RESULT_MESSAGE.format(delayNotification.displayName)
            setChatId(chatId)
        }.apply { sender.execute(this) }

        deleteOldButtons(chatId, messageId)
    }

    private fun sendReplayOnNotificationAnswerButtons(
        queryData: String, userId: Long,
        chatId: Long, messageId: Int
    ) {
        val answerNotification = AnswerNotificationType.valueOf(queryData)

        when (answerNotification) {
            AnswerNotificationType.YES -> {
                notificationAccessService.updateTimeOfLastNotification(userId, OffsetDateTime.now())

                SendMessage().apply {
                    text = MessageHelper.NOTIFICATION_ANSWER_YES_MESSAGE
                    setChatId(chatId)
                }.apply { sender.execute(this) }
            }

            AnswerNotificationType.NO -> {
                val notification = notificationAccessService.findByUserId(userId)
                notificationAccessService
                    .updateTimeOfLastNotification(userId, notification.timeOfLastNotification.plusMinutes(10))

                SendMessage().apply {
                    text = MessageHelper.NOTIFICATION_ANSWER_NO_MESSAGE
                    setChatId(chatId)
                }.apply { sender.execute(this) }
            }

            AnswerNotificationType.CANCEL -> {
                notificationAccessService.updateTimeOfLastNotification(userId, OffsetDateTime.now())
                silent.send(
                    MessageHelper.NOTIFICATION_ANSWER_CANCEL_MESSAGE,
                    chatId
                )
            }
        }

        deleteOldButtons(chatId, messageId)
    }

    private fun deleteOldButtons(chatId: Long, messageId: Int) {
        EditMessageReplyMarkup()
            .apply {
                setChatId(chatId)
                setMessageId(messageId)
            }
            .apply { execute(this) }
    }

    private fun registerCommands() {
        if (telegramBotProperties.autoUpdateCommands) {
            log.warn("Telegram Commands will be updated!")
            EnumSet.allOf(TelegramCommandType::class.java)
                .stream()
                .filter { it.visible }
                .map { BotCommand(it.command, it.descriptions) }
                .toList()
                .let {
                    SetMyCommands()
                        .apply {
                            commands = it
                            scope = BotCommandScopeAllPrivateChats()
                        }
                }.apply { execute(this) }
        }
    }
}
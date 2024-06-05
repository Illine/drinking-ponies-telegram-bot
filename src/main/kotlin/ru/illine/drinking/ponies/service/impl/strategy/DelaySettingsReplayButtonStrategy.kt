package ru.illine.drinking.ponies.service.impl.strategy

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.SettingsType
import ru.illine.drinking.ponies.service.ButtonEditorService
import ru.illine.drinking.ponies.service.ReplyButtonStrategy
import ru.illine.drinking.ponies.util.MessageHelper
import ru.illine.drinking.ponies.util.TelegramBotKeyboardHelper
import java.util.*

@Service
class DelaySettingsReplayButtonStrategy(
    private val sender: TelegramClient,
    private val notificationAccessService: NotificationAccessService,
    private val buttonEditorService: ButtonEditorService
) : ReplyButtonStrategy {

    override fun reply(callbackQuery: CallbackQuery) {
        val chatId = callbackQuery.message.chatId
        val messageId = callbackQuery.message.messageId

        buttonEditorService.deleteReplyMarkup(chatId, messageId)

        val currentNotification =
            notificationAccessService.findByUserId(callbackQuery.from.id).delayNotification

        SendMessage(
            chatId.toString(),
            MessageHelper.SETTINGS_DELAY_NOTIFICATION_GREETING_MESSAGE.format(currentNotification.displayName)
        ).apply {
            enableMarkdown(true)
        }.apply { sender.execute(this) }

        SendMessage(
            chatId.toString(),
            MessageHelper.SETTINGS_DELAY_NOTIFICATION_BUTTON_MESSAGE
        ).apply {
            replyMarkup = TelegramBotKeyboardHelper.delayTimeButtons(currentNotification)
        }.apply { sender.execute(this) }
    }

    override fun isQueryData(queryData: String): Boolean {
        return Objects.equals(SettingsType.DELAY_NOTIFICATION.queryData.toString(), queryData)
    }
}
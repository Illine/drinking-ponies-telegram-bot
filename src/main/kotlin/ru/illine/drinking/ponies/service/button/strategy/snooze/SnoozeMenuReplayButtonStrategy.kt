package ru.illine.drinking.ponies.service.button.strategy.snooze

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.service.button.ReplyButtonStrategy
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.util.telegram.TelegramBotKeyboardHelper
import ru.illine.drinking.ponies.util.telegram.TelegramMessageConstants
import java.util.*

@Service
class SnoozeMenuReplayButtonStrategy(
    private val sender: TelegramClient,
    private val messageEditorService: MessageEditorService
) : ReplyButtonStrategy {

    override fun reply(callbackQuery: CallbackQuery) {
        val chatId = callbackQuery.message.chatId
        val messageId = callbackQuery.message.messageId
        val messageText =
            TelegramMessageConstants.NOTIFICATION_QUESTION_EDITED_MESSAGE_PATTERN.format(
                AnswerNotificationType.SNOOZE.displayName
            )

        messageEditorService.editReplyMarkup(messageText, chatId, messageId, true)

        SendMessage(
            chatId.toString(),
            TelegramMessageConstants.NOTIFICATION_SNOOZE_MENU_MESSAGE
        ).apply {
            replyMarkup = TelegramBotKeyboardHelper.snoozeTimeButtons()
        }.apply { sender.execute(this) }
    }

    override fun isQueryData(queryData: String): Boolean {
        return Objects.equals(AnswerNotificationType.SNOOZE.queryData.toString(), queryData)
    }

}

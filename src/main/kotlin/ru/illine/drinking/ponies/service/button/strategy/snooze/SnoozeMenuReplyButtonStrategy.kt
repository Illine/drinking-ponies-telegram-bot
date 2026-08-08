package ru.illine.drinking.ponies.service.button.strategy.snooze

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.dto.internal.NoContext
import ru.illine.drinking.ponies.model.dto.internal.NotificationQuestionEditedContext
import ru.illine.drinking.ponies.service.button.ReplyButtonStrategy
import ru.illine.drinking.ponies.service.message.MessageProvider
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.util.message.MessageSpec
import ru.illine.drinking.ponies.util.telegram.TelegramBotKeyboardHelper
import java.util.Objects

@Service
class SnoozeMenuReplyButtonStrategy(
    private val sender: TelegramClient,
    private val messageEditorService: MessageEditorService,
    private val messageProvider: MessageProvider,
) : ReplyButtonStrategy {
    override fun reply(callbackQuery: CallbackQuery) {
        val chatId = callbackQuery.message.chatId
        val messageId = callbackQuery.message.messageId
        val messageText =
            messageProvider
                .getMessage(
                    MessageSpec.NotificationQuestionEdited,
                    NotificationQuestionEditedContext(AnswerNotificationType.SNOOZE.displayName),
                ).text

        messageEditorService.editReplyMarkup(messageText, chatId, messageId, true)

        SendMessage(
            chatId.toString(),
            messageProvider.getMessage(MessageSpec.NotificationSnoozeMenu, NoContext).text,
        ).apply {
            replyMarkup = TelegramBotKeyboardHelper.snoozeTimeButtons()
        }.apply { sender.execute(this) }
    }

    override fun isQueryData(queryData: String): Boolean =
        Objects.equals(AnswerNotificationType.SNOOZE.queryData.toString(), queryData)
}

package ru.illine.drinking.ponies.service.button.strategy

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.service.MessageEditorService
import ru.illine.drinking.ponies.service.button.ReplyButtonStrategy
import ru.illine.drinking.ponies.util.TelegramConstants
import java.util.*

abstract class AbstractAnswerNotificationReplyButtonStrategy<T>(
    private val messageSender: TelegramClient,
    private val messageEditorService: MessageEditorService
) : ReplyButtonStrategy {

    protected val questionMessageEditedPattern = "%s\nВаш ответ: *%s*"

    override fun reply(callbackQuery: CallbackQuery) {
        editNotification(callbackQuery)

        updateLastNotificationTime(callbackQuery).invoke()

        SendMessage(
            callbackQuery.message.chatId.toString(), getMessageText()
        ).apply { messageSender.execute(this) }
    }

    override fun isQueryData(queryData: String): Boolean {
        return Objects.equals(getAnswerType().queryData.toString(), queryData)
    }

    protected fun editNotification(callbackQuery: CallbackQuery) {
        messageEditorService.editReplyMarkup(
            TelegramConstants.NOTIFICATION_QUESTION_EDITED_MESSAGE_PATTERN.format(getAnswerType().displayName),
            callbackQuery.message.chatId,
            callbackQuery.message.messageId,
            true
        )
    }

    abstract protected fun updateLastNotificationTime(callbackQuery: CallbackQuery): () -> T

    abstract protected fun getMessageText(): String

    abstract protected fun getAnswerType(): AnswerNotificationType

}
package ru.illine.drinking.ponies.service

import org.telegram.abilitybots.api.sender.MessageSender
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.util.MessageHelper
import java.util.*

abstract class AbstractAnswerNotificationReplyButtonStrategy<T>(
    private val messageSender: MessageSender,
    private val buttonEditorService: ButtonEditorService
) : ReplyButtonStrategy {

    protected val questionMessageEditedPattern = "%s\nВаш ответ: *%s*"

    override fun reply(callbackQuery: CallbackQuery) {
        editNotification(callbackQuery)

        updateLastNotificationTime(callbackQuery).invoke()

        SendMessage().apply {
            text = getMessageText()
            setChatId(callbackQuery.message.chatId)
        }.apply { messageSender.execute(this) }
    }

    override fun isQueryData(queryData: String): Boolean {
        return Objects.equals(getAnswerType().queryData.toString(), queryData)
    }

    protected fun editNotification(callbackQuery: CallbackQuery) {
        buttonEditorService.editReplyMarkup(
            MessageHelper.NOTIFICATION_QUESTION_EDITED_MESSAGE_PATTERN.format(getAnswerType().displayName),
            callbackQuery.message.chatId,
            callbackQuery.message.messageId,
            true
        )
    }

    abstract protected fun updateLastNotificationTime(callbackQuery: CallbackQuery): () -> T

    abstract protected fun getMessageText(): String

    abstract protected fun getAnswerType(): AnswerNotificationType

}
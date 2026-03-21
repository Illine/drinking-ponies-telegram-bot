package ru.illine.drinking.ponies.service.button.strategy.answer

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.service.MessageEditorService
import ru.illine.drinking.ponies.service.button.strategy.AbstractAnswerNotificationReplyButtonStrategy
import ru.illine.drinking.ponies.util.TelegramBotKeyboardHelper
import ru.illine.drinking.ponies.util.TelegramMessageConstants

@Service
class DelayAnswerNotificationReplayButtonStrategy(
    sender: TelegramClient,
    messageEditorService: MessageEditorService
) : AbstractAnswerNotificationReplyButtonStrategy<Unit>(sender, messageEditorService) {

    override fun reply(callbackQuery: CallbackQuery) {
        editNotification(callbackQuery)

        SendMessage(
            callbackQuery.message.chatId.toString(),
            getMessageText()
        ).apply {
            replyMarkup = TelegramBotKeyboardHelper.delayOptionButtons()
            messageSender.execute(this)
        }
    }

    override fun updateLastNotificationTime(callbackQuery: CallbackQuery): () -> Unit = {}

    override fun getMessageText(): String = TelegramMessageConstants.NOTIFICATION_ANSWER_DELAY_MESSAGE

    override fun getAnswerType(): AnswerNotificationType = AnswerNotificationType.DELAY
}

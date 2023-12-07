package ru.illine.drinking.ponies.service.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.abilitybots.api.sender.MessageSender
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import ru.illine.drinking.ponies.service.ButtonEditorService
import ru.illine.drinking.ponies.util.FunctionHelper

@Service
class ButtonEditorServiceImpl(
    private val sender: MessageSender
) : ButtonEditorService {

    private val log = LoggerFactory.getLogger("SERVICE")

    override fun deleteReplyMarkup(chatId: Long, messageId: Int) {
        EditMessageReplyMarkup()
            .apply {
                setChatId(chatId)
                setMessageId(messageId)
            }
            .apply { sender.execute(this) }
    }

    override fun editReplyMarkup(newText: String, chatId: Long, messageId: Int, enableMarkDown: Boolean) {
        deleteReplyMarkup(chatId, messageId)

        EditMessageText()
            .apply {
                setChatId(chatId)
                setMessageId(messageId)
                text = newText
                enableMarkdown(enableMarkDown)
            }
            .apply { sender.execute(this) }
    }

    override fun deleteMessage(chatId: Long, messageId: Int) {
        deleteMessage(Pair(chatId, messageId))
    }

    override fun deleteMessage(messageInfo: Pair<Long, Int>) {
        DeleteMessage()
            .apply {
                setChatId(messageInfo.first)
                messageId = messageInfo.second
            }.apply {
                FunctionHelper.catchAny(
                    action = { sender.execute(this) },
                    errorLogging = {
                        log.error(
                            "A previous message can't be deleted! A chatId: [${this.chatId}], messageId: [${this.messageId}]",
                            it
                        )
                    }
                )
            }
    }

    override fun deleteMessages(messageInfo: Collection<Pair<Long, Int>>) {
        messageInfo.forEach { deleteMessage(it) }
    }
}
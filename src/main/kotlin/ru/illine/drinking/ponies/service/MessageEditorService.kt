package ru.illine.drinking.ponies.service

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup

interface MessageEditorService {

    fun deleteReplyMarkup(chatId: Long, messageId: Int)

    fun editReplyMarkup(
        newText: String,
        chatId: Long,
        messageId: Int,
        enableMarkDown: Boolean = false,
        replayKeyboard: InlineKeyboardMarkup? = null
    )

    fun deleteMessage(chatId: Long, messageId: Int)

    fun deleteMessage(messageInfo: Pair<Long, Int>)

    fun deleteMessages(messageInfo: Collection<Pair<Long, Int>>)
}
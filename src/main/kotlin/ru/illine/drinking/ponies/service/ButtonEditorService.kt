package ru.illine.drinking.ponies.service

interface ButtonEditorService {

    fun deleteReplyMarkup(chatId: Long, messageId: Int)

    fun editReplyMarkup(newText: String, chatId: Long, messageId: Int, enableMarkDown: Boolean = false)

    fun deleteMessage(chatId: Long, messageId: Int)

    fun deleteMessage(messageInfo: Pair<Long, Int>)

    fun deleteMessages(messageInfo: Collection<Pair<Long, Int>>)
}
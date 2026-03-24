package ru.illine.drinking.ponies.service.telegram

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.service.telegram.impl.MessageEditorServiceImpl
import ru.illine.drinking.ponies.test.tag.UnitTest

@UnitTest
@DisplayName("MessageEditorService Unit Test")
class MessageEditorServiceTest {

    private val chatId = 1L
    private val messageId = 2

    private lateinit var sender: TelegramClient
    private lateinit var service: MessageEditorServiceImpl

    @BeforeEach
    fun setUp() {
        sender = mock(TelegramClient::class.java)
        service = MessageEditorServiceImpl(sender)
    }

    @Test
    @DisplayName("deleteReplyMarkup(): executes EditMessageReplyMarkup via sender")
    fun `deleteReplyMarkup executes edit`() {
        service.deleteReplyMarkup(chatId, messageId)

        verify(sender).execute(any<EditMessageReplyMarkup>())
    }

    @Test
    @DisplayName("editReplyMarkup(): deletes old markup and sends new text without keyboard")
    fun `editReplyMarkup without keyboard`() {
        service.editReplyMarkup("new text", chatId, messageId)

        verify(sender).execute(any<EditMessageReplyMarkup>())
        verify(sender).execute(any<EditMessageText>())
    }

    @Test
    @DisplayName("editReplyMarkup(): deletes old markup and sends new text with keyboard")
    fun `editReplyMarkup with keyboard`() {
        val keyboard = mock(InlineKeyboardMarkup::class.java)

        service.editReplyMarkup("new text", chatId, messageId, replayKeyboard = keyboard)

        verify(sender).execute(any<EditMessageReplyMarkup>())
        verify(sender).execute(any<EditMessageText>())
    }

    @Test
    @DisplayName("deleteMessage(): executes DeleteMessage via sender")
    fun `deleteMessage by chatId and messageId`() {
        service.deleteMessage(chatId, messageId)

        verify(sender).execute(any<DeleteMessage>())
    }

    @Test
    @DisplayName("deleteMessage(): executes DeleteMessage via sender using Pair")
    fun `deleteMessage by pair`() {
        service.deleteMessage(Pair(chatId, messageId))

        verify(sender).execute(any<DeleteMessage>())
    }

    @Test
    @DisplayName("deleteMessages(): executes DeleteMessage for each element")
    fun `deleteMessages with elements`() {
        val messages = listOf(
            Pair(chatId, 1),
            Pair(chatId, 2)
        )

        service.deleteMessages(messages)

        verify(sender, times(2)).execute(any<DeleteMessage>())
    }

    @Test
    @DisplayName("deleteMessages(): does nothing for empty collection")
    fun `deleteMessages with empty collection`() {
        service.deleteMessages(emptyList())

        verifyNoInteractions(sender)
    }

    @Test
    @DisplayName("deleteMessage(): catches exception and does not rethrow")
    fun `deleteMessage catches exception`() {
        doThrow(RuntimeException("Telegram error")).`when`(sender).execute(any<DeleteMessage>())

        service.deleteMessage(chatId, messageId)

        verify(sender).execute(any<DeleteMessage>())
    }
}

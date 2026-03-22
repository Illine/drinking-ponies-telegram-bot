package ru.illine.drinking.ponies.service.button.strategy.snooze

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.SnoozeNotificationType
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.telegram.TelegramMessageConstants

@UnitTest
@DisplayName("SnoozeMenuReplayButtonStrategy Unit Test")
class SnoozeMenuReplayButtonStrategyTest {

    private val chatId = 100500L
    private val messageId = 42

    private lateinit var sender: TelegramClient
    private lateinit var messageEditorService: MessageEditorService
    private lateinit var strategy: SnoozeMenuReplayButtonStrategy

    @BeforeEach
    fun setUp() {
        sender = mock(TelegramClient::class.java)
        messageEditorService = mock(MessageEditorService::class.java)
        strategy = SnoozeMenuReplayButtonStrategy(sender, messageEditorService)
    }

    // reply

    @Test
    @DisplayName("reply(): edits original message with snooze display name")
    fun `reply edits original message`() {
        val callbackQuery = buildCallbackQuery()

        strategy.reply(callbackQuery)

        val expectedText = TelegramMessageConstants.NOTIFICATION_QUESTION_EDITED_MESSAGE_PATTERN
            .format(AnswerNotificationType.SNOOZE.displayName)
        verify(messageEditorService).editReplyMarkup(expectedText, chatId, messageId, true)
    }

    @Test
    @DisplayName("reply(): sends snooze menu message with buttons")
    fun `reply sends snooze menu message`() {
        val callbackQuery = buildCallbackQuery()
        val captor = ArgumentCaptor.forClass(SendMessage::class.java)

        strategy.reply(callbackQuery)

        verify(sender).execute(captor.capture())
        val sent = captor.value
        assertEquals(chatId.toString(), sent.chatId)
        assertEquals(TelegramMessageConstants.NOTIFICATION_SNOOZE_MENU_MESSAGE, sent.text)
        val buttons = (sent.replyMarkup as org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup).keyboard
        assertEquals(SnoozeNotificationType.entries.size, buttons.size)
    }

    @Test
    @DisplayName("isQueryData(): returns true for SNOOZE queryData")
    fun `isQueryData returns true for snooze uuid`() {
        val result = strategy.isQueryData(AnswerNotificationType.SNOOZE.queryData.toString())
        assertTrue(result)
    }

    @ParameterizedTest
    @EnumSource(AnswerNotificationType::class, names = ["SNOOZE"], mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("isQueryData(): returns false for non-SNOOZE answer types")
    fun `isQueryData returns false for other answer types`(type: AnswerNotificationType) {
        val result = strategy.isQueryData(type.queryData.toString())
        assertFalse(result)
    }

    @Test
    @DisplayName("isQueryData(): returns false for random string")
    fun `isQueryData returns false for random string`() {
        val result = strategy.isQueryData("not-a-uuid")
        assertFalse(result)
    }

    private fun buildCallbackQuery(): CallbackQuery {
        val message = mock(Message::class.java)
        `when`(message.chatId).thenReturn(chatId)
        `when`(message.messageId).thenReturn(messageId)

        val callbackQuery = mock(CallbackQuery::class.java)
        `when`(callbackQuery.message).thenReturn(message)
        return callbackQuery
    }
}

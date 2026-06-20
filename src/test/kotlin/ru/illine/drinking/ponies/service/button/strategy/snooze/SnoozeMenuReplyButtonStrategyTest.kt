package ru.illine.drinking.ponies.service.button.strategy.snooze

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
@DisplayName("SnoozeMenuReplyButtonStrategy Unit Test")
class SnoozeMenuReplyButtonStrategyTest {

    private val chatId = 1L
    private val messageId = 2

    private lateinit var sender: TelegramClient
    private lateinit var messageEditorService: MessageEditorService
    private lateinit var strategy: SnoozeMenuReplyButtonStrategy

    @BeforeEach
    fun setUp() {
        sender = mock<TelegramClient>()
        messageEditorService = mock<MessageEditorService>()
        strategy = SnoozeMenuReplyButtonStrategy(sender, messageEditorService)
    }

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
        val captor = argumentCaptor<SendMessage>()

        strategy.reply(callbackQuery)

        verify(sender).execute(captor.capture())
        val sent = captor.firstValue
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
        val message = mock<Message>()
        whenever(message.chatId).thenReturn(chatId)
        whenever(message.messageId).thenReturn(messageId)

        val callbackQuery = mock<CallbackQuery>()
        whenever(callbackQuery.message).thenReturn(message)
        return callbackQuery
    }
}

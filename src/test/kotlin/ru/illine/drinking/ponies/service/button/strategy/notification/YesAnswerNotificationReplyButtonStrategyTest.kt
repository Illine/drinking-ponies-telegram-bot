package ru.illine.drinking.ponies.service.button.strategy.notification

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.WaterAmountType
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.telegram.TelegramMessageConstants

@UnitTest
@DisplayName("YesAnswerNotificationReplyButtonStrategy Unit Test")
class YesAnswerNotificationReplyButtonStrategyTest {

    private val chatId = 1L
    private val messageId = 2

    private lateinit var sender: TelegramClient
    private lateinit var messageEditorService: MessageEditorService
    private lateinit var strategy: YesAnswerNotificationReplyButtonStrategy

    @BeforeEach
    fun setUp() {
        sender = mock(TelegramClient::class.java)
        messageEditorService = mock(MessageEditorService::class.java)
        strategy = YesAnswerNotificationReplyButtonStrategy(sender, messageEditorService)
    }

    @Test
    @DisplayName("reply(): edits original message with YES display name")
    fun `reply edits original message`() {
        strategy.reply(buildCallbackQuery())

        val expectedText = TelegramMessageConstants.NOTIFICATION_QUESTION_EDITED_MESSAGE_PATTERN
            .format(AnswerNotificationType.YES.displayName)
        verify(messageEditorService).editReplyMarkup(expectedText, chatId, messageId, true)
    }

    @Test
    @DisplayName("reply(): sends water amount menu message with buttons")
    fun `reply sends water amount menu message`() {
        val captor = ArgumentCaptor.forClass(SendMessage::class.java)

        strategy.reply(buildCallbackQuery())

        verify(sender).execute(captor.capture())
        val sent = captor.value
        assertEquals(chatId.toString(), sent.chatId)
        assertEquals(TelegramMessageConstants.NOTIFICATION_WATER_AMOUNT_MENU_MESSAGE, sent.text)
        val buttons = (sent.replyMarkup as InlineKeyboardMarkup).keyboard
        assertEquals(WaterAmountType.entries.size, buttons.size)
    }

    @Test
    @DisplayName("reply(): only interacts with sender and messageEditorService")
    fun `reply has no side effects beyond menu display`() {
        strategy.reply(buildCallbackQuery())

        verify(messageEditorService).editReplyMarkup(any<String>(), any<Long>(), any<Int>(), any<Boolean>(), anyOrNull())
        verify(sender).execute(any<SendMessage>())
        verifyNoMoreInteractions(messageEditorService)
        verifyNoMoreInteractions(sender)
    }

    @Test
    @DisplayName("isQueryData(): returns true for YES queryData")
    fun `isQueryData returns true for yes uuid`() {
        val result = strategy.isQueryData(AnswerNotificationType.YES.queryData.toString())
        assertTrue(result)
    }

    @ParameterizedTest
    @EnumSource(AnswerNotificationType::class, names = ["YES"], mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("isQueryData(): returns false for non-YES answer types")
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

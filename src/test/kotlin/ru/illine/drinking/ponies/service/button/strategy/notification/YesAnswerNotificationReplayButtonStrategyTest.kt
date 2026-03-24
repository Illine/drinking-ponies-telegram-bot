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
import org.mockito.kotlin.eq
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.telegram.TelegramMessageConstants
import java.time.LocalDateTime

@UnitTest
@DisplayName("YesAnswerNotificationReplayButtonStrategy Unit Test")
class YesAnswerNotificationReplayButtonStrategyTest {

    private val userId = 1L
    private val chatId = 2L
    private val messageId = 3

    private lateinit var sender: TelegramClient
    private lateinit var messageEditorService: MessageEditorService
    private lateinit var notificationAccessService: NotificationAccessService
    private lateinit var strategy: YesAnswerNotificationReplayButtonStrategy

    @BeforeEach
    fun setUp() {
        sender = mock(TelegramClient::class.java)
        messageEditorService = mock(MessageEditorService::class.java)
        notificationAccessService = mock(NotificationAccessService::class.java)
        strategy = YesAnswerNotificationReplayButtonStrategy(sender, messageEditorService, notificationAccessService)
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
    @DisplayName("reply(): updates last notification time to now()")
    fun `reply updates notification time`() {
        strategy.reply(buildCallbackQuery())

        verify(notificationAccessService).updateTimeOfLastNotification(eq(userId), any<LocalDateTime>())
    }

    @Test
    @DisplayName("reply(): sends YES confirmation message")
    fun `reply sends confirmation message`() {
        val captor = ArgumentCaptor.forClass(SendMessage::class.java)
        strategy.reply(buildCallbackQuery())

        verify(sender).execute(captor.capture())
        val sent = captor.value
        assertEquals(chatId.toString(), sent.chatId)
        assertEquals(TelegramMessageConstants.NOTIFICATION_ANSWER_YES_MESSAGE, sent.text)
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
        val user = mock(User::class.java)
        `when`(user.id).thenReturn(userId)

        val message = mock(Message::class.java)
        `when`(message.chatId).thenReturn(chatId)
        `when`(message.messageId).thenReturn(messageId)

        val callbackQuery = mock(CallbackQuery::class.java)
        `when`(callbackQuery.from).thenReturn(user)
        `when`(callbackQuery.message).thenReturn(message)
        return callbackQuery
    }
}

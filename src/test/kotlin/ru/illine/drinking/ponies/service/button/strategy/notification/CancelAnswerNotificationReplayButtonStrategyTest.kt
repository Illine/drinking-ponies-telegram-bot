package ru.illine.drinking.ponies.service.button.strategy.notification

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
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.telegram.TelegramMessageConstants
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset

@UnitTest
@DisplayName("CancelAnswerNotificationReplayButtonStrategy Unit Test")
class CancelAnswerNotificationReplayButtonStrategyTest {

    private val userId = 1L
    private val chatId = 2L
    private val messageId = 3
    private val fixedNow = LocalDateTime.of(2025, 1, 1, 14, 0, 0)
    private val fixedClock = Clock.fixed(fixedNow.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private lateinit var sender: TelegramClient
    private lateinit var messageEditorService: MessageEditorService
    private lateinit var notificationAccessService: NotificationAccessService
    private lateinit var strategy: CancelAnswerNotificationReplayButtonStrategy

    @BeforeEach
    fun setUp() {
        sender = mock(TelegramClient::class.java)
        messageEditorService = mock(MessageEditorService::class.java)
        notificationAccessService = mock(NotificationAccessService::class.java)
        strategy = CancelAnswerNotificationReplayButtonStrategy(sender, messageEditorService, notificationAccessService, fixedClock)
    }

    @Test
    @DisplayName("reply(): edits original message with CANCEL display name")
    fun `reply edits original message`() {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        `when`(notificationAccessService.updateTimeOfLastNotification(userId, fixedNow)).thenReturn(notificationDto)

        strategy.reply(buildCallbackQuery())

        val expectedText = TelegramMessageConstants.NOTIFICATION_QUESTION_EDITED_MESSAGE_PATTERN
            .format(AnswerNotificationType.CANCEL.displayName)
        verify(messageEditorService).editReplyMarkup(expectedText, chatId, messageId, true)
    }

    @Test
    @DisplayName("reply(): updates last notification time to now(clock)")
    fun `reply updates notification time`() {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        `when`(notificationAccessService.updateTimeOfLastNotification(userId, fixedNow)).thenReturn(notificationDto)

        strategy.reply(buildCallbackQuery())

        verify(notificationAccessService).updateTimeOfLastNotification(userId, fixedNow)
    }

    @Test
    @DisplayName("reply(): sends CANCEL confirmation message")
    fun `reply sends confirmation message`() {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        `when`(notificationAccessService.updateTimeOfLastNotification(userId, fixedNow)).thenReturn(notificationDto)

        val captor = ArgumentCaptor.forClass(SendMessage::class.java)
        strategy.reply(buildCallbackQuery())

        verify(sender).execute(captor.capture())
        val sent = captor.value
        assertEquals(chatId.toString(), sent.chatId)
        assertEquals(TelegramMessageConstants.NOTIFICATION_ANSWER_CANCEL_MESSAGE, sent.text)
    }

    @Test
    @DisplayName("isQueryData(): returns true for CANCEL queryData")
    fun `isQueryData returns true for cancel uuid`() {
        val result = strategy.isQueryData(AnswerNotificationType.CANCEL.queryData.toString())
        assertTrue(result)
    }

    @ParameterizedTest
    @EnumSource(AnswerNotificationType::class, names = ["CANCEL"], mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("isQueryData(): returns false for non-CANCEL answer types")
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

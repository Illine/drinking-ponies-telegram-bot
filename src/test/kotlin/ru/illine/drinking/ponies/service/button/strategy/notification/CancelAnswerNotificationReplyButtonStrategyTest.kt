package ru.illine.drinking.ponies.service.button.strategy.notification

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.service.notification.NotificationSettingsService
import ru.illine.drinking.ponies.service.statistic.WaterStatisticService
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.telegram.TelegramMessageConstants
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset

@UnitTest
@DisplayName("CancelAnswerNotificationReplyButtonStrategy Unit Test")
class CancelAnswerNotificationReplyButtonStrategyTest {
    private val externalUserId = 1L
    private val chatId = 2L
    private val messageId = 3
    private val fixedNow = LocalDateTime.of(2025, 1, 1, 14, 0, 0)
    private val fixedClock = Clock.fixed(fixedNow.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private lateinit var sender: TelegramClient
    private lateinit var messageEditorService: MessageEditorService
    private lateinit var notificationSettingsService: NotificationSettingsService
    private lateinit var waterStatisticService: WaterStatisticService
    private lateinit var strategy: CancelAnswerNotificationReplyButtonStrategy

    @BeforeEach
    fun setUp() {
        sender = mock<TelegramClient>()
        messageEditorService = mock<MessageEditorService>()
        notificationSettingsService = mock<NotificationSettingsService>()
        waterStatisticService = mock<WaterStatisticService>()
        strategy =
            CancelAnswerNotificationReplyButtonStrategy(
                sender,
                messageEditorService,
                notificationSettingsService,
                waterStatisticService,
                fixedClock,
            )
    }

    @Test
    @DisplayName("reply(): edits original message with CANCEL display name")
    fun `reply edits original message`() {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId)
        whenever(
            notificationSettingsService.resetNotificationTimer(externalUserId, fixedNow),
        ).thenReturn(notificationDto)

        strategy.reply(buildCallbackQuery())

        val expectedText =
            TelegramMessageConstants.NOTIFICATION_QUESTION_EDITED_MESSAGE_PATTERN
                .format(AnswerNotificationType.CANCEL.displayName)
        verify(messageEditorService).editReplyMarkup(expectedText, chatId, messageId, true)
    }

    @Test
    @DisplayName("reply(): updates last notification time to now(clock)")
    fun `reply updates notification time`() {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId)
        whenever(
            notificationSettingsService.resetNotificationTimer(externalUserId, fixedNow),
        ).thenReturn(notificationDto)

        strategy.reply(buildCallbackQuery())

        verify(notificationSettingsService).resetNotificationTimer(externalUserId, fixedNow)
    }

    @Test
    @DisplayName("reply(): records water statistic with CANCEL event type")
    fun `reply records statistic`() {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId)
        whenever(
            notificationSettingsService.resetNotificationTimer(externalUserId, fixedNow),
        ).thenReturn(notificationDto)

        strategy.reply(buildCallbackQuery())

        verify(waterStatisticService).recordEvent(notificationDto.telegramUser, AnswerNotificationType.CANCEL)
    }

    @Test
    @DisplayName("reply(): sends CANCEL confirmation message")
    fun `reply sends confirmation message`() {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId)
        whenever(
            notificationSettingsService.resetNotificationTimer(externalUserId, fixedNow),
        ).thenReturn(notificationDto)

        val captor = argumentCaptor<SendMessage>()
        strategy.reply(buildCallbackQuery())

        verify(sender).execute(captor.capture())
        val sent = captor.firstValue
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

    @Test
    @DisplayName("reply(): sends CANCEL confirmation message even when recordEvent throws an exception")
    fun `reply sends confirmation message when recordEvent throws`() {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId)
        whenever(
            notificationSettingsService.resetNotificationTimer(externalUserId, fixedNow),
        ).thenReturn(notificationDto)
        doThrow(RuntimeException("statistic error"))
            .whenever(waterStatisticService)
            .recordEvent(any(), any(), any<Int>())

        val captor = argumentCaptor<SendMessage>()
        strategy.reply(buildCallbackQuery())

        verify(sender).execute(captor.capture())
        val sent = captor.firstValue
        assertEquals(chatId.toString(), sent.chatId)
        assertEquals(TelegramMessageConstants.NOTIFICATION_ANSWER_CANCEL_MESSAGE, sent.text)
    }

    private fun buildCallbackQuery(): CallbackQuery {
        val user = mock<User>()
        whenever(user.id).thenReturn(externalUserId)

        val message = mock<Message>()
        whenever(message.chatId).thenReturn(chatId)
        whenever(message.messageId).thenReturn(messageId)

        val callbackQuery = mock<CallbackQuery>()
        whenever(callbackQuery.from).thenReturn(user)
        whenever(callbackQuery.message).thenReturn(message)
        return callbackQuery
    }
}

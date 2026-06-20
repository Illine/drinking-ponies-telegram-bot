package ru.illine.drinking.ponies.service.button.strategy.snooze

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.service.notification.NotificationSettingsService
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.SnoozeNotificationType
import ru.illine.drinking.ponies.service.statistic.WaterStatisticService
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.telegram.TelegramMessageConstants
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset

@UnitTest
@DisplayName("SnoozeApplyReplyButtonStrategy Unit Test")
class SnoozeApplyReplyButtonStrategyTest {

    private val externalUserId = 1L
    private val chatId = 2L
    private val messageId = 3
    private val fixedNow = LocalDateTime.of(2025, 1, 1, 14, 0, 0)
    private val fixedClock = Clock.fixed(fixedNow.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private lateinit var sender: TelegramClient
    private lateinit var notificationSettingsService: NotificationSettingsService
    private lateinit var messageEditorService: MessageEditorService
    private lateinit var waterStatisticService: WaterStatisticService
    private lateinit var strategy: SnoozeApplyReplyButtonStrategy

    @BeforeEach
    fun setUp() {
        sender = mock<TelegramClient>()
        notificationSettingsService = mock<NotificationSettingsService>()
        messageEditorService = mock<MessageEditorService>()
        waterStatisticService = mock<WaterStatisticService>()
        strategy = SnoozeApplyReplyButtonStrategy(
            sender,
            notificationSettingsService,
            waterStatisticService,
            messageEditorService,
            fixedClock
        )
    }

    @ParameterizedTest
    @EnumSource(SnoozeNotificationType::class)
    @DisplayName("reply(): deletes reply markup on original message")
    fun `reply deletes reply markup`(snoozeType: SnoozeNotificationType) {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId)
        whenever(notificationSettingsService.getNotificationSettings(externalUserId)).thenReturn(notificationDto)
        whenever(notificationSettingsService.resetNotificationTimer(eq(externalUserId), any())).thenReturn(notificationDto)

        val callbackQuery = buildCallbackQuery(snoozeType.queryData.toString())
        strategy.reply(callbackQuery)

        verify(messageEditorService).deleteReplyMarkup(chatId, messageId)
    }

    @ParameterizedTest
    @EnumSource(SnoozeNotificationType::class)
    @DisplayName("reply(): next notification fires exactly snoozeMinutes from now")
    fun `reply updates notification time`(snoozeType: SnoozeNotificationType) {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId)
        whenever(notificationSettingsService.getNotificationSettings(externalUserId)).thenReturn(notificationDto)
        whenever(notificationSettingsService.resetNotificationTimer(eq(externalUserId), any())).thenReturn(notificationDto)

        val callbackQuery = buildCallbackQuery(snoozeType.queryData.toString())
        strategy.reply(callbackQuery)

        val interval = notificationDto.notificationInterval.minutes
        val expectedTime = fixedNow.minusMinutes(interval).plusMinutes(snoozeType.minutes)
        verify(notificationSettingsService).resetNotificationTimer(externalUserId, expectedTime)
    }

    @ParameterizedTest
    @EnumSource(SnoozeNotificationType::class)
    @DisplayName("reply(): sends confirmation message with snooze display name")
    fun `reply sends confirmation message`(snoozeType: SnoozeNotificationType) {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId)
        whenever(notificationSettingsService.getNotificationSettings(externalUserId)).thenReturn(notificationDto)
        whenever(notificationSettingsService.resetNotificationTimer(eq(externalUserId), any())).thenReturn(notificationDto)

        val callbackQuery = buildCallbackQuery(snoozeType.queryData.toString())
        val captor = argumentCaptor<SendMessage>()

        strategy.reply(callbackQuery)

        verify(sender).execute(captor.capture())
        val sent = captor.firstValue
        assertEquals(chatId.toString(), sent.chatId)
        assertEquals(
            TelegramMessageConstants.NOTIFICATION_SUSPEND_MESSAGE.format(snoozeType.displayName),
            sent.text
        )
    }

    @Test
    @DisplayName("reply(): falls back to TEN_MINS when queryData doesn't match any SnoozeNotificationType")
    fun `reply falls back to TEN_MINS for unknown queryData`() {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId)
        whenever(notificationSettingsService.getNotificationSettings(externalUserId)).thenReturn(notificationDto)
        whenever(notificationSettingsService.resetNotificationTimer(eq(externalUserId), any())).thenReturn(notificationDto)

        val callbackQuery = buildCallbackQuery("00000000-0000-0000-0000-000000000000")
        strategy.reply(callbackQuery)

        val interval = notificationDto.notificationInterval.minutes
        val expectedTime = fixedNow.minusMinutes(interval).plusMinutes(SnoozeNotificationType.TEN_MINS.minutes)
        verify(notificationSettingsService).resetNotificationTimer(externalUserId, expectedTime)
    }

    @ParameterizedTest
    @EnumSource(SnoozeNotificationType::class)
    @DisplayName("reply(): records water statistic with SNOOZE event type")
    fun `reply records statistic any type`(snoozeType: SnoozeNotificationType) {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId)
        whenever(notificationSettingsService.getNotificationSettings(externalUserId)).thenReturn(notificationDto)
        whenever(notificationSettingsService.resetNotificationTimer(eq(externalUserId), any())).thenReturn(notificationDto)

        val callbackQuery = buildCallbackQuery(snoozeType.queryData.toString())

        strategy.reply(callbackQuery)

        verify(waterStatisticService).recordEvent(notificationDto.telegramUser, AnswerNotificationType.SNOOZE)
    }

    @ParameterizedTest
    @EnumSource(SnoozeNotificationType::class)
    @DisplayName("isQueryData(): returns true for each SnoozeNotificationType queryData")
    fun `isQueryData returns true for snooze types`(snoozeType: SnoozeNotificationType) {
        val result = strategy.isQueryData(snoozeType.queryData.toString())
        assertTrue(result)
    }

    @Test
    @DisplayName("isQueryData(): returns false for random string")
    fun `isQueryData returns false for random string`() {
        val result = strategy.isQueryData("not-a-uuid")
        assertFalse(result)
    }

    @Test
    @DisplayName("isQueryData(): returns false for unknown UUID")
    fun `isQueryData returns false for unknown uuid`() {
        val result = strategy.isQueryData("00000000-0000-0000-0000-000000000000")
        assertFalse(result)
    }

    @Test
    @DisplayName("reply(): sends snooze confirmation message even when recordEvent throws an exception")
    fun `reply sends confirmation message when recordEvent throws`() {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId)
        whenever(notificationSettingsService.getNotificationSettings(externalUserId)).thenReturn(notificationDto)
        whenever(notificationSettingsService.resetNotificationTimer(any(), any())).thenReturn(notificationDto)
        doThrow(RuntimeException("statistic error")).whenever(waterStatisticService)
            .recordEvent(any(), any(), any<Int>())

        val snoozeType = SnoozeNotificationType.TEN_MINS
        val captor = argumentCaptor<SendMessage>()
        strategy.reply(buildCallbackQuery(snoozeType.queryData.toString()))

        verify(sender).execute(captor.capture())
        val sent = captor.firstValue
        assertEquals(chatId.toString(), sent.chatId)
        assertEquals(TelegramMessageConstants.NOTIFICATION_SUSPEND_MESSAGE.format(snoozeType.displayName), sent.text)
    }

    private fun buildCallbackQuery(queryData: String): CallbackQuery {
        val user = mock<User>()
        whenever(user.id).thenReturn(externalUserId)

        val message = mock<Message>()
        whenever(message.chatId).thenReturn(chatId)
        whenever(message.messageId).thenReturn(messageId)

        val callbackQuery = mock<CallbackQuery>()
        whenever(callbackQuery.from).thenReturn(user)
        whenever(callbackQuery.message).thenReturn(message)
        whenever(callbackQuery.data).thenReturn(queryData)
        return callbackQuery
    }

}

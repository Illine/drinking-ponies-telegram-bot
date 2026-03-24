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
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.SnoozeNotificationType
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.telegram.TelegramMessageConstants
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset

@UnitTest
@DisplayName("SnoozeApplyReplayButtonStrategy Unit Test")
class SnoozeApplyReplayButtonStrategyTest {

    private val userId = 1L
    private val chatId = 2L
    private val messageId = 3
    private val fixedNow = LocalDateTime.of(2025, 1, 1, 14, 0, 0)
    private val fixedClock = Clock.fixed(fixedNow.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private lateinit var sender: TelegramClient
    private lateinit var notificationAccessService: NotificationAccessService
    private lateinit var messageEditorService: MessageEditorService
    private lateinit var strategy: SnoozeApplyReplayButtonStrategy

    @BeforeEach
    fun setUp() {
        sender = mock(TelegramClient::class.java)
        notificationAccessService = mock(NotificationAccessService::class.java)
        messageEditorService = mock(MessageEditorService::class.java)
        strategy = SnoozeApplyReplayButtonStrategy(sender, notificationAccessService, messageEditorService, fixedClock)
    }

    @ParameterizedTest
    @EnumSource(SnoozeNotificationType::class)
    @DisplayName("reply(): deletes reply markup on original message")
    fun `reply deletes reply markup`(snoozeType: SnoozeNotificationType) {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        `when`(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(notificationDto)

        val callbackQuery = buildCallbackQuery(snoozeType.queryData.toString())
        strategy.reply(callbackQuery)

        verify(messageEditorService).deleteReplyMarkup(chatId, messageId)
    }

    @ParameterizedTest
    @EnumSource(SnoozeNotificationType::class)
    @DisplayName("reply(): next notification fires exactly snoozeMinutes from now")
    fun `reply updates notification time`(snoozeType: SnoozeNotificationType) {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        `when`(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(notificationDto)

        val callbackQuery = buildCallbackQuery(snoozeType.queryData.toString())
        strategy.reply(callbackQuery)

        val interval = notificationDto.notificationInterval.minutes
        val expectedTime = fixedNow.minusMinutes(interval).plusMinutes(snoozeType.minutes)
        verify(notificationAccessService).updateTimeOfLastNotification(userId, expectedTime)
    }

    @ParameterizedTest
    @EnumSource(SnoozeNotificationType::class)
    @DisplayName("reply(): sends confirmation message with snooze display name")
    fun `reply sends confirmation message`(snoozeType: SnoozeNotificationType) {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        `when`(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(notificationDto)

        val callbackQuery = buildCallbackQuery(snoozeType.queryData.toString())
        val captor = ArgumentCaptor.forClass(SendMessage::class.java)

        strategy.reply(callbackQuery)

        verify(sender).execute(captor.capture())
        val sent = captor.value
        assertEquals(chatId.toString(), sent.chatId)
        assertEquals(
            TelegramMessageConstants.NOTIFICATION_SUSPEND_MESSAGE.format(snoozeType.displayName),
            sent.text
        )
    }

    @ParameterizedTest
    @EnumSource(SnoozeNotificationType::class)
    @DisplayName("isQueryData(): returns true for each SnoozeNotificationType queryData")
    fun `isQueryData returns true for snooze types`(snoozeType: SnoozeNotificationType) {
        val result = strategy.isQueryData(snoozeType.queryData.toString())
        assertTrue(result)
    }

    @Test
    @DisplayName("reply(): falls back to TEN_MINS when queryData doesn't match any SnoozeNotificationType")
    fun `reply falls back to TEN_MINS for unknown queryData`() {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        `when`(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(notificationDto)

        val callbackQuery = buildCallbackQuery("00000000-0000-0000-0000-000000000000")
        strategy.reply(callbackQuery)

        val interval = notificationDto.notificationInterval.minutes
        val expectedTime = fixedNow.minusMinutes(interval).plusMinutes(SnoozeNotificationType.TEN_MINS.minutes)
        verify(notificationAccessService).updateTimeOfLastNotification(userId, expectedTime)
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

    private fun buildCallbackQuery(queryData: String): CallbackQuery {
        val user = mock(User::class.java)
        `when`(user.id).thenReturn(userId)

        val message = mock(Message::class.java)
        `when`(message.chatId).thenReturn(chatId)
        `when`(message.messageId).thenReturn(messageId)

        val callbackQuery = mock(CallbackQuery::class.java)
        `when`(callbackQuery.from).thenReturn(user)
        `when`(callbackQuery.message).thenReturn(message)
        `when`(callbackQuery.data).thenReturn(queryData)
        return callbackQuery
    }

}
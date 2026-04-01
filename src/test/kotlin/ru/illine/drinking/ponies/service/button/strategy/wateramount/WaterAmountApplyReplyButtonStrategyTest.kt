package ru.illine.drinking.ponies.service.button.strategy.wateramount

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.WaterAmountType
import ru.illine.drinking.ponies.service.statistic.WaterStatisticService
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.telegram.TelegramMessageConstants
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset

@UnitTest
@DisplayName("WaterAmountApplyReplyButtonStrategy Unit Test")
class WaterAmountApplyReplyButtonStrategyTest {

    private val userId = 1L
    private val chatId = 2L
    private val messageId = 3
    private val fixedNow = LocalDateTime.of(2025, 1, 1, 14, 0, 0)
    private val fixedClock = Clock.fixed(fixedNow.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private lateinit var sender: TelegramClient
    private lateinit var notificationAccessService: NotificationAccessService
    private lateinit var waterStatisticService: WaterStatisticService
    private lateinit var messageEditorService: MessageEditorService
    private lateinit var strategy: WaterAmountApplyReplyButtonStrategy

    @BeforeEach
    fun setUp() {
        sender = mock(TelegramClient::class.java)
        notificationAccessService = mock(NotificationAccessService::class.java)
        waterStatisticService = mock(WaterStatisticService::class.java)
        messageEditorService = mock(MessageEditorService::class.java)
        strategy = WaterAmountApplyReplyButtonStrategy(
            sender,
            notificationAccessService,
            waterStatisticService,
            messageEditorService,
            fixedClock
        )
    }

    @ParameterizedTest
    @EnumSource(WaterAmountType::class)
    @DisplayName("reply(): deletes reply markup on original message")
    fun `reply deletes reply markup`(waterAmountType: WaterAmountType) {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        `when`(notificationAccessService.updateTimeOfLastNotification(userId, fixedNow)).thenReturn(notificationDto)

        strategy.reply(buildCallbackQuery(waterAmountType.queryData.toString()))

        verify(messageEditorService).deleteReplyMarkup(chatId, messageId)
    }

    @ParameterizedTest
    @EnumSource(WaterAmountType::class)
    @DisplayName("reply(): updates last notification time to now()")
    fun `reply updates notification time`(waterAmountType: WaterAmountType) {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        `when`(notificationAccessService.updateTimeOfLastNotification(userId, fixedNow)).thenReturn(notificationDto)

        strategy.reply(buildCallbackQuery(waterAmountType.queryData.toString()))

        verify(notificationAccessService).updateTimeOfLastNotification(userId, fixedNow)
    }

    @ParameterizedTest
    @EnumSource(WaterAmountType::class)
    @DisplayName("reply(): sends YES confirmation message")
    fun `reply sends confirmation message`(waterAmountType: WaterAmountType) {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        `when`(notificationAccessService.updateTimeOfLastNotification(userId, fixedNow)).thenReturn(notificationDto)

        val captor = ArgumentCaptor.forClass(SendMessage::class.java)
        strategy.reply(buildCallbackQuery(waterAmountType.queryData.toString()))

        verify(sender).execute(captor.capture())
        val sent = captor.value
        assertEquals(chatId.toString(), sent.chatId)
        assertEquals(TelegramMessageConstants.NOTIFICATION_ANSWER_YES_MESSAGE, sent.text)
    }

    @ParameterizedTest
    @EnumSource(WaterAmountType::class)
    @DisplayName("reply(): records water statistic with correct water amount")
    fun `reply records statistic with correct water amount`(waterAmountType: WaterAmountType) {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        `when`(notificationAccessService.updateTimeOfLastNotification(userId, fixedNow)).thenReturn(notificationDto)

        strategy.reply(buildCallbackQuery(waterAmountType.queryData.toString()))

        verify(waterStatisticService).recordEvent(
            notificationDto.telegramUser,
            AnswerNotificationType.YES,
            waterAmountType.amountMl
        )
    }

    @Test
    @DisplayName("reply(): falls back to ML_250 when queryData doesn't match any WaterAmountType")
    fun `reply falls back to ML_250 for unknown queryData`() {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        `when`(notificationAccessService.updateTimeOfLastNotification(userId, fixedNow)).thenReturn(notificationDto)

        strategy.reply(buildCallbackQuery("00000000-0000-0000-0000-000000000000"))

        verify(waterStatisticService).recordEvent(
            notificationDto.telegramUser,
            AnswerNotificationType.YES,
            WaterAmountType.ML_250.amountMl
        )
    }

    @Test
    @DisplayName("reply(): executes operations in correct order")
    fun `reply executes in correct order`() {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        `when`(notificationAccessService.updateTimeOfLastNotification(userId, fixedNow)).thenReturn(notificationDto)

        strategy.reply(buildCallbackQuery(WaterAmountType.ML_250.queryData.toString()))

        val inOrder = inOrder(messageEditorService, notificationAccessService, waterStatisticService, sender)
        inOrder.verify(messageEditorService).deleteReplyMarkup(chatId, messageId)
        inOrder.verify(notificationAccessService).updateTimeOfLastNotification(userId, fixedNow)
        inOrder.verify(waterStatisticService).recordEvent(
            notificationDto.telegramUser,
            AnswerNotificationType.YES,
            WaterAmountType.ML_250.amountMl
        )
        inOrder.verify(sender).execute(any<SendMessage>())
    }

    @ParameterizedTest
    @EnumSource(WaterAmountType::class)
    @DisplayName("isQueryData(): returns true for each WaterAmountType queryData")
    fun `isQueryData returns true for water amount types`(waterAmountType: WaterAmountType) {
        val result = strategy.isQueryData(waterAmountType.queryData.toString())
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
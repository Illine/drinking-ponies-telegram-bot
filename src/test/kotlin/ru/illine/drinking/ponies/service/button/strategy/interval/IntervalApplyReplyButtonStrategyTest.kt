package ru.illine.drinking.ponies.service.button.strategy.interval

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
import ru.illine.drinking.ponies.model.base.IntervalNotificationType
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.telegram.TelegramMessageConstants

@UnitTest
@DisplayName("IntervalApplyReplyButtonStrategy Unit Test")
class IntervalApplyReplyButtonStrategyTest {

    private val userId = 1L
    private val chatId = 2L
    private val messageId = 3

    private lateinit var sender: TelegramClient
    private lateinit var notificationAccessService: NotificationAccessService
    private lateinit var messageEditorService: MessageEditorService
    private lateinit var strategy: IntervalApplyReplyButtonStrategy

    @BeforeEach
    fun setUp() {
        sender = mock(TelegramClient::class.java)
        notificationAccessService = mock(NotificationAccessService::class.java)
        messageEditorService = mock(MessageEditorService::class.java)
        strategy = IntervalApplyReplyButtonStrategy(sender, notificationAccessService, messageEditorService)
    }

    @ParameterizedTest
    @EnumSource(IntervalNotificationType::class)
    @DisplayName("reply(): deletes reply markup on original message")
    fun `reply deletes reply markup`(intervalType: IntervalNotificationType) {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        `when`(
            notificationAccessService.updateNotificationSettings(userId, chatId, intervalType)
        ).thenReturn(notificationDto)

        val callbackQuery = buildCallbackQuery(intervalType.queryData.toString())
        strategy.reply(callbackQuery)

        verify(messageEditorService).deleteReplyMarkup(chatId, messageId)
    }

    @ParameterizedTest
    @EnumSource(IntervalNotificationType::class)
    @DisplayName("reply(): updates notification settings with selected interval")
    fun `reply updates notification settings`(intervalType: IntervalNotificationType) {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        `when`(
            notificationAccessService.updateNotificationSettings(userId, chatId, intervalType)
        ).thenReturn(notificationDto)

        val callbackQuery = buildCallbackQuery(intervalType.queryData.toString())
        strategy.reply(callbackQuery)

        verify(notificationAccessService).updateNotificationSettings(userId, chatId, intervalType)
    }

    @ParameterizedTest
    @EnumSource(IntervalNotificationType::class)
    @DisplayName("reply(): sends confirmation message with interval display name")
    fun `reply sends confirmation message`(intervalType: IntervalNotificationType) {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        `when`(
            notificationAccessService.updateNotificationSettings(userId, chatId, intervalType)
        ).thenReturn(notificationDto)

        val callbackQuery = buildCallbackQuery(intervalType.queryData.toString())
        val captor = ArgumentCaptor.forClass(SendMessage::class.java)

        strategy.reply(callbackQuery)

        verify(sender).execute(captor.capture())
        val sent = captor.value
        assertEquals(chatId.toString(), sent.chatId)
        assertEquals(
            TelegramMessageConstants.TIME_BUTTON_RESULT_MESSAGE.format(intervalType.displayName),
            sent.text
        )
    }

    @Test
    @DisplayName("reply(): falls back to TWO_HOURS for unknown queryData")
    fun `reply falls back to TWO_HOURS for unknown queryData`() {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        `when`(
            notificationAccessService.updateNotificationSettings(userId, chatId, IntervalNotificationType.TWO_HOURS)
        ).thenReturn(notificationDto)

        val callbackQuery = buildCallbackQuery("00000000-0000-0000-0000-000000000000")
        strategy.reply(callbackQuery)

        verify(notificationAccessService).updateNotificationSettings(userId, chatId, IntervalNotificationType.TWO_HOURS)
    }

    @ParameterizedTest
    @EnumSource(IntervalNotificationType::class)
    @DisplayName("isQueryData(): returns true for each IntervalNotificationType queryData")
    fun `isQueryData returns true for interval types`(intervalType: IntervalNotificationType) {
        val result = strategy.isQueryData(intervalType.queryData.toString())
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

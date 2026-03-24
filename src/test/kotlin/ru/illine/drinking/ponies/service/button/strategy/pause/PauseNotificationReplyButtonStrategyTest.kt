package ru.illine.drinking.ponies.service.button.strategy.pause

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
import ru.illine.drinking.ponies.model.base.PauseNotificationType
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.telegram.TelegramMessageConstants
import java.time.LocalDateTime

@UnitTest
@DisplayName("PauseNotificationReplyButtonStrategy Unit Test")
class PauseNotificationReplyButtonStrategyTest {

    private val userId = 1L
    private val chatId = 2L
    private val messageId = 3

    private lateinit var sender: TelegramClient
    private lateinit var notificationAccessService: NotificationAccessService
    private lateinit var messageEditorService: MessageEditorService
    private lateinit var strategy: PauseNotificationReplyButtonStrategy

    @BeforeEach
    fun setUp() {
        sender = mock(TelegramClient::class.java)
        notificationAccessService = mock(NotificationAccessService::class.java)
        messageEditorService = mock(MessageEditorService::class.java)
        strategy = PauseNotificationReplyButtonStrategy(sender, notificationAccessService, messageEditorService)
    }

    @ParameterizedTest
    @EnumSource(PauseNotificationType::class, names = ["RESET"], mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("reply(): deletes reply markup on original message")
    fun `reply deletes reply markup`(pauseType: PauseNotificationType) {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        `when`(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(notificationDto)

        strategy.reply(buildCallbackQuery(pauseType.queryData.toString()))

        verify(messageEditorService).deleteReplyMarkup(chatId, messageId)
    }

    @ParameterizedTest
    @EnumSource(PauseNotificationType::class, names = ["RESET"], mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("reply(): updates notification time to now + pauseMinutes")
    fun `reply updates notification time`(pauseType: PauseNotificationType) {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        `when`(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(notificationDto)

        strategy.reply(buildCallbackQuery(pauseType.queryData.toString()))

        verify(notificationAccessService).updateTimeOfLastNotification(eq(userId), any<LocalDateTime>())
    }

    @ParameterizedTest
    @EnumSource(PauseNotificationType::class, names = ["RESET"], mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("reply(): sends confirmation message with pause displayName")
    fun `reply sends confirmation message`(pauseType: PauseNotificationType) {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        `when`(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(notificationDto)

        val captor = ArgumentCaptor.forClass(SendMessage::class.java)
        strategy.reply(buildCallbackQuery(pauseType.queryData.toString()))

        verify(sender).execute(captor.capture())
        val sent = captor.value
        assertEquals(chatId.toString(), sent.chatId)
        assertEquals(
            TelegramMessageConstants.PAUSE_BUTTON_RESULT_MESSAGE.format(pauseType.displayName),
            sent.text
        )
    }

    @Test
    @DisplayName("reply(): deletes reply markup on original message")
    fun `reply deletes reply markup`() {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        `when`(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(notificationDto)

        strategy.reply(buildCallbackQuery(PauseNotificationType.RESET.queryData.toString()))

        verify(messageEditorService).deleteReplyMarkup(chatId, messageId)
    }

    @Test
    @DisplayName("reply(): resets notification time to now + interval minutes")
    fun `reply updates notification time`() {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        `when`(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(notificationDto)

        strategy.reply(buildCallbackQuery(PauseNotificationType.RESET.queryData.toString()))

        verify(notificationAccessService).updateTimeOfLastNotification(eq(userId), any<LocalDateTime>())
    }

    @Test
    @DisplayName("reply(): sends reset message with interval displayName")
    fun `reply sends reset message`() {
        val notificationDto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        `when`(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(notificationDto)

        val captor = ArgumentCaptor.forClass(SendMessage::class.java)
        strategy.reply(buildCallbackQuery(PauseNotificationType.RESET.queryData.toString()))

        verify(sender).execute(captor.capture())
        val sent = captor.value
        assertEquals(chatId.toString(), sent.chatId)
        assertTrue(sent.text.contains(notificationDto.notificationInterval.displayName))
    }

    @Test
    @DisplayName("reply(): throws IllegalArgumentException when queryData is not correct")
    fun `reply throws IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> {
            strategy.reply(buildCallbackQuery("00000000-0000-0000-0000-000000000000"))
        }

        verify(sender, never()).execute(any<SendMessage>())
    }

    @ParameterizedTest
    @EnumSource(PauseNotificationType::class)
    @DisplayName("isQueryData(): returns true for each PauseNotificationType queryData")
    fun `isQueryData returns true for pause types`(pauseType: PauseNotificationType) {
        val result = strategy.isQueryData(pauseType.queryData.toString())
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

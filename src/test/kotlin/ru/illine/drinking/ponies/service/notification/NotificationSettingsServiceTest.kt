package ru.illine.drinking.ponies.service.notification

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.service.notification.impl.NotificationSettingsServiceImpl
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.telegram.TelegramMessageConstants
import java.time.LocalTime

@UnitTest
@DisplayName("NotificationSettingsService Unit Test")
class NotificationSettingsServiceTest {

    private val userId = 1L
    private val chatId = 2L
    private val messageId = 3

    private lateinit var notificationAccessService: NotificationAccessService
    private lateinit var messageEditorService: MessageEditorService
    private lateinit var sender: TelegramClient
    private lateinit var service: NotificationSettingsServiceImpl

    @BeforeEach
    fun setUp() {
        notificationAccessService = Mockito.mock(NotificationAccessService::class.java)
        messageEditorService = Mockito.mock(MessageEditorService::class.java)
        sender = Mockito.mock(TelegramClient::class.java)
        service = NotificationSettingsServiceImpl(notificationAccessService, messageEditorService, sender)
    }

    @Test
    @DisplayName("changeQuietMode(): updates quiet mode via access service")
    fun `changeQuietMode updates quiet mode`() {
        val start = LocalTime.of(22, 0)
        val end = LocalTime.of(8, 0)
        val notificationDto = DtoGenerator.Companion.generateNotificationDto(externalUserId = userId, externalChatId = chatId)
        Mockito.`when`(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(notificationDto)

        service.changeQuietMode(userId, messageId, start, end)

        Mockito.verify(notificationAccessService).changeQuietMode(userId, start, end)
    }

    @Test
    @DisplayName("changeQuietMode(): deletes reply markup on original message")
    fun `changeQuietMode deletes reply markup`() {
        val start = LocalTime.of(22, 0)
        val end = LocalTime.of(8, 0)
        val notificationDto = DtoGenerator.Companion.generateNotificationDto(externalUserId = userId, externalChatId = chatId)
        Mockito.`when`(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(notificationDto)

        service.changeQuietMode(userId, messageId, start, end)

        Mockito.verify(messageEditorService).deleteReplyMarkup(chatId, messageId)
    }

    @Test
    @DisplayName("changeQuietMode(): sends confirmation message with quiet mode times")
    fun `changeQuietMode sends confirmation message`() {
        val start = LocalTime.of(22, 0)
        val end = LocalTime.of(8, 0)
        val notificationDto = DtoGenerator.Companion.generateNotificationDto(externalUserId = userId, externalChatId = chatId)
        Mockito.`when`(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(notificationDto)

        val captor = ArgumentCaptor.forClass(SendMessage::class.java)
        service.changeQuietMode(userId, messageId, start, end)

        Mockito.verify(sender).execute(captor.capture())
        val sent = captor.value
        Assertions.assertEquals(chatId.toString(), sent.chatId)
        Assertions.assertEquals(
            TelegramMessageConstants.SETTINGS_QUIET_MODE_TIME_NOTIFICATION_CHANGING.format(start, end),
            sent.text
        )
    }

    @Test
    @DisplayName("changeQuietMode(): throws IllegalArgumentException when start equals end")
    fun `changeQuietMode throws when start equals end`() {
        val time = LocalTime.of(10, 0)

        Assertions.assertThrows(IllegalArgumentException::class.java) {
            service.changeQuietMode(userId, messageId, time, time)
        }
    }

    @Test
    @DisplayName("disableQuietMode(): disables quiet mode via access service")
    fun `disableQuietMode disables quiet mode`() {
        service.disableQuietMode(userId)

        Mockito.verify(notificationAccessService).disableQuietMode(userId)
    }
}
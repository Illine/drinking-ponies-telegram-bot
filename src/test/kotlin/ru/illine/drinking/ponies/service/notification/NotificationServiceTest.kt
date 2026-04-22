package ru.illine.drinking.ponies.service.notification

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.telegram.telegrambots.abilitybots.api.objects.MessageContext
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.service.notification.impl.NotificationServiceImpl
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.telegram.TelegramMessageConstants

@UnitTest
@DisplayName("NotificationService Unit Test")
class NotificationServiceTest {

    private val userId = 1L
    private val chatId = 2L

    private lateinit var sender: TelegramClient
    private lateinit var notificationAccessService: NotificationAccessService
    private lateinit var service: NotificationService

    @BeforeEach
    fun setUp() {
        sender = mock(TelegramClient::class.java)
        notificationAccessService = mock(NotificationAccessService::class.java)
        service = NotificationServiceImpl(
            sender,
            notificationAccessService
        )
    }

    @Test
    @DisplayName("start(): existing user - sends greeting and default settings, finds existing settings")
    fun `start existing user`() {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        doReturn(true).`when`(notificationAccessService).existsByTelegramUserId(userId)
        `when`(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(dto)

        service.start(buildMessageContext())

        verify(sender, times(2)).execute(any<SendMessage>())
        verify(notificationAccessService).findNotificationSettingByTelegramUserId(userId)
        verify(notificationAccessService, never()).save(any(), any(), any())
    }

    @Test
    @DisplayName("start(): new user - creates user, saves settings, sends default settings message")
    fun `start new user`() {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        doReturn(false).`when`(notificationAccessService).existsByTelegramUserId(userId)
        `when`(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(dto)

        service.start(buildMessageContext())

        verify(notificationAccessService).save(any(), any(), any())
        verify(notificationAccessService).findNotificationSettingByTelegramUserId(userId)
        verify(sender, times(2)).execute(any<SendMessage>())
    }

    @Test
    @DisplayName("stop(): disables notifications and sends stop message")
    fun `stop disables notifications and sends message`() {
        service.stop(buildMessageContext())

        verify(notificationAccessService).disableNotifications(userId)
        val captor = ArgumentCaptor.forClass(SendMessage::class.java)
        verify(sender).execute(captor.capture())
        assertEquals(chatId.toString(), captor.value.chatId)
        assertEquals(TelegramMessageConstants.STOP_GREETING_MESSAGE.format("testUser"), captor.value.text)
    }

    @Test
    @DisplayName("resume(): enables notifications and sends resume message")
    fun `resume enables notifications and sends message`() {
        service.resume(buildMessageContext())

        verify(notificationAccessService).enableNotifications(userId)
        val captor = ArgumentCaptor.forClass(SendMessage::class.java)
        verify(sender).execute(captor.capture())
        assertEquals(chatId.toString(), captor.value.chatId)
    }

    @Test
    @DisplayName("pause(): when enabled - sends pause menu with keyboard")
    fun `pause when enabled sends pause menu`() {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        doReturn(true).`when`(notificationAccessService).isEnabledNotifications(userId)
        `when`(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(dto)

        service.pause(buildMessageContext())

        val captor = ArgumentCaptor.forClass(SendMessage::class.java)
        verify(sender).execute(captor.capture())
        assertEquals(chatId.toString(), captor.value.chatId)
        assertEquals(TelegramMessageConstants.PAUSE_GREETING_MESSAGE, captor.value.text)
        assertNotNull(captor.value.replyMarkup)
    }

    @Test
    @DisplayName("pause(): when disabled - sends not-active message")
    fun `pause when disabled sends not active message`() {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = userId)
        doReturn(false).`when`(notificationAccessService).isEnabledNotifications(userId)
        `when`(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(dto)

        service.pause(buildMessageContext())

        val captor = ArgumentCaptor.forClass(SendMessage::class.java)
        verify(sender).execute(captor.capture())
        assertEquals(chatId.toString(), captor.value.chatId)
        assertTrue(captor.value.text.contains(TelegramMessageConstants.NOTIFICATION_NOT_ACTIVE_MESSAGE))
    }

    private fun buildMessageContext(): MessageContext {
        val user = mock(User::class.java)
        `when`(user.id).thenReturn(userId)
        `when`(user.userName).thenReturn("testUser")

        val context = mock(MessageContext::class.java)
        `when`(context.user()).thenReturn(user)
        `when`(context.chatId()).thenReturn(chatId)
        return context
    }
}

package ru.illine.drinking.ponies.service.notification

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.telegram.telegrambots.abilitybots.api.objects.MessageContext
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.SettingsType
import ru.illine.drinking.ponies.service.button.ButtonDataService
import ru.illine.drinking.ponies.service.notification.impl.NotificationServiceImpl
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.telegram.TelegramMessageConstants
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@UnitTest
@DisplayName("NotificationService Unit Test")
class NotificationServiceTest {

    private val userId = 1L
    private val chatId = 2L

    private lateinit var sender: TelegramClient
    private lateinit var messageEditorService: MessageEditorService
    private lateinit var notificationAccessService: NotificationAccessService
    private lateinit var settingsButtonDataService: ButtonDataService<SettingsType>
    private lateinit var clock: Clock
    private lateinit var service: NotificationServiceImpl

    @BeforeEach
    fun setUp() {
        sender = mock(TelegramClient::class.java)
        messageEditorService = mock(MessageEditorService::class.java)
        notificationAccessService = mock(NotificationAccessService::class.java)
        @Suppress("UNCHECKED_CAST")
        settingsButtonDataService = mock(ButtonDataService::class.java) as ButtonDataService<SettingsType>
        clock = Clock.fixed(Instant.now(), ZoneOffset.UTC)
        service = NotificationServiceImpl(sender, messageEditorService, notificationAccessService, settingsButtonDataService, clock)
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

    @Test
    @DisplayName("settings(): when enabled - sends settings message and calls editReplyMarkup")
    fun `settings when enabled sends menu and edits markup`() {
        doReturn(true).`when`(notificationAccessService).isEnabledNotifications(userId)
        `when`(settingsButtonDataService.getData(any())).thenReturn("http://example.com")

        val returnedMessage = mock(Message::class.java)
        `when`(returnedMessage.messageId).thenReturn(2)
        `when`(returnedMessage.chatId).thenReturn(chatId)
        `when`(returnedMessage.text).thenReturn(TelegramMessageConstants.SETTINGS_GREETING_MESSAGE)
        doReturn(returnedMessage).`when`(sender).execute(any<SendMessage>())

        service.settings(buildMessageContext())

        verify(sender).execute(any<SendMessage>())
        verify(messageEditorService).editReplyMarkup(any(), any(), any(), any(), anyOrNull())
    }

    @Test
    @DisplayName("settings(): when disabled - sends not-active message")
    fun `settings when disabled sends not active message`() {
        doReturn(false).`when`(notificationAccessService).isEnabledNotifications(userId)

        service.settings(buildMessageContext())

        val captor = ArgumentCaptor.forClass(SendMessage::class.java)
        verify(sender).execute(captor.capture())
        assertTrue(captor.value.text.contains(TelegramMessageConstants.NOTIFICATION_NOT_ACTIVE_MESSAGE))
    }

    @Test
    @DisplayName("sendNotifications(): empty collection - no interactions with sender or access service")
    fun `sendNotifications with empty list does nothing`() {
        service.sendNotifications(emptyList())

        verifyNoInteractions(sender)
        verifyNoInteractions(notificationAccessService)
        verifyNoInteractions(messageEditorService)
    }

    @Test
    @DisplayName("sendNotifications(): sends notification messages, increments attempts, updates settings")
    fun `sendNotifications sends and updates`() {
        val dto = DtoGenerator.generateNotificationDto(
            externalUserId = userId,
            externalChatId = chatId,
            notificationAttempts = 0,
            previousNotificationMessageId = 10
        )
        val returnedMessage = mock(Message::class.java)
        `when`(returnedMessage.messageId).thenReturn(2)
        doReturn(returnedMessage).`when`(sender).execute(any<SendMessage>())

        service.sendNotifications(listOf(dto))

        verify(messageEditorService).deleteMessages(anyCollection())
        verify(sender).execute(any<SendMessage>())
        verify(notificationAccessService).updateNotificationSettings(anyCollection())
        assertEquals(1, dto.notificationAttempts)
        assertEquals(2, dto.telegramChat.previousNotificationMessageId)
    }

    @Test
    @DisplayName("sendNotifications(): 403 error - disables user and excludes from settings update")
    fun `sendNotifications on 403 disables notifications`() {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = userId, externalChatId = chatId)
        val exception = mock(TelegramApiRequestException::class.java)
        `when`(exception.errorCode).thenReturn(403)
        doThrow(exception).`when`(sender).execute(any<SendMessage>())

        service.sendNotifications(listOf(dto))

        verify(notificationAccessService).disableNotifications(userId)
        verify(notificationAccessService).updateNotificationSettings(anyCollection())
    }

    @Test
    @DisplayName("suspendNotifications(): empty collection - no interactions with sender or access service")
    fun `suspendNotifications with empty list does nothing`() {
        service.suspendNotifications(emptyList())

        verifyNoInteractions(sender)
        verifyNoInteractions(notificationAccessService)
        verifyNoInteractions(messageEditorService)
    }

    @Test
    @DisplayName("suspendNotifications(): sends suspend message, resets attempts and time, updates settings")
    fun `suspendNotifications sends and updates`() {
        val dto = DtoGenerator.generateNotificationDto(
            externalUserId = userId,
            externalChatId = chatId,
            notificationAttempts = 3,
            previousNotificationMessageId = 10
        )

        service.suspendNotifications(listOf(dto))

        verify(messageEditorService).deleteMessages(anyCollection())
        verify(sender).execute(any<SendMessage>())
        verify(notificationAccessService).updateNotificationSettings(anyCollection())
        assertEquals(0, dto.notificationAttempts)
        assertNull(dto.telegramChat.previousNotificationMessageId)
    }

    @Test
    @DisplayName("suspendNotifications(): 403 error - disables user and excludes from settings update")
    fun `suspendNotifications on 403 disables notifications`() {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = userId, externalChatId = chatId)
        val exception = mock(TelegramApiRequestException::class.java)
        `when`(exception.errorCode).thenReturn(403)
        doThrow(exception).`when`(sender).execute(any<SendMessage>())

        service.suspendNotifications(listOf(dto))

        verify(notificationAccessService).disableNotifications(userId)
        verify(notificationAccessService).updateNotificationSettings(anyCollection())
    }

    @Test
    @DisplayName("sendNotifications(): non-403 error - rethrows exception")
    fun `sendNotifications rethrows non-403 exception`() {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = userId, externalChatId = chatId)
        val exception = mock(TelegramApiRequestException::class.java)
        `when`(exception.errorCode).thenReturn(500)
        doThrow(exception).`when`(sender).execute(any<SendMessage>())

        assertThrows(TelegramApiRequestException::class.java) {
            service.sendNotifications(listOf(dto))
        }
    }

    @Test
    @DisplayName("sendNotifications(): null errorCode - rethrows exception")
    fun `sendNotifications rethrows null errorCode exception`() {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = userId, externalChatId = chatId)
        val exception = mock(TelegramApiRequestException::class.java)
        doReturn(null).`when`(exception).errorCode
        doThrow(exception).`when`(sender).execute(any<SendMessage>())

        assertThrows(TelegramApiRequestException::class.java) {
            service.sendNotifications(listOf(dto))
        }
    }

    @Test
    @DisplayName("suspendNotifications(): non-403 error - rethrows exception")
    fun `suspendNotifications rethrows non-403 exception`() {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = userId, externalChatId = chatId)
        val exception = mock(TelegramApiRequestException::class.java)
        `when`(exception.errorCode).thenReturn(500)
        doThrow(exception).`when`(sender).execute(any<SendMessage>())

        assertThrows(TelegramApiRequestException::class.java) {
            service.suspendNotifications(listOf(dto))
        }
        verify(notificationAccessService, never()).disableNotifications(any())
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

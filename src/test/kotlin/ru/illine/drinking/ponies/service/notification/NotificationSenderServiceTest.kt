package ru.illine.drinking.ponies.service.notification

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.config.property.TelegramBotProperties
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.IntervalNotificationType
import ru.illine.drinking.ponies.service.notification.impl.NotificationSenderServiceImpl
import ru.illine.drinking.ponies.service.statistic.WaterStatisticService
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@UnitTest
@DisplayName("NotificationSenderService Unit Test")
class NotificationSenderServiceTest {

    private val userId = 1L
    private val chatId = 2L

    private val retryIntervalMinutes = 1L
    private val botProperties = TelegramBotProperties(
        version = "1.0.0",
        token = "token",
        username = "username",
        creatorId = 1L,
        autoUpdateCommands = true,
        http = TelegramBotProperties.Http(connectionTimeToLiveInSec = 30, maxConnectionTotal = 10),
        notification = TelegramBotProperties.Notification(retryIntervalMinutes = retryIntervalMinutes)
    )

    private lateinit var sender: TelegramClient
    private lateinit var messageEditorService: MessageEditorService
    private lateinit var notificationAccessService: NotificationAccessService
    private lateinit var waterStatisticService: WaterStatisticService
    private lateinit var clock: Clock
    private lateinit var service: NotificationSenderService

    @BeforeEach
    fun setUp() {
        sender = mock(TelegramClient::class.java)
        messageEditorService = mock(MessageEditorService::class.java)
        notificationAccessService = mock(NotificationAccessService::class.java)
        @Suppress("UNCHECKED_CAST")
        waterStatisticService = mock(WaterStatisticService::class.java)
        clock = Clock.fixed(Instant.now(), ZoneOffset.UTC)
        service = NotificationSenderServiceImpl(
            sender,
            messageEditorService,
            notificationAccessService,
            botProperties,
            waterStatisticService,
            clock
        )
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

        val expectedTimeOfLastNotification = LocalDateTime.now(clock)
            .minusMinutes(IntervalNotificationType.HOUR.minutes)
            .plusMinutes(retryIntervalMinutes)

        verify(messageEditorService).deleteMessages(anyCollection())
        verify(sender).execute(any<SendMessage>())
        verify(notificationAccessService).updateNotificationSettings(anyCollection())
        assertEquals(1, dto.notificationAttempts)
        assertEquals(2, dto.telegramChat.previousNotificationMessageId)
        assertEquals(expectedTimeOfLastNotification, dto.timeOfLastNotification)
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
    @DisplayName("suspendNotifications(): records water statistic events for each sent notification")
    fun `suspendNotifications records water statistics`() {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = userId, externalChatId = chatId)

        service.suspendNotifications(listOf(dto))

        verify(waterStatisticService).recordEvents(listOf(dto.telegramUser), AnswerNotificationType.CANCEL)
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
}

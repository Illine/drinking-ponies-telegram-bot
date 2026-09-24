package ru.illine.drinking.ponies.service.notification

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.config.property.TelegramBotProperties
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.exception.NotificationSettingsNotFoundException
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.IntervalNotificationType
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.service.message.impl.LocalMessageProvider
import ru.illine.drinking.ponies.service.notification.impl.NotificationSenderServiceImpl
import ru.illine.drinking.ponies.service.statistic.WaterStatisticService
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.random.Random

@UnitTest
@DisplayName("NotificationSenderService Unit Test")
class NotificationSenderServiceTest {
    private val externalUserId = 1L
    private val chatId = 2L

    private val retryIntervalMinutes = 1L
    private val botProperties =
        TelegramBotProperties(
            token = "token",
            username = "username",
            miniAppUrl = "https://t.me/Test/app",
            autoUpdateTelegramConfig = true,
            http = TelegramBotProperties.Http(connectionTimeToLiveInSec = 30, maxConnectionTotal = 10),
            notification = TelegramBotProperties.Notification(retryIntervalMinutes = retryIntervalMinutes),
        )

    private lateinit var sender: TelegramClient
    private lateinit var messageEditorService: MessageEditorService
    private lateinit var notificationAccessService: NotificationAccessService
    private lateinit var waterStatisticService: WaterStatisticService
    private lateinit var clock: Clock
    private lateinit var service: NotificationSenderService

    @BeforeEach
    fun setUp() {
        sender = mock<TelegramClient>()
        messageEditorService = mock<MessageEditorService>()
        notificationAccessService = mock<NotificationAccessService>()
        waterStatisticService = mock<WaterStatisticService>()
        clock = Clock.fixed(Instant.now(), ZoneOffset.UTC)
        service =
            NotificationSenderServiceImpl(
                sender,
                messageEditorService,
                notificationAccessService,
                botProperties,
                waterStatisticService,
                clock,
                LocalMessageProvider(Random(42)),
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
        val dto =
            DtoGenerator.generateNotificationDto(
                externalUserId = externalUserId,
                externalChatId = chatId,
                notificationAttempts = 0,
                previousNotificationMessageId = 10,
            )
        val returnedMessage = mock<Message>()
        whenever(returnedMessage.messageId).thenReturn(2)
        doReturn(returnedMessage).whenever(sender).execute(any<SendMessage>())

        service.sendNotifications(listOf(dto))

        val expectedTimeOfLastNotification =
            LocalDateTime
                .now(clock)
                .minusMinutes(IntervalNotificationType.HOUR.minutes)
                .plusMinutes(retryIntervalMinutes)

        verify(messageEditorService).deleteMessages(any())
        verify(sender).execute(any<SendMessage>())
        verify(notificationAccessService).recordMailingResults(any())
        assertEquals(1, dto.notificationAttempts)
        assertEquals(2, dto.telegramChat.previousNotificationMessageId)
        assertEquals(expectedTimeOfLastNotification, dto.timeOfLastNotification)
    }

    @Test
    @DisplayName("sendNotifications(): 403 error - disables user and excludes from settings update")
    fun `sendNotifications on 403 disables notifications`() {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId, externalChatId = chatId)
        val exception = mock<TelegramApiRequestException>()
        whenever(exception.errorCode).thenReturn(403)
        doThrow(exception).whenever(sender).execute(any<SendMessage>())

        service.sendNotifications(listOf(dto))

        val captor = argumentCaptor<Collection<NotificationSettingDto>>()
        verify(notificationAccessService).updateNotificationsDisabled(externalUserId)
        verify(notificationAccessService).recordMailingResults(captor.capture())
        assertEquals(emptyList<NotificationSettingDto>(), captor.firstValue.toList())
    }

    @Test
    @DisplayName("sendNotifications(): 400 error - skips user without disabling notifications")
    fun `sendNotifications on 400 chat not found`() {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId, externalChatId = chatId)
        val exception = mock<TelegramApiRequestException>()
        whenever(exception.errorCode).thenReturn(400)
        doThrow(exception).whenever(sender).execute(any<SendMessage>())

        service.sendNotifications(listOf(dto))

        val captor = argumentCaptor<Collection<NotificationSettingDto>>()
        verify(notificationAccessService, never()).updateNotificationsDisabled(any())
        verify(notificationAccessService).recordMailingResults(captor.capture())
        assertEquals(emptyList<NotificationSettingDto>(), captor.firstValue.toList())
    }

    @Test
    @DisplayName("sendNotifications(): 400 error for one notification - the rest of the batch is still sent")
    fun `sendNotifications on 400 keeps sending the rest of the batch`() {
        val failingChatId = chatId + 1
        val first = DtoGenerator.generateNotificationDto(externalUserId = externalUserId, externalChatId = chatId)
        val failing =
            DtoGenerator.generateNotificationDto(
                externalUserId = externalUserId + 1,
                externalChatId = failingChatId,
            )
        val last =
            DtoGenerator.generateNotificationDto(
                externalUserId = externalUserId + 2,
                externalChatId = chatId + 2,
            )
        val exception = mock<TelegramApiRequestException>()
        whenever(exception.errorCode).thenReturn(400)
        val returnedMessage = mock<Message>()
        whenever(returnedMessage.messageId).thenReturn(2)
        doAnswer { invocation ->
            if (invocation.getArgument<SendMessage>(0).chatId == failingChatId.toString()) {
                throw exception
            }
            returnedMessage
        }.whenever(sender).execute(any<SendMessage>())

        service.sendNotifications(listOf(first, failing, last))

        val captor = argumentCaptor<Collection<NotificationSettingDto>>()
        verify(sender, times(3)).execute(any<SendMessage>())
        verify(notificationAccessService, never()).updateNotificationsDisabled(any())
        verify(notificationAccessService).recordMailingResults(captor.capture())
        assertEquals(listOf(first, last), captor.firstValue.toList())
    }

    @Test
    @DisplayName("sendNotifications(): 400 error - updates time of last notification to now")
    fun `sendNotifications on 400 updates time of last notification`() {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId, externalChatId = chatId)
        val exception = mock<TelegramApiRequestException>()
        whenever(exception.errorCode).thenReturn(400)
        doThrow(exception).whenever(sender).execute(any<SendMessage>())

        service.sendNotifications(listOf(dto))

        verify(notificationAccessService).updateTimeOfLastNotification(externalUserId, LocalDateTime.now(clock))
    }

    @Test
    @DisplayName("sendNotifications(): 400 error in a batch - updates time of last notification only for failing user")
    fun `sendNotifications on 400 updates time of last notification only for failing user`() {
        val failingUserId = externalUserId + 1
        val failingChatId = chatId + 1
        val first = DtoGenerator.generateNotificationDto(externalUserId = externalUserId, externalChatId = chatId)
        val failing =
            DtoGenerator.generateNotificationDto(
                externalUserId = failingUserId,
                externalChatId = failingChatId,
            )
        val last =
            DtoGenerator.generateNotificationDto(
                externalUserId = externalUserId + 2,
                externalChatId = chatId + 2,
            )
        val exception = mock<TelegramApiRequestException>()
        whenever(exception.errorCode).thenReturn(400)
        val returnedMessage = mock<Message>()
        whenever(returnedMessage.messageId).thenReturn(2)
        doAnswer { invocation ->
            if (invocation.getArgument<SendMessage>(0).chatId == failingChatId.toString()) {
                throw exception
            }
            returnedMessage
        }.whenever(sender).execute(any<SendMessage>())

        service.sendNotifications(listOf(first, failing, last))

        verify(notificationAccessService).updateTimeOfLastNotification(failingUserId, LocalDateTime.now(clock))
        verify(notificationAccessService, times(1)).updateTimeOfLastNotification(any(), any())
    }

    @Test
    @DisplayName("sendNotifications(): 403 error - disables notifications without updating time of last notification")
    fun `sendNotifications on 403 does not update time of last notification`() {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId, externalChatId = chatId)
        val exception = mock<TelegramApiRequestException>()
        whenever(exception.errorCode).thenReturn(403)
        doThrow(exception).whenever(sender).execute(any<SendMessage>())

        service.sendNotifications(listOf(dto))

        verify(notificationAccessService).updateNotificationsDisabled(externalUserId)
        verify(notificationAccessService, never()).updateTimeOfLastNotification(any(), any())
    }

    @ParameterizedTest(name = "[{index}] errorCode={0}")
    @ValueSource(ints = [400, 403])
    @DisplayName("sendNotifications(): DB write for a rejected user fails - the rest of the batch is still recorded")
    fun `sendNotifications survives a failing db write for a rejected user`(errorCode: Int) {
        val failingChatId = chatId + 1
        val first = DtoGenerator.generateNotificationDto(externalUserId = externalUserId, externalChatId = chatId)
        val failing =
            DtoGenerator.generateNotificationDto(
                externalUserId = externalUserId + 1,
                externalChatId = failingChatId,
            )
        val last =
            DtoGenerator.generateNotificationDto(
                externalUserId = externalUserId + 2,
                externalChatId = chatId + 2,
            )
        val exception = mock<TelegramApiRequestException>()
        whenever(exception.errorCode).thenReturn(errorCode)
        val returnedMessage = mock<Message>()
        whenever(returnedMessage.messageId).thenReturn(2)
        doAnswer { invocation ->
            if (invocation.getArgument<SendMessage>(0).chatId == failingChatId.toString()) {
                throw exception
            }
            returnedMessage
        }.whenever(sender).execute(any<SendMessage>())
        val dbFailure = NotificationSettingsNotFoundException("not found")
        doThrow(dbFailure).whenever(notificationAccessService).updateTimeOfLastNotification(any(), any())
        doThrow(dbFailure).whenever(notificationAccessService).updateNotificationsDisabled(any())

        service.sendNotifications(listOf(first, failing, last))

        val captor = argumentCaptor<Collection<NotificationSettingDto>>()
        verify(sender, times(3)).execute(any<SendMessage>())
        verify(notificationAccessService).recordMailingResults(captor.capture())
        assertEquals(listOf(first, last), captor.firstValue.toList())
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
        val dto =
            DtoGenerator.generateNotificationDto(
                externalUserId = externalUserId,
                externalChatId = chatId,
                notificationAttempts = 3,
                previousNotificationMessageId = 10,
            )

        service.suspendNotifications(listOf(dto))

        verify(messageEditorService).deleteMessages(any())
        verify(sender).execute(any<SendMessage>())
        verify(notificationAccessService).recordMailingResults(any())
        assertEquals(0, dto.notificationAttempts)
        assertNull(dto.telegramChat.previousNotificationMessageId)
    }

    @Test
    @DisplayName("suspendNotifications(): records water statistic events for each sent notification")
    fun `suspendNotifications records water statistics`() {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId, externalChatId = chatId)

        service.suspendNotifications(listOf(dto))

        verify(waterStatisticService).recordEvents(listOf(dto.telegramUser), AnswerNotificationType.CANCEL)
    }

    @Test
    @DisplayName("suspendNotifications(): 403 error - disables user and excludes from settings update")
    fun `suspendNotifications on 403 disables notifications`() {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId, externalChatId = chatId)
        val exception = mock<TelegramApiRequestException>()
        whenever(exception.errorCode).thenReturn(403)
        doThrow(exception).whenever(sender).execute(any<SendMessage>())

        service.suspendNotifications(listOf(dto))

        val captor = argumentCaptor<Collection<NotificationSettingDto>>()
        verify(notificationAccessService).updateNotificationsDisabled(externalUserId)
        verify(notificationAccessService).recordMailingResults(captor.capture())
        assertEquals(emptyList<NotificationSettingDto>(), captor.firstValue.toList())
        verify(waterStatisticService, never()).recordEvents(argThat { contains(dto.telegramUser) }, any())
    }

    @Test
    @DisplayName("suspendNotifications(): 400 error - skips user without disabling notifications")
    fun `suspendNotifications on 400 chat not found`() {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId, externalChatId = chatId)
        val exception = mock<TelegramApiRequestException>()
        whenever(exception.errorCode).thenReturn(400)
        doThrow(exception).whenever(sender).execute(any<SendMessage>())

        service.suspendNotifications(listOf(dto))

        val captor = argumentCaptor<Collection<NotificationSettingDto>>()
        verify(notificationAccessService, never()).updateNotificationsDisabled(any())
        verify(notificationAccessService).recordMailingResults(captor.capture())
        assertEquals(emptyList<NotificationSettingDto>(), captor.firstValue.toList())
    }

    @Test
    @DisplayName("suspendNotifications(): 400 error - updates time of last notification to now")
    fun `suspendNotifications on 400 updates time of last notification`() {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId, externalChatId = chatId)
        val exception = mock<TelegramApiRequestException>()
        whenever(exception.errorCode).thenReturn(400)
        doThrow(exception).whenever(sender).execute(any<SendMessage>())

        service.suspendNotifications(listOf(dto))

        verify(notificationAccessService).updateTimeOfLastNotification(externalUserId, LocalDateTime.now(clock))
    }

    @Test
    @DisplayName("suspendNotifications(): 400 error - records no CANCEL water statistic for the failing user")
    fun `suspendNotifications on 400 records no cancel statistic`() {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId, externalChatId = chatId)
        val exception = mock<TelegramApiRequestException>()
        whenever(exception.errorCode).thenReturn(400)
        doThrow(exception).whenever(sender).execute(any<SendMessage>())

        service.suspendNotifications(listOf(dto))

        verify(waterStatisticService, never()).recordEvents(argThat { contains(dto.telegramUser) }, any())
    }

    @ParameterizedTest(name = "[{index}] errorCode={0}")
    @ValueSource(ints = [400, 403])
    @DisplayName("suspendNotifications(): DB write for a rejected user fails - the rest of the batch is still recorded")
    fun `suspendNotifications survives a failing db write for a rejected user`(errorCode: Int) {
        val failingChatId = chatId + 1
        val first = DtoGenerator.generateNotificationDto(externalUserId = externalUserId, externalChatId = chatId)
        val failing =
            DtoGenerator.generateNotificationDto(
                externalUserId = externalUserId + 1,
                externalChatId = failingChatId,
            )
        val last =
            DtoGenerator.generateNotificationDto(
                externalUserId = externalUserId + 2,
                externalChatId = chatId + 2,
            )
        val exception = mock<TelegramApiRequestException>()
        whenever(exception.errorCode).thenReturn(errorCode)
        doAnswer { invocation ->
            if (invocation.getArgument<SendMessage>(0).chatId == failingChatId.toString()) {
                throw exception
            }
            null
        }.whenever(sender).execute(any<SendMessage>())
        val dbFailure = NotificationSettingsNotFoundException("not found")
        doThrow(dbFailure).whenever(notificationAccessService).updateTimeOfLastNotification(any(), any())
        doThrow(dbFailure).whenever(notificationAccessService).updateNotificationsDisabled(any())

        service.suspendNotifications(listOf(first, failing, last))

        val captor = argumentCaptor<Collection<NotificationSettingDto>>()
        verify(sender, times(3)).execute(any<SendMessage>())
        verify(notificationAccessService).recordMailingResults(captor.capture())
        assertEquals(listOf(first, last), captor.firstValue.toList())
        verify(waterStatisticService).recordEvents(
            listOf(first.telegramUser, last.telegramUser),
            AnswerNotificationType.CANCEL,
        )
    }

    @Test
    @DisplayName("sendNotifications(): error other than 400/403 - rethrows exception")
    fun `sendNotifications rethrows exception other than 400 and 403`() {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId, externalChatId = chatId)
        val exception = mock<TelegramApiRequestException>()
        whenever(exception.errorCode).thenReturn(500)
        doThrow(exception).whenever(sender).execute(any<SendMessage>())

        assertThrows(TelegramApiRequestException::class.java) {
            service.sendNotifications(listOf(dto))
        }
    }

    @Test
    @DisplayName("sendNotifications(): null errorCode - rethrows exception")
    fun `sendNotifications rethrows null errorCode exception`() {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId, externalChatId = chatId)
        val exception = mock<TelegramApiRequestException>()
        doReturn(null).whenever(exception).errorCode
        doThrow(exception).whenever(sender).execute(any<SendMessage>())

        assertThrows(TelegramApiRequestException::class.java) {
            service.sendNotifications(listOf(dto))
        }
    }

    @Test
    @DisplayName("suspendNotifications(): error other than 400/403 - rethrows exception")
    fun `suspendNotifications rethrows exception other than 400 and 403`() {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId, externalChatId = chatId)
        val exception = mock<TelegramApiRequestException>()
        whenever(exception.errorCode).thenReturn(500)
        doThrow(exception).whenever(sender).execute(any<SendMessage>())

        assertThrows(TelegramApiRequestException::class.java) {
            service.suspendNotifications(listOf(dto))
        }
        verify(notificationAccessService, never()).updateNotificationsDisabled(any())
    }
}

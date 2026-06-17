package ru.illine.drinking.ponies.service.notification

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
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

@UnitTest
@DisplayName("NotificationService Unit Test")
class NotificationServiceTest {

    private val externalUserId = 1L
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
        val dto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId)
        doReturn(true).`when`(notificationAccessService).existsByExternalUserId(externalUserId)
        `when`(notificationAccessService.findNotificationSettingByExternalUserId(externalUserId)).thenReturn(dto)

        service.start(buildMessageContext())

        verify(sender, times(2)).execute(any<SendMessage>())
        verify(notificationAccessService).findNotificationSettingByExternalUserId(externalUserId)
        verify(notificationAccessService, never()).save(any(), any(), any())
    }

    @Test
    @DisplayName("start(): new user - creates user, saves settings, sends default settings message")
    fun `start new user`() {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = externalUserId)
        doReturn(false).`when`(notificationAccessService).existsByExternalUserId(externalUserId)
        `when`(notificationAccessService.findNotificationSettingByExternalUserId(externalUserId)).thenReturn(dto)

        service.start(buildMessageContext())

        verify(notificationAccessService).save(any(), any(), any())
        verify(notificationAccessService).findNotificationSettingByExternalUserId(externalUserId)
        verify(sender, times(2)).execute(any<SendMessage>())
    }

    private fun buildMessageContext(): MessageContext {
        val user = mock(User::class.java)
        `when`(user.id).thenReturn(externalUserId)
        `when`(user.userName).thenReturn("testUser")

        val context = mock(MessageContext::class.java)
        `when`(context.user()).thenReturn(user)
        `when`(context.chatId()).thenReturn(chatId)
        return context
    }
}

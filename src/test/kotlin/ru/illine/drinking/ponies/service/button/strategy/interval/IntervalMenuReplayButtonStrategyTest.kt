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
import ru.illine.drinking.ponies.model.base.SettingsType
import ru.illine.drinking.ponies.service.button.ButtonDataService
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.telegram.TelegramMessageConstants

@UnitTest
@DisplayName("IntervalMenuReplayButtonStrategy Unit Test")
class IntervalMenuReplayButtonStrategyTest {

    private val userId = 1L
    private val chatId = 100500L
    private val messageId = 2

    private lateinit var sender: TelegramClient
    private lateinit var notificationAccessService: NotificationAccessService
    private lateinit var messageEditorService: MessageEditorService
    private lateinit var settingsButtonDataService: ButtonDataService<SettingsType>
    private lateinit var strategy: IntervalMenuReplayButtonStrategy

    @BeforeEach
    fun setUp() {
        sender = mock(TelegramClient::class.java)
        notificationAccessService = mock(NotificationAccessService::class.java)
        messageEditorService = mock(MessageEditorService::class.java)
        @Suppress("UNCHECKED_CAST")
        settingsButtonDataService = mock(ButtonDataService::class.java) as ButtonDataService<SettingsType>
        strategy = IntervalMenuReplayButtonStrategy(
            sender, notificationAccessService, messageEditorService, settingsButtonDataService
        )
    }

    @ParameterizedTest
    @EnumSource(IntervalNotificationType::class)
    @DisplayName("reply(): deletes reply markup on original message")
    fun `reply deletes reply markup`(intervalType: IntervalNotificationType) {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = userId, notificationInterval = intervalType)
        `when`(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(dto)

        strategy.reply(buildCallbackQuery())

        verify(messageEditorService).deleteReplyMarkup(chatId, messageId)
    }

    @ParameterizedTest
    @EnumSource(IntervalNotificationType::class)
    @DisplayName("reply(): sends current interval greeting message with markdown")
    fun `reply sends current interval greeting message`(intervalType: IntervalNotificationType) {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = userId, notificationInterval = intervalType)
        `when`(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(dto)

        val captor = ArgumentCaptor.forClass(SendMessage::class.java)
        strategy.reply(buildCallbackQuery())

        verify(sender, times(2)).execute(captor.capture())
        val firstMessage = captor.allValues[0]
        assertEquals(chatId.toString(), firstMessage.chatId)
        assertEquals(
            TelegramMessageConstants.SETTINGS_NOTIFICATION_INTERVAL_GREETING_MESSAGE.format(intervalType.displayName),
            firstMessage.text
        )
        assertEquals("Markdown", firstMessage.parseMode)
    }

    @ParameterizedTest
    @EnumSource(IntervalNotificationType::class)
    @DisplayName("reply(): sends interval buttons message with keyboard excluding current interval")
    fun `reply sends interval buttons message with keyboard`(intervalType: IntervalNotificationType) {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = userId, notificationInterval = intervalType)
        `when`(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(dto)

        val captor = ArgumentCaptor.forClass(SendMessage::class.java)
        strategy.reply(buildCallbackQuery())

        verify(sender, times(2)).execute(captor.capture())
        val secondMessage = captor.allValues[1]
        assertEquals(chatId.toString(), secondMessage.chatId)
        assertEquals(TelegramMessageConstants.SETTINGS_NOTIFICATION_INTERVAL_BUTTON_MESSAGE, secondMessage.text)
        assertNotNull(secondMessage.replyMarkup)
    }

    @Test
    @DisplayName("isQueryData(): returns true when queryData matches interval button data")
    fun `isQueryData returns true for interval button data`() {
        `when`(settingsButtonDataService.getData(SettingsType.NOTIFICATION_INTERVAL)).thenReturn("interval-data")

        val result = strategy.isQueryData("interval-data")

        assertTrue(result)
    }

    @Test
    @DisplayName("isQueryData(): returns false for other queryData")
    fun `isQueryData returns false for other data`() {
        `when`(settingsButtonDataService.getData(SettingsType.NOTIFICATION_INTERVAL)).thenReturn("interval-data")

        val result = strategy.isQueryData("other-data")

        assertFalse(result)
    }

    private fun buildCallbackQuery(): CallbackQuery {
        val user = mock(User::class.java)
        `when`(user.id).thenReturn(userId)

        val message = mock(Message::class.java)
        `when`(message.chatId).thenReturn(chatId)
        `when`(message.messageId).thenReturn(messageId)

        val callbackQuery = mock(CallbackQuery::class.java)
        `when`(callbackQuery.from).thenReturn(user)
        `when`(callbackQuery.message).thenReturn(message)
        return callbackQuery
    }
}

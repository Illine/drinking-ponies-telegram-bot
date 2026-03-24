package ru.illine.drinking.ponies.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.IntervalNotificationType
import ru.illine.drinking.ponies.model.base.SettingsType
import ru.illine.drinking.ponies.model.base.SnoozeNotificationType
import ru.illine.drinking.ponies.service.button.ButtonDataService
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.telegram.TelegramBotKeyboardHelper

@UnitTest
@DisplayName("TelegramBotKeyboardHelper Unit Test")
class TelegramBotKeyboardHelperTest {

    private val notificationInterval = "fd789961-0706-47fa-869d-a17a5ecc871b"
    private val queryModeTime = "https://quietmodetime.url"
    private val timezone = "f99bf271-8e39-4a62-87d7-13fbcbc85355"

    private lateinit var service: ButtonDataService<SettingsType>

    @BeforeEach
    fun setUp() {
        @Suppress("unchecked_cast")
        service = mock(ButtonDataService::class.java) as ButtonDataService<SettingsType>
        `when`(service.getData(SettingsType.NOTIFICATION_INTERVAL)).thenReturn(notificationInterval)
        `when`(service.getData(SettingsType.QUIET_MODE_TIME)).thenReturn(queryModeTime)
        `when`(service.getData(SettingsType.TIMEZONE)).thenReturn(timezone)
    }

    // ToDo Добавить проверку, что такие-то кнопки являются webApp
    @Test
    @DisplayName("settingsButtons(): returns valid keyboard without messageId")
    fun `successful settingsButtons`() {
        val expectedButtonsSize = 1
        val expectedRowsSize =
            SettingsType.entries
                .filter { it.visible }
                .count()

        val actual = TelegramBotKeyboardHelper.settingsButtons(service)

        assertNotNull(actual)
        assertDoesNotThrow { actual.validate() }
        assertEquals(expectedRowsSize, actual.keyboard.size)
        assertEquals(expectedButtonsSize, actual.keyboard[0].size)
    }

    @Test
    @DisplayName("settingsButtons(): appends messageId to web button url when messageId provided")
    fun `successful settingsButtons with messageId`() {
        val messageId = 1

        val actual = TelegramBotKeyboardHelper.settingsButtons(service, messageId)

        assertNotNull(actual)
        assertDoesNotThrow { actual.validate() }
    }

    @ParameterizedTest
    @EnumSource(IntervalNotificationType::class)
    @DisplayName("intervalTimeButtons(): returns valid keyboard")
    fun `successful intervalTimeButtons`(intervalNotificationType: IntervalNotificationType) {
        val expectedButtonsSize = 1
        val expectedRowsSize = IntervalNotificationType.entries.size - 1

        val actual =
            TelegramBotKeyboardHelper.intervalTimeButtons(intervalNotificationType) as InlineKeyboardMarkup

        assertNotNull(actual)
        assertDoesNotThrow { actual.validate() }
        assertEquals(expectedRowsSize, actual.keyboard.size)
        assertEquals(expectedButtonsSize, actual.keyboard[0].size)
    }

    @Test
    @DisplayName("intervalTimeButtons(): returns valid keyboard")
    fun `successful intervalTimeButtons default arg`() {
        val expectedButtonsSize = 1
        val expectedRowsSize = IntervalNotificationType.entries.size

        val actual =
            TelegramBotKeyboardHelper.intervalTimeButtons() as InlineKeyboardMarkup

        assertNotNull(actual)
        assertDoesNotThrow { actual.validate() }
        assertEquals(expectedRowsSize, actual.keyboard.size)
        assertEquals(expectedButtonsSize, actual.keyboard[0].size)
    }

    @Test
    @DisplayName("snoozeTimeButtons(): returns valid keyboard")
    fun `successful snoozeTimeButtons`() {
        val expectedButtonsSize = 1
        val expectedRowsSize = SnoozeNotificationType.entries.size

        val actual =
            TelegramBotKeyboardHelper.snoozeTimeButtons() as InlineKeyboardMarkup

        assertNotNull(actual)
        assertDoesNotThrow { actual.validate() }
        assertEquals(expectedRowsSize, actual.keyboard.size)
        assertEquals(expectedButtonsSize, actual.keyboard[0].size)
    }

    @Test
    @DisplayName("notifyButtons(): returns valid keyboard")
    fun `successful notifyButtons`() {
        val expectedRowsSize = 1
        val expectedButtonsSize = AnswerNotificationType.entries.size


        val actual =
            TelegramBotKeyboardHelper.notifyButtons() as InlineKeyboardMarkup

        assertNotNull(actual)
        assertDoesNotThrow { actual.validate() }
        assertEquals(expectedRowsSize, actual.keyboard.size)
        assertEquals(expectedButtonsSize, actual.keyboard[0].size)
    }
}
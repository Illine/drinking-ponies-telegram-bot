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
import ru.illine.drinking.ponies.model.base.DelayNotificationType
import ru.illine.drinking.ponies.model.base.SettingsType
import ru.illine.drinking.ponies.service.button.ButtonDataService
import ru.illine.drinking.ponies.test.tag.UnitTest

@UnitTest
@DisplayName("TelegramBotKeyboardHelper Unit Test")
class TelegramBotKeyboardHelperTest {

    private val delayNotification = "fd789961-0706-47fa-869d-a17a5ecc871b"
    private val queryModeTime = "https://quietmodetime.url"
    private val timezone = "f99bf271-8e39-4a62-87d7-13fbcbc85355"

    private lateinit var service: ButtonDataService<SettingsType>

    @BeforeEach
    fun setUp() {
        @Suppress("unchecked_cast")
        service = mock(ButtonDataService::class.java) as ButtonDataService<SettingsType>
        `when`(service.getData(SettingsType.DELAY_NOTIFICATION)).thenReturn(delayNotification)
        `when`(service.getData(SettingsType.QUIET_MODE_TIME)).thenReturn(queryModeTime)
        `when`(service.getData(SettingsType.TIMEZONE)).thenReturn(timezone)
    }
    // settingsButtons

    // ToDo Добавить проверку, что такие-то кнопки являются webApp
    @Test
    @DisplayName("settingsButtons(): returns valid keyboard")
    fun `successful settingsButtons`() {
        val expectedButtonsSize = 1
        val expectedRowsSize =
            SettingsType.values()
                .filter { it.visible }
                .count()

        val actual =
            TelegramBotKeyboardHelper.settingsButtons(service) as InlineKeyboardMarkup

        assertNotNull(actual)
        assertDoesNotThrow { actual.validate() }
        assertEquals(expectedRowsSize, actual.keyboard.size)
        assertEquals(expectedButtonsSize, actual.keyboard[0].size)
    }

    // delayTimeButtons

    @ParameterizedTest
    @EnumSource(DelayNotificationType::class)
    @DisplayName("delayTimeButtons(): returns valid keyboard")
    fun `successful delayTimeButtons`(delayNotificationType: DelayNotificationType) {
        val expectedButtonsSize = 1
        val expectedRowsSize = DelayNotificationType.values().size - 1

        val actual =
            TelegramBotKeyboardHelper.delayTimeButtons(delayNotificationType) as InlineKeyboardMarkup

        assertNotNull(actual)
        assertDoesNotThrow { actual.validate() }
        assertEquals(expectedRowsSize, actual.keyboard.size)
        assertEquals(expectedButtonsSize, actual.keyboard[0].size)
    }

    @Test
    @DisplayName("delayTimeButtons(): returns valid keyboard")
    fun `successful delayTimeButtons default arg`() {
        val expectedButtonsSize = 1
        val expectedRowsSize = DelayNotificationType.values().size

        val actual =
            TelegramBotKeyboardHelper.delayTimeButtons() as InlineKeyboardMarkup

        assertNotNull(actual)
        assertDoesNotThrow { actual.validate() }
        assertEquals(expectedRowsSize, actual.keyboard.size)
        assertEquals(expectedButtonsSize, actual.keyboard[0].size)
    }

    // notifyButtons

    @Test
    @DisplayName("notifyButtons(): returns valid keyboard")
    fun `successful notifyButtons`() {
        val expectedRowsSize = 1
        val expectedButtonsSize = AnswerNotificationType.values().size


        val actual =
            TelegramBotKeyboardHelper.notifyButtons() as InlineKeyboardMarkup

        assertNotNull(actual)
        assertDoesNotThrow { actual.validate() }
        assertEquals(expectedRowsSize, actual.keyboard.size)
        assertEquals(expectedButtonsSize, actual.keyboard[0].size)
    }
}
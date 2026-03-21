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
import ru.illine.drinking.ponies.model.base.SettingsType
import ru.illine.drinking.ponies.model.base.TimeNotificationType
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
            SettingsType.entries
                .filter { it.visible }
                .count()

        val actual = TelegramBotKeyboardHelper.settingsButtons(service)

        assertNotNull(actual)
        assertDoesNotThrow { actual.validate() }
        assertEquals(expectedRowsSize, actual.keyboard.size)
        assertEquals(expectedButtonsSize, actual.keyboard[0].size)
    }

    // timeOptionButtons

    @ParameterizedTest
    @EnumSource(TimeNotificationType::class)
    @DisplayName("timeOptionButtons(): returns valid keyboard with exclude")
    fun `successful timeOptionButtons with exclude`(timeNotificationType: TimeNotificationType) {
        val expectedButtonsSize = 1
        val expectedRowsSize = TimeNotificationType.entries.size - 1

        val actual =
            TelegramBotKeyboardHelper.timeOptionButtons(
                TimeNotificationType.entries,
                exclude = timeNotificationType
            ) as InlineKeyboardMarkup

        assertNotNull(actual)
        assertDoesNotThrow { actual.validate() }
        assertEquals(expectedRowsSize, actual.keyboard.size)
        assertEquals(expectedButtonsSize, actual.keyboard[0].size)
    }

    @Test
    @DisplayName("timeOptionButtons(): returns valid keyboard without exclude")
    fun `successful timeOptionButtons without exclude`() {
        val expectedButtonsSize = 1
        val expectedRowsSize = TimeNotificationType.entries.size

        val actual =
            TelegramBotKeyboardHelper.timeOptionButtons(
                TimeNotificationType.entries
            ) as InlineKeyboardMarkup

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
        val expectedButtonsSize = AnswerNotificationType.entries.size


        val actual =
            TelegramBotKeyboardHelper.notifyButtons() as InlineKeyboardMarkup

        assertNotNull(actual)
        assertDoesNotThrow { actual.validate() }
        assertEquals(expectedRowsSize, actual.keyboard.size)
        assertEquals(expectedButtonsSize, actual.keyboard[0].size)
    }
}
package ru.illine.drinking.ponies.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.DelayNotificationType
import ru.illine.drinking.ponies.model.base.SettingsType
import ru.illine.drinking.ponies.test.tag.UnitTest
import java.util.*

@UnitTest
@DisplayName("TelegramBotKeyboardHelper Unit Test")
class TelegramBotKeyboardHelperTest {

    // settingsButtons

    @Test
    @DisplayName("settingsButtons(): returns valid keyboard")
    fun `successful settingsButtons`() {
        val expectedButtonsSize = 1
        val expectedRowsSize =
            SettingsType.values()
                .filter { it.visible }
                .count()

        val actual =
            TelegramBotKeyboardHelper.settingsButtons() as InlineKeyboardMarkup

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
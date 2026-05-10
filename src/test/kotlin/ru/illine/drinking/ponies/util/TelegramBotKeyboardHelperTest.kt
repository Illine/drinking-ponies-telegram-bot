package ru.illine.drinking.ponies.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.SnoozeNotificationType
import ru.illine.drinking.ponies.model.base.WaterAmountType
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.telegram.TelegramBotKeyboardHelper

@UnitTest
@DisplayName("TelegramBotKeyboardHelper Unit Test")
class TelegramBotKeyboardHelperTest {

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
    @DisplayName("waterAmountButtons(): returns valid keyboard")
    fun `successful waterAmountButtons`() {
        val expectedButtonsSize = 1
        val expectedRowsSize = WaterAmountType.entries.size

        val actual =
            TelegramBotKeyboardHelper.waterAmountButtons() as InlineKeyboardMarkup

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

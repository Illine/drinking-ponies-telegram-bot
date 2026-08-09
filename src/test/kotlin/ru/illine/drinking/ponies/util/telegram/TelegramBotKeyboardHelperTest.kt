package ru.illine.drinking.ponies.util.telegram

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.SnoozeNotificationType
import ru.illine.drinking.ponies.model.base.WaterAmountType
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.telegram.TelegramBotKeyboardHelper
import java.util.stream.Stream

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
    @DisplayName("waterAmountButtons(): returns valid keyboard laid out as two rows of three buttons")
    fun `successful waterAmountButtons`() {
        val expectedRowsSize = 2
        val expectedButtonsSize = 3

        val actual =
            TelegramBotKeyboardHelper.waterAmountButtons() as InlineKeyboardMarkup

        assertNotNull(actual)
        assertDoesNotThrow { actual.validate() }
        assertEquals(expectedRowsSize, actual.keyboard.size)
        actual.keyboard.forEach { assertEquals(expectedButtonsSize, it.size) }
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

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideKeyboardButtonCases")
    @DisplayName("keyboards: every entry becomes a button carrying its display name and query data")
    fun `keyboard buttons carry display name and query data of every entry`(
        @Suppress("UNUSED_PARAMETER") keyboardName: String,
        keyboardFactory: () -> ReplyKeyboard,
        expectedTexts: List<String>,
        expectedCallbackData: List<String>,
    ) {
        val actual = keyboardFactory() as InlineKeyboardMarkup

        val buttons = actual.keyboard.flatten()

        assertEquals(expectedTexts, buttons.map { it.text })
        assertEquals(expectedCallbackData, buttons.map { it.callbackData })
    }

    companion object {
        @JvmStatic
        fun provideKeyboardButtonCases(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    "snoozeTimeButtons",
                    TelegramBotKeyboardHelper::snoozeTimeButtons,
                    SnoozeNotificationType.entries.map { it.displayName },
                    SnoozeNotificationType.entries.map { it.queryData.toString() },
                ),
                Arguments.of(
                    "waterAmountButtons",
                    TelegramBotKeyboardHelper::waterAmountButtons,
                    WaterAmountType.entries.map { it.displayName },
                    WaterAmountType.entries.map { it.queryData.toString() },
                ),
                Arguments.of(
                    "notifyButtons",
                    TelegramBotKeyboardHelper::notifyButtons,
                    AnswerNotificationType.entries.map { it.displayName },
                    AnswerNotificationType.entries.map { it.queryData.toString() },
                ),
            )
    }
}

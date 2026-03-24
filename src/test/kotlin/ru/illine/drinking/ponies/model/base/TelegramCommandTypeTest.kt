package ru.illine.drinking.ponies.model.base

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.illine.drinking.ponies.test.tag.UnitTest

@UnitTest
@DisplayName("TelegramCommandType Unit Test")
class TelegramCommandTypeTest {

    @Test
    @DisplayName("toString(): returns command value for each entry")
    fun `toString returns command`() {
        TelegramCommandType.entries.forEach { entry ->
            assertEquals(entry.command, entry.toString())
        }
    }
}

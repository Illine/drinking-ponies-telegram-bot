package ru.illine.drinking.ponies.model.base

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.boot.logging.LogLevel
import ru.illine.drinking.ponies.test.tag.UnitTest

@UnitTest
@DisplayName("LogLevelType Unit Test")
class LogLevelTypeTest {
    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(LogLevelType::class)
    @DisplayName("level: an entry carries the logging level of the same name, so the wire value is not renamed")
    fun `entries carry the level of the same name`(type: LogLevelType) {
        assertEquals(LogLevel.valueOf(type.name), type.level)
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(LogLevelType::class)
    @DisplayName("of(): a level read back from a logger is translated into the entry that set it")
    fun `of translates a level back into its entry`(type: LogLevelType) {
        assertEquals(type, LogLevelType.of(type.level))
    }

    @Test
    @DisplayName("of(): FATAL is the only level with no entry, the logging system applying it as ERROR instead")
    fun `fatal is the only level left out`() {
        val withoutEntry = LogLevel.entries.filter { LogLevelType.of(it) == null }

        assertEquals(listOf(LogLevel.FATAL), withoutEntry)
    }

    @Test
    @DisplayName("of(): a logger with no level at all yields no entry instead of a default one")
    fun `no level yields no entry`() {
        assertNull(LogLevelType.of(null))
    }
}

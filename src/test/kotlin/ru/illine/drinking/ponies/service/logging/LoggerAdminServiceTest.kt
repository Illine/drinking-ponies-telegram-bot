package ru.illine.drinking.ponies.service.logging

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.logging.LogLevel
import org.springframework.boot.logging.LoggerConfiguration
import org.springframework.boot.logging.LoggingSystem
import ru.illine.drinking.ponies.model.base.AppLogger
import ru.illine.drinking.ponies.service.logging.impl.LoggerAdminServiceImpl
import ru.illine.drinking.ponies.test.tag.UnitTest

@UnitTest
@DisplayName("LoggerAdminService Unit Test")
class LoggerAdminServiceTest {
    private val loggingSystem = mock<LoggingSystem>()

    private val service = LoggerAdminServiceImpl(loggingSystem)

    @Test
    @DisplayName("getKnownLevels(): asks the logging system about every AppLogger entry")
    fun `known levels cover the enum`() {
        whenever(loggingSystem.getLoggerConfiguration(any()))
            .thenAnswer { LoggerConfiguration(it.arguments[0] as String, null, LogLevel.INFO) }

        val levels = service.getKnownLevels()

        assertEquals(PUBLISHED_LOGGERS.map { it.value }, levels.map { it.name })
        PUBLISHED_LOGGERS.forEach { verify(loggingSystem).getLoggerConfiguration(it.value) }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(AppLogger::class)
    @DisplayName("getLevel(): a logger the application declares is reported as its own")
    fun `declared loggers are reported as ours`(appLogger: AppLogger) {
        whenever(loggingSystem.getLoggerConfiguration(appLogger.value))
            .thenReturn(LoggerConfiguration(appLogger.value, null, LogLevel.INFO))

        assertTrue(service.getLevel(appLogger.value).application)
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = [THIRD_PARTY_LOGGER, UNKNOWN_LOGGER])
    @DisplayName("getLevel(): a logger outside the enum is not reported as ours, the dots in its name aside")
    fun `foreign loggers are not reported as ours`(name: String) {
        whenever(loggingSystem.getLoggerConfiguration(name))
            .thenReturn(LoggerConfiguration(name, null, LogLevel.INFO))

        assertFalse(service.getLevel(name).application)
    }

    @Test
    @DisplayName("getKnownLevels(): a third-party logger with an explicit level joins the list, once")
    fun `known levels pick up configured outsiders`() {
        whenever(loggingSystem.loggerConfigurations)
            .thenReturn(
                listOf(
                    LoggerConfiguration(AppLogger.ROOT.value, LogLevel.INFO, LogLevel.INFO),
                    LoggerConfiguration(AppLogger.AUDIT.value, LogLevel.INFO, LogLevel.INFO),
                    LoggerConfiguration(THIRD_PARTY_LOGGER, LogLevel.DEBUG, LogLevel.DEBUG),
                    LoggerConfiguration(UNKNOWN_LOGGER, null, LogLevel.INFO),
                ),
            )
        whenever(loggingSystem.getLoggerConfiguration(any()))
            .thenAnswer { LoggerConfiguration(it.arguments[0] as String, null, LogLevel.INFO) }

        val names = service.getKnownLevels().map { it.name }

        assertEquals(PUBLISHED_LOGGERS.map { it.value } + THIRD_PARTY_LOGGER, names)
    }

    @Test
    @DisplayName("getKnownLevels(): tells our loggers from the libraries', the flat list carrying both")
    fun `known levels tell ours from the libraries`() {
        whenever(loggingSystem.loggerConfigurations)
            .thenReturn(listOf(LoggerConfiguration(THIRD_PARTY_LOGGER, LogLevel.DEBUG, LogLevel.DEBUG)))
        whenever(loggingSystem.getLoggerConfiguration(any()))
            .thenAnswer { LoggerConfiguration(it.arguments[0] as String, null, LogLevel.INFO) }

        val ours = service.getKnownLevels().filter { it.application }.map { it.name }

        assertEquals(PUBLISHED_LOGGERS.map { it.value }, ours)
    }

    @Test
    @DisplayName("setLevel(): refuses to touch the audit logger, which would silence the record of doing so")
    fun `audit logger cannot be reconfigured`() {
        assertThrows<IllegalArgumentException> {
            service.setLevel(AppLogger.AUDIT.value, LogLevel.OFF, ACTOR_ID)
        }

        verify(loggingSystem, never()).setLogLevel(any(), eq(LogLevel.OFF))
    }

    @Test
    @DisplayName("the audit logger gets an explicit level on startup, so lowering the root cannot reach it")
    fun `audit logger is pinned on startup`() {
        verify(loggingSystem).setLogLevel(eq(AppLogger.AUDIT.value), eq(LogLevel.INFO))
    }

    @Test
    @DisplayName("resetLevels(): puts back the startup level instead of clearing every logger")
    fun `reset returns loggers to their startup level`() {
        whenever(loggingSystem.loggerConfigurations)
            .thenReturn(listOf(LoggerConfiguration(CONFIGURED_LOGGER, LogLevel.WARN, LogLevel.WARN)))
            .thenReturn(
                listOf(
                    LoggerConfiguration(CONFIGURED_LOGGER, LogLevel.DEBUG, LogLevel.DEBUG),
                    LoggerConfiguration(THIRD_PARTY_LOGGER, LogLevel.DEBUG, LogLevel.DEBUG),
                ),
            )
        whenever(loggingSystem.getLoggerConfiguration(any()))
            .thenAnswer { LoggerConfiguration(it.arguments[0] as String, null, LogLevel.INFO) }
        val started = LoggerAdminServiceImpl(loggingSystem)

        started.resetLevels(ACTOR_ID)

        verify(loggingSystem).setLogLevel(eq(CONFIGURED_LOGGER), eq(LogLevel.WARN))
        verify(loggingSystem).setLogLevel(eq(THIRD_PARTY_LOGGER), eq(null))
    }

    @Test
    @DisplayName("getLevel(): an unknown logger has no level rather than a missing answer")
    fun `unknown logger has no level`() {
        whenever(loggingSystem.getLoggerConfiguration(UNKNOWN_LOGGER)).thenReturn(null)

        val level = service.getLevel(UNKNOWN_LOGGER)

        assertEquals(UNKNOWN_LOGGER, level.name)
        assertNull(level.configuredLevel)
        assertNull(level.effectiveLevel)
    }

    @Test
    @DisplayName("setLevel(): applies the level and answers with the state that followed the change")
    fun `set level applies and reports the new state`() {
        whenever(loggingSystem.getLoggerConfiguration(AppLogger.SQL.value))
            .thenReturn(LoggerConfiguration(AppLogger.SQL.value, null, LogLevel.WARN))
            .thenReturn(LoggerConfiguration(AppLogger.SQL.value, LogLevel.DEBUG, LogLevel.DEBUG))

        val applied = service.setLevel(AppLogger.SQL.value, LogLevel.DEBUG, ACTOR_ID)

        verify(loggingSystem).setLogLevel(eq(AppLogger.SQL.value), eq(LogLevel.DEBUG))
        assertEquals(LogLevel.DEBUG, applied.configuredLevel)
        assertEquals(LogLevel.DEBUG, applied.effectiveLevel)
        assertTrue(applied.application)
    }

    @Test
    @DisplayName("setLevel(): records the actor before applying, so silencing a logger keeps its own record")
    fun `audit record precedes the change`() {
        whenever(loggingSystem.getLoggerConfiguration(AppLogger.SQL.value))
            .thenReturn(LoggerConfiguration(AppLogger.SQL.value, null, LogLevel.WARN))
        val auditLogger = AppLogger.AUDIT.logger as Logger
        val auditRecords = ListAppender<ILoggingEvent>().apply { start() }
        auditLogger.addAppender(auditRecords)
        doAnswer { assertTrue(auditRecords.list.isNotEmpty(), "the change was applied before it was recorded") }
            .whenever(loggingSystem)
            .setLogLevel(any(), any())

        try {
            service.setLevel(AppLogger.SQL.value, LogLevel.OFF, ACTOR_ID)
        } finally {
            auditLogger.detachAppender(auditRecords)
        }

        val record = auditRecords.list.single().formattedMessage
        assertTrue(record.contains(ACTOR_ID.toString()), "the audit record names the actor: $record")
        assertTrue(record.contains(AppLogger.SQL.value), "the audit record names the logger: $record")
        assertTrue(record.contains(LogLevel.OFF.name), "the audit record names the new level: $record")
    }

    companion object {
        private const val ACTOR_ID = 42L
        private const val UNKNOWN_LOGGER = "no.such.logger"
        private const val THIRD_PARTY_LOGGER = "org.hibernate.SQL"
        private const val CONFIGURED_LOGGER = "org.telegram.telegrambots.abilitybots.api.bot"

        private val PUBLISHED_LOGGERS = AppLogger.entries - AppLogger.AUDIT
    }
}

package ru.illine.drinking.ponies.controller

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.logging.LogLevel
import org.springframework.boot.logging.LoggingSystem
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.cache.CacheManager
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlConfig
import ru.illine.drinking.ponies.config.cache.CacheConfig
import ru.illine.drinking.ponies.config.web.security.AuthErrorType
import ru.illine.drinking.ponies.model.base.AppLogger
import ru.illine.drinking.ponies.model.base.LogLevelType
import ru.illine.drinking.ponies.model.dto.response.LoggerLevelResponse
import ru.illine.drinking.ponies.model.dto.response.LoggersResponse
import ru.illine.drinking.ponies.service.telegram.TelegramValidatorService
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest

@SpringIntegrationTest
@DisplayName("LoggerAdminController Spring Integration Test")
@Sql(
    scripts = ["classpath:sql/access/TelegramUserAccessService.sql"],
    config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
)
@Sql(
    scripts = ["classpath:sql/clear.sql"],
    config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
)
class LoggerAdminControllerTest
    @Autowired
    constructor(
        private val restTemplate: TestRestTemplate,
        private val cacheManager: CacheManager,
        private val loggingSystem: LoggingSystem,
    ) {
        @MockitoBean
        private lateinit var telegramValidatorService: TelegramValidatorService

        private val adminUser = DtoGenerator.generateTelegramAuthUserDto(externalUserId = ADMIN_EXTERNAL_ID)

        private val nonAdminUser = DtoGenerator.generateTelegramAuthUserDto(externalUserId = PLAIN_EXTERNAL_ID)

        @BeforeEach
        fun setUp() {
            cacheManager.getCache(CacheConfig.USER_ACCESS_FLAGS)?.clear()
            whenever(telegramValidatorService.verifySignature(any())).thenReturn(true)
            whenever(telegramValidatorService.map(any())).thenReturn(adminUser)
        }

        @AfterEach
        fun tearDown() {
            loggingSystem.setLogLevel(SANDBOX_LOGGER, null)
        }

        private fun buildHeaders(): HttpHeaders =
            HttpHeaders().apply {
                set("X-Authorization-Telegram-Data", "test-init-data")
                contentType = MediaType.APPLICATION_JSON
            }

        private fun getLoggers(): LoggersResponse {
            val response =
                restTemplate.exchange(
                    "/systems/loggers",
                    HttpMethod.GET,
                    HttpEntity<Void>(buildHeaders()),
                    LoggersResponse::class.java,
                )

            assertEquals(HttpStatus.OK, response.statusCode)
            return response.body!!
        }

        private fun getLogger(name: String): LoggerLevelResponse {
            val response =
                restTemplate.exchange(
                    "/systems/loggers/$name",
                    HttpMethod.GET,
                    HttpEntity<Void>(buildHeaders()),
                    LoggerLevelResponse::class.java,
                )

            assertEquals(HttpStatus.OK, response.statusCode)
            return response.body!!
        }

        private fun putLevel(
            name: String,
            body: String,
        ) = restTemplate.exchange(
            "/systems/loggers/$name",
            HttpMethod.PUT,
            HttpEntity(body, buildHeaders()),
            String::class.java,
        )

        private fun putLevelForResponse(
            name: String,
            body: String,
        ): LoggerLevelResponse {
            val response =
                restTemplate.exchange(
                    "/systems/loggers/$name",
                    HttpMethod.PUT,
                    HttpEntity(body, buildHeaders()),
                    LoggerLevelResponse::class.java,
                )

            assertEquals(HttpStatus.OK, response.statusCode)
            return response.body!!
        }

        private fun resetLevels(): LoggersResponse {
            val response =
                restTemplate.exchange(
                    "/systems/loggers/reset",
                    HttpMethod.POST,
                    HttpEntity<Void>(buildHeaders()),
                    LoggersResponse::class.java,
                )

            assertEquals(HttpStatus.OK, response.statusCode)
            return response.body!!
        }

        @Nested
        @DisplayName("GET /loggers")
        inner class GetLoggers {
            @Test
            @DisplayName("opens with every logger the application declares, in declaration order")
            fun `lists the declared loggers`() {
                val names = getLoggers().loggers.map { it.name }

                assertEquals(PUBLISHED_LOGGERS.map { it.value }, names.take(PUBLISHED_LOGGERS.size))
            }

            @Test
            @DisplayName("the audit logger is not offered, so nobody is tempted to silence it")
            fun `hides the audit logger`() {
                assertFalse(getLoggers().loggers.any { it.name == AppLogger.AUDIT.value })
            }

            @Test
            @DisplayName("a logger raised through this API stays listed, so it can be turned back down")
            fun `lists what the admin has touched`() {
                assertFalse(getLoggers().loggers.any { it.name == SANDBOX_LOGGER })

                putLevel(SANDBOX_LOGGER, """{"level": "DEBUG"}""")

                assertTrue(getLoggers().loggers.any { it.name == SANDBOX_LOGGER })
            }

            @Test
            @DisplayName("tells our loggers from the libraries', so the client does not guess by the dot in a name")
            fun `marks which loggers are ours`() {
                putLevel(SANDBOX_LOGGER, """{"level": "DEBUG"}""")

                val loggers = getLoggers().loggers

                assertEquals(PUBLISHED_LOGGERS.map { it.value }, loggers.filter { it.application }.map { it.name })
                assertFalse(loggers.single { it.name == SANDBOX_LOGGER }.application)
            }

            @Test
            @DisplayName("a logger with no level of its own reports the one inherited from the root")
            fun `reports the inherited level`() {
                val loggers = getLoggers().loggers
                val service = loggers.single { it.name == AppLogger.SERVICE.value }
                val root = loggers.single { it.name == AppLogger.ROOT.value }

                assertNull(service.configuredLevel)
                assertEquals(root.effectiveLevel, service.effectiveLevel)
            }
        }

        @Nested
        @DisplayName("GET /loggers/{name}")
        inner class GetLogger {
            @Test
            @DisplayName("a third-party logger is readable, not only the ones we declare")
            fun `reads a third party logger`() {
                val body = getLogger(THIRD_PARTY_LOGGER)

                assertEquals(THIRD_PARTY_LOGGER, body.name)
                assertNotNull(body.effectiveLevel)
                assertFalse(body.application)
            }

            @Test
            @DisplayName("one of our own loggers is answered as ours, read by name and not only in the list")
            fun `marks a single logger of ours`() {
                assertTrue(getLogger(AppLogger.SERVICE.value).application)
            }

            @Test
            @DisplayName("a logger nobody has created yet answers with no level instead of an error")
            fun `an unknown logger has no level`() {
                val body = getLogger(UNKNOWN_LOGGER)

                assertNull(body.configuredLevel)
                assertNull(body.effectiveLevel)
                assertFalse(body.application)
            }
        }

        @Nested
        @DisplayName("PUT /loggers/{name}")
        inner class SetLevel {
            @Test
            @DisplayName("the new level takes effect on the running logger, no restart involved")
            fun `applies the level at runtime`() {
                assertTrue(LoggerFactory.getLogger(SANDBOX_LOGGER).isErrorEnabled)

                val response = putLevel(SANDBOX_LOGGER, """{"level": "OFF"}""")

                assertEquals(HttpStatus.OK, response.statusCode)
                assertFalse(LoggerFactory.getLogger(SANDBOX_LOGGER).isErrorEnabled)
            }

            @ParameterizedTest(name = "[{index}] {0}")
            @EnumSource(LogLevelType::class)
            @DisplayName("a level of the contract is applied as asked and answered back as the very same level")
            fun `answers with the level that was asked for`(level: LogLevelType) {
                val body = putLevelForResponse(SANDBOX_LOGGER, """{"level": "${level.name}"}""")

                assertEquals(level, body.configuredLevel)
                assertEquals(level, body.effectiveLevel)
                assertEquals(
                    LogLevel.valueOf(level.name),
                    loggingSystem.getLoggerConfiguration(SANDBOX_LOGGER)?.configuredLevel,
                )
            }

            @Test
            @DisplayName("the answer says whether the logger is ours, so the changed row keeps its section")
            fun `reports ownership of the changed logger`() {
                val sqlWas = loggingSystem.getLoggerConfiguration(AppLogger.SQL.value).configuredLevel

                try {
                    assertTrue(putLevelForResponse(AppLogger.SQL.value, """{"level": "DEBUG"}""").application)
                } finally {
                    loggingSystem.setLogLevel(AppLogger.SQL.value, sqlWas)
                }

                assertFalse(putLevelForResponse(SANDBOX_LOGGER, """{"level": "DEBUG"}""").application)
            }

            @ParameterizedTest(name = "[{index}] {0}")
            @ValueSource(
                strings = ["""{"level": "FATAL"}""", """{"level": "LOUD"}""", """{"level": null}""", "{}"],
            )
            @DisplayName("a level outside the contract is rejected with 400, the logger keeping the level it had")
            fun `rejects an unknown level`(body: String) {
                loggingSystem.setLogLevel(SANDBOX_LOGGER, LogLevel.DEBUG)

                val response = putLevel(SANDBOX_LOGGER, body)

                assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
                assertEquals(LogLevel.DEBUG, loggingSystem.getLoggerConfiguration(SANDBOX_LOGGER)?.configuredLevel)
            }

            @Test
            @DisplayName("the audit logger is refused, and keeps the level logback pinned it to")
            fun `refuses to silence the audit logger`() {
                val response = putLevel(AppLogger.AUDIT.value, """{"level": "OFF"}""")

                assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
                assertTrue(LoggerFactory.getLogger(AppLogger.AUDIT.value).isInfoEnabled)
            }

            @Test
            @DisplayName("silencing the root logger leaves the audit trail alone, its level being pinned")
            fun `root cannot silence the audit logger`() {
                val rootWas = loggingSystem.getLoggerConfiguration(AppLogger.ROOT.value).configuredLevel

                try {
                    putLevel(AppLogger.ROOT.value, """{"level": "OFF"}""")

                    assertTrue(LoggerFactory.getLogger(AppLogger.AUDIT.value).isInfoEnabled)
                } finally {
                    loggingSystem.setLogLevel(AppLogger.ROOT.value, rootWas)
                }
            }

            @Test
            @DisplayName("a name outside the allowed charset is rejected, so no junk logger is created")
            fun `rejects a malformed name`() {
                val response = putLevel(MALFORMED_LOGGER, """{"level": "DEBUG"}""")

                assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
                assertNull(loggingSystem.getLoggerConfiguration(MALFORMED_LOGGER))
            }
        }

        @Nested
        @DisplayName("POST /loggers/reset")
        inner class ResetLevels {
            @Test
            @DisplayName("puts a raised logger back to inheriting, and drops it from the list")
            fun `reset undoes a raised level`() {
                putLevel(SANDBOX_LOGGER, """{"level": "DEBUG"}""")

                val body = resetLevels()

                assertNull(getLogger(SANDBOX_LOGGER).configuredLevel)
                assertFalse(body.loggers.any { it.name == SANDBOX_LOGGER })
            }

            @Test
            @DisplayName("a logger configured at startup keeps its level, the reset not clearing everything")
            fun `reset keeps configured levels`() {
                val configured = getLoggers().loggers.filter { it.configuredLevel != null }

                resetLevels()

                assertEquals(configured, getLoggers().loggers.filter { it.configuredLevel != null })
            }

            @Test
            @DisplayName("the answer keeps the ownership flag, both sections being redrawn from it")
            fun `reset reports ownership`() {
                val loggers = resetLevels().loggers

                assertEquals(PUBLISHED_LOGGERS.map { it.value }, loggers.filter { it.application }.map { it.name })
            }

            @Test
            @DisplayName("the audit logger survives the reset, its level having been pinned before the snapshot")
            fun `reset leaves the audit logger alone`() {
                resetLevels()

                assertTrue(LoggerFactory.getLogger(AppLogger.AUDIT.value).isInfoEnabled)
            }
        }

        @Nested
        @DisplayName("class-level @AdminOnly guard")
        inner class AdminGuard {
            // The interceptor itself is proven by AdminAuthInterceptorIntegrationTest, and CodeLayoutTest holds the
            // annotation in place: what is left to show here is that a refused write changes nothing.
            @Test
            @DisplayName("non-admin caller - 403 with forbidden_admin, and the running logger stays untouched")
            fun `rejects a non admin`() {
                whenever(telegramValidatorService.map(any())).thenReturn(nonAdminUser)

                val response = putLevel(SANDBOX_LOGGER, """{"level": "OFF"}""")

                assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
                assertEquals(
                    AuthErrorType.FORBIDDEN_ADMIN.value,
                    response.headers.getFirst(AuthErrorType.HEADER_NAME),
                )
                assertNull(loggingSystem.getLoggerConfiguration(SANDBOX_LOGGER)?.configuredLevel)
            }
        }

        companion object {
            private const val ADMIN_EXTERNAL_ID = 1L
            private const val PLAIN_EXTERNAL_ID = 2L

            private const val SANDBOX_LOGGER = "test.sandbox.logger"
            private const val THIRD_PARTY_LOGGER = "org.hibernate.SQL"
            private const val UNKNOWN_LOGGER = "no.such.logger.here"
            private const val MALFORMED_LOGGER = "bad!name"

            private val PUBLISHED_LOGGERS = AppLogger.entries - AppLogger.AUDIT
        }
    }

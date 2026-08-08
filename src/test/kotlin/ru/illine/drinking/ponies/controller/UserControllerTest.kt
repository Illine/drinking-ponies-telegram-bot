package ru.illine.drinking.ponies.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.cache.CacheManager
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlConfig
import ru.illine.drinking.ponies.config.cache.CacheConfig
import ru.illine.drinking.ponies.config.web.security.AuthErrorType
import ru.illine.drinking.ponies.model.dto.response.MeResponse
import ru.illine.drinking.ponies.service.telegram.TelegramValidatorService
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest

@SpringIntegrationTest
@DisplayName("UserController Spring Integration Test")
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
class UserControllerTest
    @Autowired
    constructor(
        private val restTemplate: TestRestTemplate,
        private val objectMapper: ObjectMapper,
        private val cacheManager: CacheManager,
    ) {
        @MockitoBean
        private lateinit var telegramValidatorService: TelegramValidatorService

        private val telegramUser = DtoGenerator.generateTelegramUserDto(externalUserId = ADMIN_EXTERNAL_ID)

        @BeforeEach
        fun setUp() {
            cacheManager.getCache(CacheConfig.USER_ACCESS_FLAGS)?.clear()
            whenever(telegramValidatorService.verifySignature(any())).thenReturn(true)
            whenever(telegramValidatorService.map(any())).thenReturn(telegramUser)
        }

        private fun buildHeaders(): HttpHeaders =
            HttpHeaders().apply {
                set("X-Authorization-Telegram-Data", "test-init-data")
            }

        @ParameterizedTest(name = "[{index}] externalUserId={0} - isAdmin={1}, isBanned={2}, isActive={3}")
        @CsvSource(
            "1,      true,  false, true",
            "2,      false, false, true",
            "777002, false, true,  true",
            "777001, false, false, false",
            "777004, false, true,  false",
            "0,      false, false, true",
        )
        @DisplayName("getMe(): returns 200 with the access flags of the caller, unknown callers included")
        fun `returns 200 with access flags`(
            externalUserId: Long,
            isAdmin: Boolean,
            isBanned: Boolean,
            isActive: Boolean,
        ) {
            whenever(telegramValidatorService.map(any()))
                .thenReturn(telegramUser.copy(externalUserId = externalUserId))

            val response =
                restTemplate.exchange(
                    "/users/me",
                    HttpMethod.GET,
                    HttpEntity<Void>(buildHeaders()),
                    MeResponse::class.java,
                )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertNotNull(response.body)
            assertEquals(externalUserId, response.body!!.externalUserId)
            assertEquals(isAdmin, response.body!!.isAdmin)
            assertEquals(isBanned, response.body!!.isBanned)
            assertEquals(isActive, response.body!!.isActive)
        }

        @Test
        @DisplayName("getMe(): a soft-deleted caller still reaches the endpoint and learns they are inactive")
        fun `stays open for a soft deleted caller`() {
            whenever(telegramValidatorService.map(any()))
                .thenReturn(telegramUser.copy(externalUserId = DELETED_EXTERNAL_ID))

            val response =
                restTemplate.exchange(
                    "/users/me",
                    HttpMethod.GET,
                    HttpEntity<Void>(buildHeaders()),
                    MeResponse::class.java,
                )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertFalse(response.body!!.isActive)
        }

        @Test
        @DisplayName("getMe(): stays open for a non-admin, unlike the endpoints of UserAdminController")
        fun `stays open for a non admin`() {
            whenever(telegramValidatorService.map(any()))
                .thenReturn(telegramUser.copy(externalUserId = PLAIN_EXTERNAL_ID))

            val response =
                restTemplate.exchange(
                    "/users/me",
                    HttpMethod.GET,
                    HttpEntity<Void>(buildHeaders()),
                    MeResponse::class.java,
                )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertFalse(response.body!!.isAdmin)
        }

        @Test
        @DisplayName("getMe(): keeps the telegramUserId key the MiniApp consumes")
        fun `serializes the identity under the published keys`() {
            val response =
                restTemplate.exchange(
                    "/users/me",
                    HttpMethod.GET,
                    HttpEntity<Void>(buildHeaders()),
                    String::class.java,
                )

            val body = objectMapper.readTree(response.body)
            assertEquals(ADMIN_EXTERNAL_ID, body.path("telegramUserId").asLong())
            assertTrue(body.path("isAdmin").asBoolean())
            assertFalse(body.path("isBanned").asBoolean())
            assertTrue(body.path("isActive").asBoolean())
            assertFalse(body.path("isActive").isMissingNode, "The MiniApp reads the flag under this exact key")
        }

        @Test
        @DisplayName("getMe(): missing auth header - returns 401 with X-Auth-Error-Code invalid_auth_signature")
        fun `returns 401 with invalid_auth_signature header`() {
            val response =
                restTemplate.exchange(
                    "/users/me",
                    HttpMethod.GET,
                    HttpEntity<Void>(HttpHeaders()),
                    Void::class.java,
                )

            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
            assertEquals(
                AuthErrorType.INVALID_AUTH_SIGNATURE.value,
                response.headers.getFirst(AuthErrorType.HEADER_NAME),
            )
        }

        @Test
        @DisplayName("getMe(): expired auth_date - returns 403 with X-Auth-Error-Code session_expired")
        fun `returns 403 with session_expired header`() {
            whenever(telegramValidatorService.verifySignature(any())).thenReturn(false)

            val response =
                restTemplate.exchange(
                    "/users/me",
                    HttpMethod.GET,
                    HttpEntity<Void>(buildHeaders()),
                    Void::class.java,
                )

            assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
            assertEquals(
                AuthErrorType.SESSION_EXPIRED.value,
                response.headers.getFirst(AuthErrorType.HEADER_NAME),
            )
        }

        companion object {
            private const val ADMIN_EXTERNAL_ID = 1L
            private const val PLAIN_EXTERNAL_ID = 2L
            private const val DELETED_EXTERNAL_ID = 777001L
        }
    }

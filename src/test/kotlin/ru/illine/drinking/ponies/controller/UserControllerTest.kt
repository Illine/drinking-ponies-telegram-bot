package ru.illine.drinking.ponies.controller

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
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
import ru.illine.drinking.ponies.model.dto.TelegramUserDto
import ru.illine.drinking.ponies.model.dto.response.MeResponse
import ru.illine.drinking.ponies.service.telegram.TelegramValidatorService
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest

@SpringIntegrationTest
@DisplayName("UserController Spring Integration Test")
@Sql(
    scripts = ["classpath:sql/access/TelegramUserAccessService.sql"],
    config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
    scripts = ["classpath:sql/clear.sql"],
    config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class UserControllerTest @Autowired constructor(
    private val restTemplate: TestRestTemplate,
    private val cacheManager: CacheManager,
) {

    @MockitoBean
    private lateinit var telegramValidatorService: TelegramValidatorService

    private val ADMIN_USER_ID = 1L
    private val NON_ADMIN_USER_ID = 2L
    private val MISSING_USER_ID = 0L

    private val telegramUser = TelegramUserDto(
        telegramId = ADMIN_USER_ID,
        firstName = "First Name",
        lastName = null,
        username = "username"
    )

    @BeforeEach
    fun setUp() {
        cacheManager.getCache(CacheConfig.USER_IS_ADMIN)?.clear()
        `when`(telegramValidatorService.verifySignature(any())).thenReturn(true)
        `when`(telegramValidatorService.map(any())).thenReturn(telegramUser)
    }

    private fun buildHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Authorization-Telegram-Data", "test-init-data")
        }
    }

    @Nested
    @DisplayName("GET /users/me")
    inner class GetMe {

        @Test
        @DisplayName("admin user - returns 200 with isAdmin=true")
        fun `returns 200 with isAdmin true for admin`() {
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/users/me", HttpMethod.GET, HttpEntity<Void>(headers), MeResponse::class.java
            )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertNotNull(response.body)
            assertEquals(ADMIN_USER_ID, response.body!!.telegramUserId)
            assertEquals(true, response.body!!.isAdmin)
        }

        @Test
        @DisplayName("non-admin user - returns 200 with isAdmin=false")
        fun `returns 200 with isAdmin false for non-admin`() {
            `when`(telegramValidatorService.map(any())).thenReturn(telegramUser.copy(telegramId = NON_ADMIN_USER_ID))
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/users/me", HttpMethod.GET, HttpEntity<Void>(headers), MeResponse::class.java
            )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertNotNull(response.body)
            assertEquals(NON_ADMIN_USER_ID, response.body!!.telegramUserId)
            assertEquals(false, response.body!!.isAdmin)
        }

        @Test
        @DisplayName("user not in DB - returns 200 with isAdmin=false")
        fun `returns 200 with isAdmin false for user not in db`() {
            `when`(telegramValidatorService.map(any())).thenReturn(telegramUser.copy(telegramId = MISSING_USER_ID))
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/users/me", HttpMethod.GET, HttpEntity<Void>(headers), MeResponse::class.java
            )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertNotNull(response.body)
            assertEquals(MISSING_USER_ID, response.body!!.telegramUserId)
            assertEquals(false, response.body!!.isAdmin)
        }

        @Test
        @DisplayName("missing auth header - returns 401 with X-Auth-Error-Code invalid_auth_signature")
        fun `returns 401 with invalid_auth_signature header`() {
            val response = restTemplate.exchange(
                "/users/me", HttpMethod.GET, HttpEntity<Void>(HttpHeaders()), Void::class.java
            )

            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
            assertEquals(
                AuthErrorType.INVALID_AUTH_SIGNATURE.value,
                response.headers.getFirst(AuthErrorType.HEADER_NAME)
            )
        }

        @Test
        @DisplayName("expired auth_date - returns 403 with X-Auth-Error-Code session_expired")
        fun `returns 403 with session_expired header`() {
            `when`(telegramValidatorService.verifySignature(any())).thenReturn(false)
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/users/me", HttpMethod.GET, HttpEntity<Void>(headers), Void::class.java
            )

            assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
            assertEquals(
                AuthErrorType.SESSION_EXPIRED.value,
                response.headers.getFirst(AuthErrorType.HEADER_NAME)
            )
        }
    }
}

package ru.illine.drinking.ponies.config.web.interceptor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlConfig
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.illine.drinking.ponies.config.cache.CacheConfig
import ru.illine.drinking.ponies.config.web.interceptor.AdminAuthInterceptorIntegrationTest.AdminTestConfig
import ru.illine.drinking.ponies.config.web.security.AdminOnly
import ru.illine.drinking.ponies.config.web.security.AuthErrorType
import ru.illine.drinking.ponies.model.dto.TelegramUserDto
import ru.illine.drinking.ponies.service.telegram.TelegramValidatorService
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest

@SpringIntegrationTest
@DisplayName("AdminAuthInterceptor Spring Integration Test")
@Import(AdminTestConfig::class)
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
class AdminAuthInterceptorIntegrationTest @Autowired constructor(
    private val restTemplate: TestRestTemplate,
    private val cacheManager: CacheManager,
) {

    @MockitoBean
    private lateinit var telegramValidatorService: TelegramValidatorService

    private val ADMIN_USER_ID = 1L
    private val NON_ADMIN_USER_ID = 2L

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
    @DisplayName("GET /systems/admin-test")
    inner class AdminOnlyEndpoint {

        @Test
        @DisplayName("admin user - returns 200")
        fun `admin user returns 200`() {
            val response = restTemplate.exchange(
                "/systems/admin-test", HttpMethod.GET, HttpEntity<Void>(buildHeaders()), String::class.java
            )

            assertEquals(HttpStatus.OK, response.statusCode)
        }

        @Test
        @DisplayName("non-admin user - returns 403 with X-Auth-Error-Code forbidden_admin")
        fun `non-admin user returns 403 with forbidden_admin header`() {
            `when`(telegramValidatorService.map(any())).thenReturn(telegramUser.copy(telegramId = NON_ADMIN_USER_ID))

            val response = restTemplate.exchange(
                "/systems/admin-test", HttpMethod.GET, HttpEntity<Void>(buildHeaders()), Void::class.java
            )

            assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
            assertEquals(
                AuthErrorType.FORBIDDEN_ADMIN.value,
                response.headers.getFirst(AuthErrorType.HEADER_NAME)
            )
        }

        @Test
        @DisplayName("expired auth_date - returns 403 with X-Auth-Error-Code session_expired (auth runs first)")
        fun `expired auth_date returns 403 with session_expired header`() {
            `when`(telegramValidatorService.verifySignature(any())).thenReturn(false)

            val response = restTemplate.exchange(
                "/systems/admin-test", HttpMethod.GET, HttpEntity<Void>(buildHeaders()), Void::class.java
            )

            assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
            assertEquals(
                AuthErrorType.SESSION_EXPIRED.value,
                response.headers.getFirst(AuthErrorType.HEADER_NAME)
            )
            verify(telegramValidatorService, never()).map(any())
        }

        @Test
        @DisplayName("missing auth header - returns 401 with X-Auth-Error-Code invalid_auth_signature")
        fun `missing auth header returns 401 with invalid_auth_signature header`() {
            val response = restTemplate.exchange(
                "/systems/admin-test", HttpMethod.GET, HttpEntity<Void>(HttpHeaders()), Void::class.java
            )

            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
            assertEquals(
                AuthErrorType.INVALID_AUTH_SIGNATURE.value,
                response.headers.getFirst(AuthErrorType.HEADER_NAME)
            )
        }
    }

    @TestConfiguration
    class AdminTestConfig {

        @RestController
        @RequestMapping("/systems")
        class AdminTestController {

            @AdminOnly
            @GetMapping("/admin-test")
            fun adminTest(): String = "ok"
        }
    }
}

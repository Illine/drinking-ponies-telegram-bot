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
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoBean
import ru.illine.drinking.ponies.config.web.security.AuthErrorType
import ru.illine.drinking.ponies.model.dto.TelegramUserDto
import ru.illine.drinking.ponies.model.dto.response.VersionResponse
import ru.illine.drinking.ponies.service.telegram.TelegramValidatorService
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest

@SpringIntegrationTest
@DisplayName("SystemController Spring Integration Test")
class SystemControllerTest @Autowired constructor(
    private val restTemplate: TestRestTemplate
) {

    @MockitoBean
    private lateinit var telegramValidatorService: TelegramValidatorService

    private val telegramUser = TelegramUserDto(
        telegramId = 1L,
        firstName = "First Name",
        lastName = null,
        username = "username"
    )

    @BeforeEach
    fun setUp() {
        `when`(telegramValidatorService.verifySignature(any())).thenReturn(true)
        `when`(telegramValidatorService.map(any())).thenReturn(telegramUser)
    }

    private fun buildHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Authorization-Telegram-Data", "test-init-data")
        }
    }

    @Nested
    @DisplayName("GET /systems/version")
    inner class GetVersion {

        @Test
        @DisplayName("valid request - returns 200 with version from configuration")
        fun `returns 200 with version`() {
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/systems/version", HttpMethod.GET, HttpEntity<Void>(headers), VersionResponse::class.java
            )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertNotNull(response.body)
            assertEquals("X.Y.Z-test", response.body!!.version)
        }

        @Test
        @DisplayName("missing auth header - returns 401 with X-Auth-Error-Code invalid_auth_signature")
        fun `returns 401 with invalid_auth_signature header`() {
            val response = restTemplate.exchange(
                "/systems/version", HttpMethod.GET, HttpEntity<Void>(HttpHeaders()), Void::class.java
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
                "/systems/version", HttpMethod.GET, HttpEntity<Void>(headers), Void::class.java
            )

            assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
            assertEquals(
                AuthErrorType.SESSION_EXPIRED.value,
                response.headers.getFirst(AuthErrorType.HEADER_NAME)
            )
        }
    }
}

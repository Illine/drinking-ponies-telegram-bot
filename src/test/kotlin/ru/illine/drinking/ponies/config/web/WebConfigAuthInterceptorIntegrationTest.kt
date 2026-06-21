package ru.illine.drinking.ponies.config.web

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
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
import ru.illine.drinking.ponies.config.web.WebConfigAuthInterceptorIntegrationTest.DefaultSecureTestConfig
import ru.illine.drinking.ponies.config.web.security.AuthErrorType
import ru.illine.drinking.ponies.model.dto.TelegramUserDto
import ru.illine.drinking.ponies.service.telegram.TelegramValidatorService
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest

@SpringIntegrationTest
@DisplayName("WebConfig default-secure interceptor Spring Integration Test")
@Import(DefaultSecureTestConfig::class)
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
class WebConfigAuthInterceptorIntegrationTest
    @Autowired
    constructor(
        private val restTemplate: TestRestTemplate,
    ) {
        @MockitoBean
        private lateinit var telegramValidatorService: TelegramValidatorService

        private val telegramUser =
            TelegramUserDto(
                externalUserId = 1L,
                firstName = "First Name",
                lastName = null,
                username = "username",
            )

        @BeforeEach
        fun setUp() {
            whenever(telegramValidatorService.verifySignature(any())).thenReturn(true)
            whenever(telegramValidatorService.map(any())).thenReturn(telegramUser)
        }

        private fun buildHeaders(): HttpHeaders =
            HttpHeaders().apply {
                set("X-Authorization-Telegram-Data", "test-init-data")
            }

        @Nested
        @DisplayName("default-secure: endpoint outside any exclude pattern is protected by default")
        inner class DefaultSecureEndpoint {
            private val url = "/test-default-secure"

            @Test
            @DisplayName("missing auth header - returns 401 with X-Auth-Error-Code invalid_auth_signature")
            fun `missing auth header returns 401 with invalid_auth_signature header`() {
                val response =
                    restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        HttpEntity<Void>(HttpHeaders()),
                        Void::class.java,
                    )

                assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
                assertEquals(
                    AuthErrorType.INVALID_AUTH_SIGNATURE.value,
                    response.headers.getFirst(AuthErrorType.HEADER_NAME),
                )
                verify(telegramValidatorService, never()).verifySignature(any())
                verify(telegramValidatorService, never()).map(any())
            }

            @Test
            @DisplayName("valid auth header - returns 200")
            fun `valid auth header returns 200`() {
                val response =
                    restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        HttpEntity<Void>(buildHeaders()),
                        String::class.java,
                    )

                assertEquals(HttpStatus.OK, response.statusCode)
                verify(telegramValidatorService).verifySignature("test-init-data")
                verify(telegramValidatorService).map("test-init-data")
            }
        }

        @Nested
        @DisplayName("excluded public path: /v3/api-docs is reachable without auth header")
        inner class ExcludedPublicPath {
            @Test
            @DisplayName("GET /v3/api-docs without auth header - returns 200 and is not blocked by auth")
            fun `api docs without auth header returns 200`() {
                val response =
                    restTemplate.exchange(
                        "/v3/api-docs",
                        HttpMethod.GET,
                        HttpEntity<Void>(HttpHeaders()),
                        String::class.java,
                    )

                assertEquals(HttpStatus.OK, response.statusCode)
                assertNotEquals(
                    AuthErrorType.INVALID_AUTH_SIGNATURE.value,
                    response.headers.getFirst(AuthErrorType.HEADER_NAME),
                )
                verify(telegramValidatorService, never()).verifySignature(any())
            }
        }

        @TestConfiguration
        class DefaultSecureTestConfig {
            @RestController
            @RequestMapping("/test-default-secure")
            class DefaultSecureTestController {
                @GetMapping
                fun defaultSecure(): String = "ok"
            }
        }
    }

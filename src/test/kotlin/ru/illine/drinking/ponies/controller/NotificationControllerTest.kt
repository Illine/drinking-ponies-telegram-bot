package ru.illine.drinking.ponies.controller

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoBean
import ru.illine.drinking.ponies.model.dto.TelegramUserDto
import ru.illine.drinking.ponies.model.dto.response.NotificationNextResponse
import ru.illine.drinking.ponies.service.notification.NotificationSettingsService
import ru.illine.drinking.ponies.service.telegram.TelegramValidatorService
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest
import org.mockito.Mockito.`when`
import java.time.Instant

@SpringIntegrationTest
@DisplayName("NotificationController Spring Integration Test")
class NotificationControllerTest @Autowired constructor(
    private val restTemplate: TestRestTemplate
) {

    @MockitoBean
    private lateinit var telegramValidatorService: TelegramValidatorService

    @MockitoBean
    private lateinit var notificationSettingsService: NotificationSettingsService

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
    @DisplayName("GET /notifications/next")
    inner class GetNextNotification {

        @Test
        @DisplayName("valid request - returns 200 with next notification time")
        fun `returns 200 with next notification time`() {
            val expectedInstant = Instant.parse("2025-01-01T14:00:00Z")
            `when`(notificationSettingsService.getNextNotificationAt(any())).thenReturn(expectedInstant)
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/notifications/next", HttpMethod.GET, HttpEntity<Void>(headers), NotificationNextResponse::class.java
            )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals("2025-01-01T14:00:00Z", response.body!!.nextNotificationAt)
            verify(notificationSettingsService).getNextNotificationAt(any())
        }

        @Test
        @DisplayName("service throws IllegalArgumentException - returns 400")
        fun `returns 400 when service throws`() {
            doThrow(IllegalArgumentException("User not found"))
                .`when`(notificationSettingsService).getNextNotificationAt(any())
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/notifications/next", HttpMethod.GET, HttpEntity<Void>(headers), Void::class.java
            )

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }

        @Test
        @DisplayName("missing auth header - returns 401")
        fun `returns 401`() {
            val response = restTemplate.exchange(
                "/notifications/next", HttpMethod.GET, HttpEntity<Void>(HttpHeaders()), Void::class.java
            )

            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
            verifyNoInteractions(notificationSettingsService)
        }
    }
}
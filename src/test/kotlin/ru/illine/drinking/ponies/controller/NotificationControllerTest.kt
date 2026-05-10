package ru.illine.drinking.ponies.controller

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoBean
import ru.illine.drinking.ponies.exception.NotificationSettingsNotFoundException
import ru.illine.drinking.ponies.model.dto.TelegramUserDto
import ru.illine.drinking.ponies.model.dto.response.NotificationNextResponse
import ru.illine.drinking.ponies.model.dto.response.PauseStateResponse
import ru.illine.drinking.ponies.service.notification.NotificationSettingsService
import ru.illine.drinking.ponies.service.telegram.TelegramValidatorService
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest
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
            assertNotNull(response.body)
            assertEquals(expectedInstant, response.body!!.nextNotificationAt)
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

        @Test
        @DisplayName("settings not found (e.g. disabled user) - returns 404")
        fun `returns 404 when settings not found`() {
            doThrow(NotificationSettingsNotFoundException("Not found"))
                .`when`(notificationSettingsService).getNextNotificationAt(any())
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/notifications/next", HttpMethod.GET, HttpEntity<Void>(headers), Void::class.java
            )

            assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
            verify(notificationSettingsService).getNextNotificationAt(any())
        }
    }

    @Nested
    @DisplayName("GET /notifications/pause")
    inner class GetPauseState {

        @Test
        @DisplayName("paused user - returns 200 with paused=true and pauseUntil")
        fun `returns 200 with paused true`() {
            val expectedPauseUntil = Instant.parse("2025-01-01T18:00:00Z")
            val expected = PauseStateResponse(paused = true, pauseUntil = expectedPauseUntil)
            `when`(notificationSettingsService.getPauseState(any())).thenReturn(expected)
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/notifications/pause", HttpMethod.GET, HttpEntity<Void>(headers), PauseStateResponse::class.java
            )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertNotNull(response.body)
            assertEquals(true, response.body!!.paused)
            assertEquals(expectedPauseUntil, response.body!!.pauseUntil)
            verify(notificationSettingsService).getPauseState(any())
        }

        @Test
        @DisplayName("not paused - returns 200 with paused=false and null pauseUntil")
        fun `returns 200 with paused false`() {
            val expected = PauseStateResponse(paused = false, pauseUntil = null)
            `when`(notificationSettingsService.getPauseState(any())).thenReturn(expected)
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/notifications/pause", HttpMethod.GET, HttpEntity<Void>(headers), PauseStateResponse::class.java
            )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertNotNull(response.body)
            assertEquals(false, response.body!!.paused)
            assertEquals(null, response.body!!.pauseUntil)
        }

        @Test
        @DisplayName("missing auth header - returns 401")
        fun `returns 401`() {
            val response = restTemplate.exchange(
                "/notifications/pause", HttpMethod.GET, HttpEntity<Void>(HttpHeaders()), Void::class.java
            )

            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
            verifyNoInteractions(notificationSettingsService)
        }

        @Test
        @DisplayName("settings not found (e.g. disabled user) - returns 404")
        fun `returns 404 when settings not found`() {
            doThrow(NotificationSettingsNotFoundException("Not found"))
                .`when`(notificationSettingsService).getPauseState(any())
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/notifications/pause", HttpMethod.GET, HttpEntity<Void>(headers), Void::class.java
            )

            assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        }
    }

    @Nested
    @DisplayName("PUT /notifications/pause")
    inner class ChangePause {

        @Test
        @DisplayName("minutes=60 - returns 204 and calls pauseNotifications")
        fun `returns 204 on pause`() {
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/notifications/pause?minutes=60", HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java
            )

            assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
            verify(notificationSettingsService).pauseNotifications(telegramUser.telegramId, 60)
            verify(notificationSettingsService, never()).cancelPause(anyLong())
        }

        @Test
        @DisplayName("minutes=0 - returns 204 and calls cancelPause")
        fun `returns 204 on cancel`() {
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/notifications/pause?minutes=0", HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java
            )

            assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
            verify(notificationSettingsService).cancelPause(telegramUser.telegramId)
            verify(notificationSettingsService, never()).pauseNotifications(anyLong(), anyLong())
        }

        @ParameterizedTest(name = "[{index}] minutes={0} - returns 400 from @Min/@Max validation")
        @ValueSource(longs = [-1, 301])
        @DisplayName("minutes out of [0, 300] range - returns 400 from validation")
        fun `returns 400 on out-of-range minutes`(minutes: Long) {
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/notifications/pause?minutes=$minutes", HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java
            )

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            verifyNoInteractions(notificationSettingsService)
        }

        @Test
        @DisplayName("missing minutes param - returns 400")
        fun `returns 400 on missing minutes`() {
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/notifications/pause", HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java
            )

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            verifyNoInteractions(notificationSettingsService)
        }

        @Test
        @DisplayName("invalid minutes type - returns 400")
        fun `returns 400 on invalid minutes type`() {
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/notifications/pause?minutes=abc", HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java
            )

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            verifyNoInteractions(notificationSettingsService)
        }

        @Test
        @DisplayName("missing auth header - returns 401")
        fun `returns 401`() {
            val response = restTemplate.exchange(
                "/notifications/pause?minutes=60", HttpMethod.PUT, HttpEntity<Void>(HttpHeaders()), Void::class.java
            )

            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
            verifyNoInteractions(notificationSettingsService)
        }

        @Test
        @DisplayName("settings not found (e.g. disabled user) - returns 404")
        fun `returns 404 when settings not found`() {
            doThrow(NotificationSettingsNotFoundException("Not found"))
                .`when`(notificationSettingsService).pauseNotifications(anyLong(), anyLong())
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/notifications/pause?minutes=60", HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java
            )

            assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        }
    }
}

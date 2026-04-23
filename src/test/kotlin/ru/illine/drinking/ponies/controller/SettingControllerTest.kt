package ru.illine.drinking.ponies.controller

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoBean
import ru.illine.drinking.ponies.model.base.IntervalNotificationType
import ru.illine.drinking.ponies.model.dto.TelegramUserDto
import ru.illine.drinking.ponies.model.dto.response.IntervalResponse
import ru.illine.drinking.ponies.model.dto.response.NotificationStatusResponse
import ru.illine.drinking.ponies.model.dto.response.QuietModeResponse
import ru.illine.drinking.ponies.model.dto.response.TimezoneResponse
import ru.illine.drinking.ponies.service.notification.NotificationSettingsService
import ru.illine.drinking.ponies.service.telegram.TelegramValidatorService
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest
import java.time.LocalTime

@SpringIntegrationTest
@DisplayName("SettingController Spring Integration Test")
class SettingControllerTest @Autowired constructor(
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
        `when`(telegramValidatorService.verifySignature(any(), any())).thenReturn(true)
        `when`(telegramValidatorService.map(any())).thenReturn(telegramUser)
    }

    private fun buildHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Authorization-Telegram-Data", "test-init-data")
        }
    }

    @Nested
    @DisplayName("GET /settings/quiet-mode")
    inner class GetQuietMode {

        @Test
        @DisplayName("valid request - returns 200 with quiet mode in HH:mm format")
        fun `returns 200 with quiet mode`() {
            val expectedStart = LocalTime.of(23, 0)
            val expectedEnd = LocalTime.of(8, 0)
            `when`(notificationSettingsService.getQuietMode(any())).thenReturn(expectedStart to expectedEnd)
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/settings/quiet-mode", HttpMethod.GET, HttpEntity<Void>(headers), QuietModeResponse::class.java
            )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals("23:00", response.body!!.start)
            assertEquals("08:00", response.body!!.end)
            verify(notificationSettingsService).getQuietMode(any())
        }

        @Test
        @DisplayName("single-digit hours - returns HH:mm with leading zero")
        fun `returns HH-mm with leading zero`() {
            `when`(notificationSettingsService.getQuietMode(any()))
                .thenReturn(LocalTime.of(1, 30) to LocalTime.of(7, 5))
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/settings/quiet-mode", HttpMethod.GET, HttpEntity<Void>(headers), QuietModeResponse::class.java
            )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals("01:30", response.body!!.start)
            assertEquals("07:05", response.body!!.end)
        }

        @Test
        @DisplayName("missing auth header - returns 401")
        fun `returns 401`() {
            val response = restTemplate.exchange(
                "/settings/quiet-mode", HttpMethod.GET, HttpEntity<Void>(HttpHeaders()), Void::class.java
            )

            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
            verifyNoInteractions(notificationSettingsService)
        }
    }

    @Nested
    @DisplayName("PUT /settings/quiet-mode")
    inner class ChangeQuietMode {

        @Test
        @DisplayName("valid request - returns 200")
        fun `returns 200`() {
            val headers = buildHeaders()
            val url = "/settings/quiet-mode?start=08:00&end=22:00"

            val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java)

            assertEquals(HttpStatus.OK, response.statusCode)
            verify(notificationSettingsService).changeQuietMode(any(), any(), any())
        }

        @Test
        @DisplayName("missing header - returns 401")
        fun `returns 401`() {
            val url = "/settings/quiet-mode?start=08:00&end=22:00"

            val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(HttpHeaders()), Void::class.java)

            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
            verifyNoInteractions(notificationSettingsService)
        }

        @Test
        @DisplayName("missing required param - returns 400")
        fun `returns 400`() {
            val headers = buildHeaders()
            val url = "/settings/quiet-mode?end=22:00"

            val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java)

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }

        @Test
        @DisplayName("missing end param - returns 400")
        fun `returns 400 when missing end param`() {
            val headers = buildHeaders()
            val url = "/settings/quiet-mode?start=08:00"

            val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java)

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }

        @Test
        @DisplayName("invalid time format - returns 400")
        fun `returns 400 when invalid time format`() {
            val headers = buildHeaders()
            val url = "/settings/quiet-mode?start=invalid&end=22:00"

            val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java)

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }
    }

    @Nested
    @DisplayName("GET /settings/interval")
    inner class GetInterval {

        @Test
        @DisplayName("valid request - returns 200 with interval data")
        fun `returns 200 with interval`() {
            val expectedInterval = IntervalNotificationType.HOUR_AND_HALF
            val settingDto = DtoGenerator.generateNotificationDto(notificationInterval = expectedInterval)
            `when`(notificationSettingsService.getNotificationSettings(any())).thenReturn(settingDto)
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/settings/interval", HttpMethod.GET, HttpEntity<Void>(headers), IntervalResponse::class.java
            )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(expectedInterval.name, response.body!!.interval)
            assertEquals(expectedInterval.displayName, response.body!!.displayName)
            assertEquals(expectedInterval.minutes, response.body!!.minutes)
            verify(notificationSettingsService).getNotificationSettings(any())
        }

        @Test
        @DisplayName("missing auth header - returns 401")
        fun `returns 401`() {
            val response = restTemplate.exchange(
                "/settings/interval", HttpMethod.GET, HttpEntity<Void>(HttpHeaders()), Void::class.java
            )

            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
            verifyNoInteractions(notificationSettingsService)
        }
    }

    @Nested
    @DisplayName("PUT /settings/interval")
    inner class ChangeInterval {

        @Test
        @DisplayName("valid request - returns 200")
        fun `returns 200`() {
            val headers = buildHeaders()
            val url = "/settings/interval?interval=${IntervalNotificationType.HOUR.name}"

            val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java)

            assertEquals(HttpStatus.OK, response.statusCode)
            verify(notificationSettingsService).changeInterval(any<Long>(), any<IntervalNotificationType>())
        }

        @Test
        @DisplayName("missing auth header - returns 401")
        fun `returns 401`() {
            val url = "/settings/interval?interval=${IntervalNotificationType.HOUR.name}"

            val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(HttpHeaders()), Void::class.java)

            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
            verifyNoInteractions(notificationSettingsService)
        }

        @Test
        @DisplayName("missing interval param - returns 400")
        fun `returns 400 when missing param`() {
            val headers = buildHeaders()
            val url = "/settings/interval"

            val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java)

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }

        @Test
        @DisplayName("invalid interval value - returns 400")
        fun `returns 400 when invalid value`() {
            val headers = buildHeaders()
            val url = "/settings/interval?interval=INVALID_VALUE"

            val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java)

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }
    }

    @Nested
    @DisplayName("GET /settings/timezone")
    inner class GetTimezone {

        @Test
        @DisplayName("valid request - returns 200 with timezone")
        fun `returns 200 with timezone`() {
            val expectedTimezone = "America/New_York"
            val settingDto = DtoGenerator.generateNotificationDto(userTimeZone = expectedTimezone)
            `when`(notificationSettingsService.getNotificationSettings(any())).thenReturn(settingDto)
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/settings/timezone", HttpMethod.GET, HttpEntity<Void>(headers), TimezoneResponse::class.java
            )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(expectedTimezone, response.body!!.timezone)
            verify(notificationSettingsService).getNotificationSettings(any())
        }

        @Test
        @DisplayName("missing auth header - returns 401")
        fun `returns 401`() {
            val response = restTemplate.exchange(
                "/settings/timezone", HttpMethod.GET, HttpEntity<Void>(HttpHeaders()), Void::class.java
            )

            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
            verifyNoInteractions(notificationSettingsService)
        }
    }

    @Nested
    @DisplayName("GET /settings/notification-status")
    inner class GetNotificationStatus {

        @Test
        @DisplayName("notifications enabled - returns 200 with active=true")
        fun `returns active true when enabled`() {
            `when`(notificationSettingsService.isEnabledNotifications(any())).thenReturn(true)
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/settings/notification-status", HttpMethod.GET, HttpEntity<Void>(headers), NotificationStatusResponse::class.java
            )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(true, response.body!!.active)
            verify(notificationSettingsService).isEnabledNotifications(any())
        }

        @Test
        @DisplayName("notifications disabled - returns 200 with active=false")
        fun `returns active false when disabled`() {
            `when`(notificationSettingsService.isEnabledNotifications(any())).thenReturn(false)
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/settings/notification-status", HttpMethod.GET, HttpEntity<Void>(headers), NotificationStatusResponse::class.java
            )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(false, response.body!!.active)
            verify(notificationSettingsService).isEnabledNotifications(any())
        }

        @Test
        @DisplayName("missing auth header - returns 401")
        fun `returns 401`() {
            val response = restTemplate.exchange(
                "/settings/notification-status", HttpMethod.GET, HttpEntity<Void>(HttpHeaders()), Void::class.java
            )

            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
            verifyNoInteractions(notificationSettingsService)
        }
    }

    @Nested
    @DisplayName("PUT /settings/notification-status")
    inner class ChangeNotificationStatus {

        @Test
        @DisplayName("valid request - returns 200")
        fun `returns 200`() {
            val headers = buildHeaders()
            val url = "/settings/notification-status?active=false"

            val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java)

            assertEquals(HttpStatus.OK, response.statusCode)
            verify(notificationSettingsService).changeNotificationStatus(any(), any())
        }

        @Test
        @DisplayName("missing auth header - returns 401")
        fun `returns 401`() {
            val url = "/settings/notification-status?active=false"

            val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(HttpHeaders()), Void::class.java)

            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
            verifyNoInteractions(notificationSettingsService)
        }

        @Test
        @DisplayName("missing active param - returns 400")
        fun `returns 400`() {
            val headers = buildHeaders()
            val url = "/settings/notification-status"

            val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java)

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }

        @Test
        @DisplayName("invalid active value - returns 400")
        fun `returns 400 when invalid active value`() {
            val headers = buildHeaders()
            val url = "/settings/notification-status?active=INVALID"

            val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java)

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }
    }
}

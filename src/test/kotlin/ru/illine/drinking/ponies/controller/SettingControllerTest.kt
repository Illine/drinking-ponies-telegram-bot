package ru.illine.drinking.ponies.controller

import org.junit.jupiter.api.Assertions.assertEquals
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
import ru.illine.drinking.ponies.model.base.IntervalNotificationType
import ru.illine.drinking.ponies.model.dto.SettingDto
import ru.illine.drinking.ponies.model.dto.TelegramUserDto
import ru.illine.drinking.ponies.model.dto.response.SettingResponse
import ru.illine.drinking.ponies.service.notification.NotificationSettingsService
import ru.illine.drinking.ponies.service.telegram.TelegramValidatorService
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest

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
        `when`(telegramValidatorService.verifySignature(any())).thenReturn(true)
        `when`(telegramValidatorService.map(any())).thenReturn(telegramUser)
    }

    private fun buildHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Authorization-Telegram-Data", "test-init-data")
        }
    }

    @Nested
    @DisplayName("GET /settings")
    inner class GetSettings {

        @Test
        @DisplayName("valid request - returns 200 with all settings")
        fun `returns 200 with all settings`() {
            val expectedInterval = IntervalNotificationType.HOUR_AND_HALF
            val settingDto = SettingDto(
                interval = expectedInterval.name,
                intervalDisplayName = expectedInterval.displayName,
                intervalMinutes = expectedInterval.minutes,
                quietModeStart = "23:00",
                quietModeEnd = "08:00",
                timezone = "America/New_York",
                dailyGoalMl = 2500,
                notificationActive = true,
            )
            `when`(notificationSettingsService.getAllSettings(any())).thenReturn(settingDto)
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/settings", HttpMethod.GET, HttpEntity<Void>(headers), SettingResponse::class.java
            )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(expectedInterval.name, response.body!!.interval)
            assertEquals(expectedInterval.displayName, response.body!!.intervalDisplayName)
            assertEquals(expectedInterval.minutes, response.body!!.intervalMinutes)
            assertEquals("23:00", response.body!!.quietModeStart)
            assertEquals("08:00", response.body!!.quietModeEnd)
            assertEquals("America/New_York", response.body!!.timezone)
            assertEquals(2500, response.body!!.dailyGoalMl)
            assertEquals(true, response.body!!.notificationActive)
        }

        @Test
        @DisplayName("notifications disabled - returns 200 with notificationActive=false")
        fun `returns 200 with only notificationActive when disabled`() {
            val settingDto = SettingDto(notificationActive = false)
            `when`(notificationSettingsService.getAllSettings(any())).thenReturn(settingDto)
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/settings", HttpMethod.GET, HttpEntity<Void>(headers), SettingResponse::class.java
            )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(false, response.body!!.notificationActive)
            assertEquals(null, response.body!!.interval)
            assertEquals(null, response.body!!.intervalDisplayName)
            assertEquals(null, response.body!!.intervalMinutes)
            assertEquals(null, response.body!!.quietModeStart)
            assertEquals(null, response.body!!.quietModeEnd)
            assertEquals(null, response.body!!.timezone)
            assertEquals(null, response.body!!.dailyGoalMl)
        }

        @Test
        @DisplayName("missing auth header - returns 401")
        fun `returns 401`() {
            val response = restTemplate.exchange(
                "/settings", HttpMethod.GET, HttpEntity<Void>(HttpHeaders()), Void::class.java
            )

            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
            verifyNoInteractions(notificationSettingsService)
        }
    }

    @Nested
    @DisplayName("PUT /settings/quiet-mode")
    inner class ChangeQuietMode {

        @Test
        @DisplayName("valid request - returns 204")
        fun `returns 204`() {
            val headers = buildHeaders()
            val url = "/settings/quiet-mode?start=08:00&end=22:00"

            val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java)

            assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
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

        @ParameterizedTest(name = "[{index}] query={0} - returns 400")
        @ValueSource(
            strings = [
                "?end=22:00",                  // missing start param
                "?start=08:00",                // missing end param
                "?start=invalid&end=22:00",    // invalid time format
            ]
        )
        @DisplayName("invalid request - returns 400")
        fun `returns 400 on invalid request`(queryString: String) {
            val headers = buildHeaders()
            val url = "/settings/quiet-mode$queryString"

            val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java)

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            verifyNoInteractions(notificationSettingsService)
        }
    }

    @Nested
    @DisplayName("PUT /settings/interval")
    inner class ChangeInterval {

        @Test
        @DisplayName("valid request - returns 204")
        fun `returns 204`() {
            val headers = buildHeaders()
            val url = "/settings/interval?interval=${IntervalNotificationType.HOUR.name}"

            val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java)

            assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
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
    @DisplayName("PUT /settings/timezone")
    inner class ChangeTimezone {

        @Test
        @DisplayName("valid request - returns 204")
        fun `returns 204`() {
            val headers = buildHeaders()
            val url = "/settings/timezone?timezone=Europe/Berlin"

            val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java)

            assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
            verify(notificationSettingsService).changeTimezone(any(), any())
        }

        @Test
        @DisplayName("missing auth header - returns 401")
        fun `returns 401`() {
            val url = "/settings/timezone?timezone=Europe/Berlin"

            val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(HttpHeaders()), Void::class.java)

            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
            verifyNoInteractions(notificationSettingsService)
        }

        @Test
        @DisplayName("missing timezone param - returns 400")
        fun `returns 400 when missing param`() {
            val headers = buildHeaders()
            val url = "/settings/timezone"

            val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java)

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }

        @Test
        @DisplayName("invalid timezone value - returns 400")
        fun `returns 400 when invalid timezone value`() {
            doThrow(IllegalArgumentException("Invalid timezone"))
                .`when`(notificationSettingsService).changeTimezone(any(), any())
            val headers = buildHeaders()
            val url = "/settings/timezone?timezone=Invalid/Zone"

            val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java)

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }
    }

    @Nested
    @DisplayName("PUT /settings/notification-status")
    inner class ChangeNotificationStatus {

        @ParameterizedTest(name = "[{index}] active={0} - returns 204")
        @ValueSource(booleans = [true, false])
        @DisplayName("valid request - returns 204")
        fun `returns 204 on valid request`(active: Boolean) {
            val headers = buildHeaders()
            val url = "/settings/notification-status?active=$active"

            val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java)

            assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
            verify(notificationSettingsService).changeNotificationStatus(telegramUser.telegramId, active)
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

    @Nested
    @DisplayName("PUT /settings/goal")
    inner class ChangeDailyGoal {

        @Test
        @DisplayName("valid request goalMl=2500 - returns 204 and delegates to service")
        fun `returns 204 on valid goal`() {
            val headers = buildHeaders()
            val url = "/settings/goal?goalMl=2500"

            val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java)

            assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
            verify(notificationSettingsService).changeDailyGoal(telegramUser.telegramId, 2500)
        }

        @Test
        @DisplayName("missing auth header - returns 401")
        fun `returns 401`() {
            val url = "/settings/goal?goalMl=2500"

            val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(HttpHeaders()), Void::class.java)

            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
            verifyNoInteractions(notificationSettingsService)
        }


        @Test
        @DisplayName("missing goalMl param - returns 400")
        fun `returns 400 on missing param`() {
            val headers = buildHeaders()
            val url = "/settings/goal"

            val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java)

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            verifyNoInteractions(notificationSettingsService)
        }

        @Test
        @DisplayName("invalid goalMl type - returns 400")
        fun `returns 400 on invalid goal type`() {
            val headers = buildHeaders()
            val url = "/settings/goal?goalMl=abc"

            val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java)

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            verifyNoInteractions(notificationSettingsService)
        }

        @ParameterizedTest(name = "[{index}] goalMl={0} - service rejects, returns 400")
        @ValueSource(ints = [1999, 3001, 2100])
        @DisplayName("service rejects out-of-range / non-allowed goalMl - returns 400")
        fun `returns 400 when service rejects goalMl`(goalMl: Int) {
            doThrow(IllegalArgumentException("Daily goal must be one of [2000, 2250, 2500, 2750, 3000] ml, got: $goalMl"))
                .`when`(notificationSettingsService).changeDailyGoal(any(), any())
            val headers = buildHeaders()
            val url = "/settings/goal?goalMl=$goalMl"

            val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java)

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }
    }
}

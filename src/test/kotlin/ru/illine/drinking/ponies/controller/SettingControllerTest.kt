package ru.illine.drinking.ponies.controller

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
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
import ru.illine.drinking.ponies.model.dto.TelegramUserDto
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
        `when`(telegramValidatorService.verifySignature(any(), any())).thenReturn(true)
        `when`(telegramValidatorService.map(any())).thenReturn(telegramUser)
    }

    @Test
    @DisplayName("PUT /settings/modes/silent: valid request - returns 200")
    fun `changeSilentMode returns 200`() {
        val headers = buildHeaders()
        val url = "/settings/modes/silent?messageId=1&start=08:00&end=22:00"

        val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)
        verify(notificationSettingsService).changeQuietMode(any(), any(), any(), any())
    }

    @Test
    @DisplayName("PUT /settings/modes/silent: missing header - returns 401")
    fun `changeSilentMode returns 401`() {
        val url = "/settings/modes/silent?messageId=1&start=08:00&end=22:00"

        val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(HttpHeaders()), Void::class.java)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        verifyNoInteractions(notificationSettingsService)
    }

    @Test
    @DisplayName("PUT /settings/modes/silent: missing required param - returns 400")
    fun `changeSilentMode returns 400`() {
        val headers = buildHeaders()
        val url = "/settings/modes/silent?start=08:00&end=22:00"

        val response = restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(headers), Void::class.java)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    private fun buildHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Authorization-Telegram-Data", "test-init-data")
        }
    }
}

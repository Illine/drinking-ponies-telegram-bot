package ru.illine.drinking.ponies.controller

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.dto.TelegramUserDto
import ru.illine.drinking.ponies.model.dto.response.StatisticsTodayResponse
import ru.illine.drinking.ponies.service.statistic.StatisticsService
import ru.illine.drinking.ponies.service.telegram.TelegramValidatorService
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest
import java.time.Instant
import java.time.LocalDateTime

@SpringIntegrationTest
@DisplayName("StatisticsController Spring Integration Test")
class StatisticsControllerTest @Autowired constructor(
    private val restTemplate: TestRestTemplate
) {

    @MockitoBean
    private lateinit var telegramValidatorService: TelegramValidatorService

    @MockitoBean
    private lateinit var statisticsService: StatisticsService

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
    @DisplayName("GET /statistics/today")
    inner class GetToday {

        @Test
        @DisplayName("valid request - returns 200 with entries serialized as ISO 8601 UTC")
        fun `returns 200 with serialized entries`() {
            val entries = listOf(
                DtoGenerator.generateWaterStatisticDto(
                    externalUserId = telegramUser.telegramId,
                    eventTime = LocalDateTime.of(2026, 5, 7, 8, 15, 0),
                    eventType = AnswerNotificationType.YES,
                    waterAmountMl = 250,
                ),
                DtoGenerator.generateWaterStatisticDto(
                    externalUserId = telegramUser.telegramId,
                    eventTime = LocalDateTime.of(2026, 5, 7, 13, 0, 0),
                    eventType = AnswerNotificationType.YES,
                    waterAmountMl = 500,
                ),
            )
            `when`(statisticsService.getToday(telegramUser.telegramId)).thenReturn(entries)
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/statistics/today", HttpMethod.GET, HttpEntity<Void>(headers), StatisticsTodayResponse::class.java
            )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertNotNull(response.body)
            val body = response.body!!
            assertEquals(2, body.entries.size)
            assertEquals(Instant.parse("2026-05-07T08:15:00Z"), body.entries[0].eventTime)
            assertEquals(250, body.entries[0].amountMl)
            assertEquals(Instant.parse("2026-05-07T13:00:00Z"), body.entries[1].eventTime)
            assertEquals(500, body.entries[1].amountMl)
            verify(statisticsService).getToday(telegramUser.telegramId)
        }

        @Test
        @DisplayName("empty list - returns 200 with empty entries")
        fun `returns 200 with empty entries`() {
            `when`(statisticsService.getToday(telegramUser.telegramId)).thenReturn(emptyList())
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/statistics/today", HttpMethod.GET, HttpEntity<Void>(headers), StatisticsTodayResponse::class.java
            )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertNotNull(response.body)
            assertEquals(0, response.body!!.entries.size)
        }

        @Test
        @DisplayName("missing auth header - returns 401")
        fun `returns 401`() {
            val response = restTemplate.exchange(
                "/statistics/today", HttpMethod.GET, HttpEntity<Void>(HttpHeaders()), Void::class.java
            )

            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
            verifyNoInteractions(statisticsService)
        }
    }
}

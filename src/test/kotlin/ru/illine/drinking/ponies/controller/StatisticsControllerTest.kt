package ru.illine.drinking.ponies.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.nullableArgumentCaptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import ru.illine.drinking.ponies.exception.NotificationSettingsNotFoundException
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.StatisticsPeriodType
import ru.illine.drinking.ponies.model.dto.BestDayDto
import ru.illine.drinking.ponies.model.dto.StatisticsDto
import ru.illine.drinking.ponies.model.dto.StatisticsPointDto
import ru.illine.drinking.ponies.model.dto.TelegramUserDto
import ru.illine.drinking.ponies.model.dto.request.WaterEntryRequest
import ru.illine.drinking.ponies.model.dto.response.StatisticsResponse
import ru.illine.drinking.ponies.model.dto.response.StatisticsTodayResponse
import ru.illine.drinking.ponies.service.statistic.StatisticsService
import ru.illine.drinking.ponies.service.statistic.WaterStatisticService
import ru.illine.drinking.ponies.service.telegram.TelegramValidatorService
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

@SpringIntegrationTest
@DisplayName("StatisticsController Spring Integration Test")
class StatisticsControllerTest @Autowired constructor(
    private val restTemplate: TestRestTemplate,
    private val objectMapper: ObjectMapper
) {

    @MockitoBean
    private lateinit var telegramValidatorService: TelegramValidatorService

    @MockitoBean
    private lateinit var statisticsService: StatisticsService

    @MockitoBean
    private lateinit var waterStatisticService: WaterStatisticService

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

    @Nested
    @DisplayName("GET /statistics")
    inner class GetStatistics {

        private fun fakeDto(period: StatisticsPeriodType): StatisticsDto = when (period) {
            StatisticsPeriodType.DAY -> StatisticsDto(
                points = (0 until 24).map { StatisticsPointDto("%02d:00".format(it), if (it == 8) 250 else 0) },
                dailyGoalMl = 2000,
                averageMlPerDay = 250,
                bestDay = null,
                currentStreakDays = 3,
                insightText = "insight text day",
            )
            StatisticsPeriodType.WEEK -> StatisticsDto(
                points = listOf(
                    StatisticsPointDto("2026-05-04", 1800),
                    StatisticsPointDto("2026-05-05", 2100),
                    StatisticsPointDto("2026-05-06", 2400),
                    StatisticsPointDto("2026-05-07", 1500),
                    StatisticsPointDto("2026-05-08", 0),
                    StatisticsPointDto("2026-05-09", 0),
                    StatisticsPointDto("2026-05-10", 0),
                ),
                dailyGoalMl = 2000,
                averageMlPerDay = 1114,
                bestDay = BestDayDto(LocalDate.of(2026, 5, 6), 2400, DayOfWeek.WEDNESDAY),
                currentStreakDays = 3,
                insightText = "insight text week",
            )
            StatisticsPeriodType.MONTH -> StatisticsDto(
                points = (1..31).map { StatisticsPointDto("2026-05-%02d".format(it), 0) },
                dailyGoalMl = 2000,
                averageMlPerDay = 0,
                bestDay = null,
                currentStreakDays = 0,
                insightText = "insight text month",
            )
        }

        @ParameterizedTest(name = "[{index}] period={0} - returns 200")
        @EnumSource(StatisticsPeriodType::class)
        @DisplayName("valid request - returns 200 for each period")
        fun `returns 200 for each period`(period: StatisticsPeriodType) {
            val dto = fakeDto(period)
            `when`(statisticsService.getStatistics(telegramUser.telegramId, period)).thenReturn(dto)
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/statistics?period=$period", HttpMethod.GET, HttpEntity<Void>(headers), StatisticsResponse::class.java
            )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertNotNull(response.body)
            assertEquals(dto.points.size, response.body!!.points.size)
            assertEquals(dto.dailyGoalMl, response.body!!.dailyGoalMl)
            assertEquals(dto.averageMlPerDay, response.body!!.averageMlPerDay)
            assertEquals(dto.currentStreakDays, response.body!!.currentStreakDays)
            assertEquals(dto.insightText, response.body!!.insight.text)
            verify(statisticsService).getStatistics(telegramUser.telegramId, period)
        }

        @Test
        @DisplayName("WEEK response includes bestDay with date, valueMl, weekday")
        fun `WEEK response carries bestDay fields`() {
            val dto = fakeDto(StatisticsPeriodType.WEEK)
            `when`(statisticsService.getStatistics(telegramUser.telegramId, StatisticsPeriodType.WEEK))
                .thenReturn(dto)
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/statistics?period=WEEK", HttpMethod.GET, HttpEntity<Void>(headers), StatisticsResponse::class.java
            )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertNotNull(response.body!!.bestDay)
            assertEquals(LocalDate.of(2026, 5, 6), response.body!!.bestDay!!.date)
            assertEquals(2400, response.body!!.bestDay!!.valueMl)
            assertEquals(DayOfWeek.WEDNESDAY, response.body!!.bestDay!!.weekday)
        }

        @ParameterizedTest(name = "[{index}] period={0} - returns 400 (invalid enum)")
        @ValueSource(strings = ["YEAR", "year", "INVALID"])
        @DisplayName("invalid period enum value - returns 400")
        fun `returns 400 on invalid period enum`(period: String) {
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/statistics?period=$period", HttpMethod.GET, HttpEntity<Void>(headers), Void::class.java
            )

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            verifyNoInteractions(statisticsService)
        }

        @Test
        @DisplayName("empty period value - returns 400")
        fun `returns 400 on empty period`() {
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/statistics?period=", HttpMethod.GET, HttpEntity<Void>(headers), Void::class.java
            )

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            verifyNoInteractions(statisticsService)
        }

        @Test
        @DisplayName("missing period param - returns 400")
        fun `returns 400 on missing period`() {
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/statistics", HttpMethod.GET, HttpEntity<Void>(headers), Void::class.java
            )

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            verifyNoInteractions(statisticsService)
        }

        @Test
        @DisplayName("missing auth header - returns 401")
        fun `returns 401`() {
            val response = restTemplate.exchange(
                "/statistics?period=DAY", HttpMethod.GET, HttpEntity<Void>(HttpHeaders()), Void::class.java
            )

            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
            verifyNoInteractions(statisticsService)
        }

        @Test
        @DisplayName("notifications disabled (NotificationSettingsNotFoundException) - returns 404")
        fun `returns 404 when settings not found`() {
            doThrow(NotificationSettingsNotFoundException("not found"))
                .`when`(statisticsService).getStatistics(any(), any())
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/statistics?period=WEEK", HttpMethod.GET, HttpEntity<Void>(headers), Void::class.java
            )

            assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
            verify(statisticsService).getStatistics(telegramUser.telegramId, StatisticsPeriodType.WEEK)
        }
    }

    @Nested
    @DisplayName("POST /statistics/water")
    inner class PostWater {

        private fun jsonHeaders(): HttpHeaders {
            return buildHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
        }

        /**
         * Returns an instant that is safely in the past relative to the server clock.
         * Margin avoids flakiness against @PastOrPresent caused by clock drift between
         * test thread and validator invocation.
         */
        private fun pastInstant(): Instant = Instant.now().minus(Duration.ofMinutes(5))

        @Test
        @DisplayName("valid request - returns 201 and calls service with parsed args")
        fun `returns 201 on valid request`() {
            val consumedAt = pastInstant()
            val body = objectMapper.writeValueAsString(WaterEntryRequest(consumedAt, 250))

            val response = restTemplate.exchange(
                "/statistics/water", HttpMethod.POST, HttpEntity(body, jsonHeaders()), Void::class.java
            )

            assertEquals(HttpStatus.CREATED, response.statusCode)
            val userIdCaptor = argumentCaptor<Long>()
            val consumedAtCaptor = argumentCaptor<Instant>()
            val amountCaptor = argumentCaptor<Int>()
            verify(waterStatisticService).manualRecordEvent(
                userIdCaptor.capture(), consumedAtCaptor.capture(), amountCaptor.capture()
            )
            assertEquals(telegramUser.telegramId, userIdCaptor.firstValue)
            assertEquals(consumedAt, consumedAtCaptor.firstValue)
            assertEquals(250, amountCaptor.firstValue)
        }

        @Test
        @DisplayName("valid request - returns 201 and calls service with parsed args")
        fun `returns 201 on valid request on null consumerAt`() {
            val body = objectMapper.writeValueAsString(WaterEntryRequest(null, 250))

            val response = restTemplate.exchange(
                "/statistics/water", HttpMethod.POST, HttpEntity(body, jsonHeaders()), Void::class.java
            )

            assertEquals(HttpStatus.CREATED, response.statusCode)
            val userIdCaptor = argumentCaptor<Long>()
            val consumedAtCaptor = nullableArgumentCaptor<Instant>()
            val amountCaptor = argumentCaptor<Int>()
            verify(waterStatisticService).manualRecordEvent(
                userIdCaptor.capture(), consumedAtCaptor.capture(), amountCaptor.capture()
            )
            assertEquals(telegramUser.telegramId, userIdCaptor.firstValue)
            assertEquals(250, amountCaptor.firstValue)
            assertNull(consumedAtCaptor.firstValue)
        }

        @Test
        @DisplayName("service throws IllegalArgumentException - returns 400 (handler maps to BAD_REQUEST)")
        fun `returns 400 on service IllegalArgumentException`() {
            doThrow(IllegalArgumentException("invalid"))
                .`when`(waterStatisticService).manualRecordEvent(any(), any(), any())
            val body = objectMapper.writeValueAsString(WaterEntryRequest(pastInstant(), 250))

            val response = restTemplate.exchange(
                "/statistics/water", HttpMethod.POST, HttpEntity(body, jsonHeaders()), Void::class.java
            )

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }

        @ParameterizedTest(name = "[{index}] amountMl={0} fails bean validation")
        @ValueSource(ints = [Int.MIN_VALUE, -1, 0, 49, 1001, Int.MAX_VALUE])
        @DisplayName("amountMl outside [MIN_ML, MAX_ML] - returns 400 (bean validation)")
        fun `returns 400 on amount out of bounds`(amount: Int) {
            val body = objectMapper.writeValueAsString(WaterEntryRequest(pastInstant(), amount))

            val response = restTemplate.exchange(
                "/statistics/water", HttpMethod.POST, HttpEntity(body, jsonHeaders()), Void::class.java
            )

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            verifyNoInteractions(waterStatisticService)
        }

        @Test
        @DisplayName("consumedAt in the future - returns 400 (bean validation)")
        fun `returns 400 on future consumedAt`() {
            val body = objectMapper.writeValueAsString(
                WaterEntryRequest(Instant.now().plus(Duration.ofHours(1)), 250)
            )

            val response = restTemplate.exchange(
                "/statistics/water", HttpMethod.POST, HttpEntity(body, jsonHeaders()), Void::class.java
            )

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            verifyNoInteractions(waterStatisticService)
        }

        @ParameterizedTest(name = "[{index}] body={0}")
        @MethodSource("ru.illine.drinking.ponies.controller.StatisticsControllerTest#malformedBodies")
        @DisplayName("malformed/missing JSON body fields - returns 400")
        fun `returns 400 on malformed body`(body: String?) {
            val entity: HttpEntity<*> = if (body == null) HttpEntity<Void>(jsonHeaders()) else HttpEntity(body, jsonHeaders())

            val response = restTemplate.exchange(
                "/statistics/water", HttpMethod.POST, entity, Void::class.java
            )

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            verifyNoInteractions(waterStatisticService)
        }

        @Test
        @DisplayName("missing auth header - returns 401")
        fun `returns 401 on missing auth header`() {
            val body = objectMapper.writeValueAsString(WaterEntryRequest(pastInstant(), 250))
            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }

            val response = restTemplate.exchange(
                "/statistics/water", HttpMethod.POST, HttpEntity(body, headers), Void::class.java
            )

            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
            verifyNoInteractions(waterStatisticService)
        }

        @Test
        @DisplayName("invalid signature - returns 403")
        fun `returns 403 on invalid signature`() {
            `when`(telegramValidatorService.verifySignature(any())).thenReturn(false)
            val body = objectMapper.writeValueAsString(WaterEntryRequest(pastInstant(), 250))

            val response = restTemplate.exchange(
                "/statistics/water", HttpMethod.POST, HttpEntity(body, jsonHeaders()), Void::class.java
            )

            assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
            verifyNoInteractions(waterStatisticService)
        }
    }

    companion object {
        @JvmStatic
        fun malformedBodies(): List<String?> {
            val pastIso = Instant.now().minus(Duration.ofMinutes(5)).toString()
            return listOf(
                null,
                """{"consumedAt":"$pastIso"}""",
                """{"consumedAt":"not-a-date","amountMl":250}""",
                """{}""",
            )
        }
    }
}

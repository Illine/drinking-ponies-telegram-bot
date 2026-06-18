package ru.illine.drinking.ponies.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.nullableArgumentCaptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.*
import org.springframework.test.context.bean.override.mockito.MockitoBean
import ru.illine.drinking.ponies.exception.NotificationSettingsNotFoundException
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.dto.StatisticsPointDto
import ru.illine.drinking.ponies.model.dto.response.StatisticsResponse
import ru.illine.drinking.ponies.model.dto.response.StatisticsTodayResponse
import ru.illine.drinking.ponies.service.statistic.StatisticsService
import ru.illine.drinking.ponies.service.statistic.WaterStatisticService
import ru.illine.drinking.ponies.service.telegram.TelegramValidatorService
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest
import java.time.*

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

    private val telegramUser = DtoGenerator.generateTelegramUserDto()

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
                    externalUserId = telegramUser.externalUserId,
                    eventTime = LocalDateTime.of(2026, 5, 7, 8, 15, 0),
                    eventType = AnswerNotificationType.YES,
                    waterAmountMl = 250,
                ),
                DtoGenerator.generateWaterStatisticDto(
                    externalUserId = telegramUser.externalUserId,
                    eventTime = LocalDateTime.of(2026, 5, 7, 13, 0, 0),
                    eventType = AnswerNotificationType.YES,
                    waterAmountMl = 500,
                ),
            )
            `when`(statisticsService.getToday(telegramUser.externalUserId)).thenReturn(entries)
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
            verify(statisticsService).getToday(telegramUser.externalUserId)
        }

        @Test
        @DisplayName("empty list - returns 200 with empty entries")
        fun `returns 200 with empty entries`() {
            `when`(statisticsService.getToday(telegramUser.externalUserId)).thenReturn(emptyList())
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

        private val from = LocalDate.of(2026, 5, 4)
        private val to = LocalDate.of(2026, 5, 10)

        private fun dailyDto(firstEntryAt: Instant? = Instant.parse("2026-04-15T10:30:00Z")) =
            DtoGenerator.generateStatisticsDto(firstEntryAt = firstEntryAt)

        private fun hourlyDto() = DtoGenerator.generateStatisticsDto(
            points = (0 until 24).map { StatisticsPointDto("%02d:00".format(it), if (it == 8) 250 else 0) },
            averageMlPerDay = 250,
            bestDay = null,
            currentStreakDays = 1,
            insightText = "today insight",
        )

        @Test
        @DisplayName("valid range - returns 200 with full body and forwards from/to to service")
        fun `valid range returns 200`() {
            val dto = dailyDto()
            `when`(statisticsService.getStatistics(eq(telegramUser.externalUserId), eq(from), eq(to))).thenReturn(dto)
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/statistics?from=$from&to=$to",
                HttpMethod.GET, HttpEntity<Void>(headers), StatisticsResponse::class.java
            )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertNotNull(response.body)
            val body = response.body!!
            assertEquals(dto.points.size, body.points.size)
            assertEquals(dto.dailyGoalMl, body.dailyGoalMl)
            assertEquals(dto.averageMlPerDay, body.averageMlPerDay)
            assertEquals(dto.currentStreakDays, body.currentStreakDays)
            assertEquals(dto.insightText, body.insight.text)
            val bestDay = body.bestDay!!
            assertEquals(LocalDate.of(2026, 5, 6), bestDay.date)
            assertEquals(2400, bestDay.valueMl)
            assertEquals(DayOfWeek.WEDNESDAY, bestDay.weekday)
            assertEquals(Instant.parse("2026-04-15T10:30:00Z"), body.firstEntryAt)
            val fromCaptor = argumentCaptor<LocalDate>()
            val toCaptor = argumentCaptor<LocalDate>()
            verify(statisticsService).getStatistics(eq(telegramUser.externalUserId), fromCaptor.capture(), toCaptor.capture())
            assertEquals(from, fromCaptor.firstValue)
            assertEquals(to, toCaptor.firstValue)
        }

        @Test
        @DisplayName("from == to - returns 200 with 24 hourly buckets")
        fun `from equals to returns hourly`() {
            val day = LocalDate.of(2026, 5, 12)
            `when`(statisticsService.getStatistics(eq(telegramUser.externalUserId), eq(day), eq(day))).thenReturn(hourlyDto())
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/statistics?from=$day&to=$day",
                HttpMethod.GET, HttpEntity<Void>(headers), StatisticsResponse::class.java
            )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(24, response.body!!.points.size)
            assertEquals("00:00", response.body!!.points.first().label)
            assertEquals("23:00", response.body!!.points.last().label)
        }

        @Test
        @DisplayName("firstEntryAt = null is rendered as null in response")
        fun `null firstEntryAt rendered as null`() {
            `when`(statisticsService.getStatistics(eq(telegramUser.externalUserId), eq(from), eq(to)))
                .thenReturn(dailyDto(firstEntryAt = null))
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/statistics?from=$from&to=$to",
                HttpMethod.GET, HttpEntity<Void>(headers), StatisticsResponse::class.java
            )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertNull(response.body!!.firstEntryAt)
        }

        @Test
        @DisplayName("service throws IllegalArgumentException (e.g. from > to in user TZ) - returns 400")
        fun `service IllegalArgumentException returns 400`() {
            `when`(statisticsService.getStatistics(any(), any(), any()))
                .thenThrow(IllegalArgumentException("Invalid parameter: 'from' must be before or equal to 'to'"))
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/statistics?from=2026-05-10&to=2026-05-09",
                HttpMethod.GET, HttpEntity<Void>(headers), Void::class.java
            )

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }

        @ParameterizedTest(name = "[{index}] from={0} - returns 400 (malformed date)")
        @ValueSource(strings = ["foobar", "2026-13-01", "2026-05-32", ""])
        @DisplayName("malformed from - returns 400")
        fun `malformed from returns 400`(from: String) {
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/statistics?from=$from&to=2026-05-10",
                HttpMethod.GET, HttpEntity<Void>(headers), Void::class.java
            )

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            verifyNoInteractions(statisticsService)
        }

        @Test
        @DisplayName("missing from param - returns 400")
        fun `missing from returns 400`() {
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/statistics?to=2026-05-10", HttpMethod.GET, HttpEntity<Void>(headers), Void::class.java
            )

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            verifyNoInteractions(statisticsService)
        }

        @Test
        @DisplayName("missing to param - returns 400")
        fun `missing to returns 400`() {
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/statistics?from=2026-05-10", HttpMethod.GET, HttpEntity<Void>(headers), Void::class.java
            )

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            verifyNoInteractions(statisticsService)
        }

        @Test
        @DisplayName("missing both params - returns 400")
        fun `missing both returns 400`() {
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
                "/statistics?from=2026-05-04&to=2026-05-10",
                HttpMethod.GET, HttpEntity<Void>(HttpHeaders()), Void::class.java
            )

            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
            verifyNoInteractions(statisticsService)
        }

        @Test
        @DisplayName("notifications disabled (NotificationSettingsNotFoundException) - returns 404")
        fun `returns 404 when settings not found`() {
            doThrow(NotificationSettingsNotFoundException("not found"))
                .`when`(statisticsService).getStatistics(any(), any(), any())
            val headers = buildHeaders()

            val response = restTemplate.exchange(
                "/statistics?from=$from&to=$to",
                HttpMethod.GET, HttpEntity<Void>(headers), Void::class.java
            )

            assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
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

        private fun pastInstant(): Instant = Instant.now().minus(Duration.ofMinutes(5))

        @Test
        @DisplayName("valid request - returns 201 and calls service with parsed args")
        fun `returns 201 on valid request`() {
            val consumedAt = pastInstant()
            val body = objectMapper.writeValueAsString(DtoGenerator.generateWaterEntryRequest(consumedAt, 250))

            val response = restTemplate.exchange(
                "/statistics/water", HttpMethod.POST, HttpEntity(body, jsonHeaders()), Void::class.java
            )

            assertEquals(HttpStatus.CREATED, response.statusCode)
            val externalUserIdCaptor = argumentCaptor<Long>()
            val consumedAtCaptor = argumentCaptor<Instant>()
            val amountCaptor = argumentCaptor<Int>()
            verify(waterStatisticService).manualRecordEvent(
                externalUserIdCaptor.capture(), consumedAtCaptor.capture(), amountCaptor.capture()
            )
            assertEquals(telegramUser.externalUserId, externalUserIdCaptor.firstValue)
            assertEquals(consumedAt, consumedAtCaptor.firstValue)
            assertEquals(250, amountCaptor.firstValue)
        }

        @Test
        @DisplayName("valid request - returns 201 and calls service with parsed args")
        fun `returns 201 on valid request on null consumerAt`() {
            val body = objectMapper.writeValueAsString(DtoGenerator.generateWaterEntryRequest(consumedAt = null, amountMl = 250))

            val response = restTemplate.exchange(
                "/statistics/water", HttpMethod.POST, HttpEntity(body, jsonHeaders()), Void::class.java
            )

            assertEquals(HttpStatus.CREATED, response.statusCode)
            val externalUserIdCaptor = argumentCaptor<Long>()
            val consumedAtCaptor = nullableArgumentCaptor<Instant>()
            val amountCaptor = argumentCaptor<Int>()
            verify(waterStatisticService).manualRecordEvent(
                externalUserIdCaptor.capture(), consumedAtCaptor.capture(), amountCaptor.capture()
            )
            assertEquals(telegramUser.externalUserId, externalUserIdCaptor.firstValue)
            assertEquals(250, amountCaptor.firstValue)
            assertNull(consumedAtCaptor.firstValue)
        }

        @Test
        @DisplayName("service throws IllegalArgumentException - returns 400 (handler maps to BAD_REQUEST)")
        fun `returns 400 on service IllegalArgumentException`() {
            doThrow(IllegalArgumentException("invalid"))
                .`when`(waterStatisticService).manualRecordEvent(any(), any(), any())
            val body = objectMapper.writeValueAsString(DtoGenerator.generateWaterEntryRequest(pastInstant(), 250))

            val response = restTemplate.exchange(
                "/statistics/water", HttpMethod.POST, HttpEntity(body, jsonHeaders()), Void::class.java
            )

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }

        @ParameterizedTest(name = "[{index}] amountMl={0} fails bean validation")
        @ValueSource(ints = [Int.MIN_VALUE, -1, 0, 49, 1001, Int.MAX_VALUE])
        @DisplayName("amountMl outside [MIN_ML, MAX_ML] - returns 400 (bean validation)")
        fun `returns 400 on amount out of bounds`(amount: Int) {
            val body = objectMapper.writeValueAsString(DtoGenerator.generateWaterEntryRequest(pastInstant(), amount))

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
                DtoGenerator.generateWaterEntryRequest(
                    consumedAt = Instant.now().plus(Duration.ofHours(1)),
                    amountMl = 250,
                )
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
            val body = objectMapper.writeValueAsString(DtoGenerator.generateWaterEntryRequest(pastInstant(), 250))
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
            val body = objectMapper.writeValueAsString(DtoGenerator.generateWaterEntryRequest(pastInstant(), 250))

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

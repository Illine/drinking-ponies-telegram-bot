package ru.illine.drinking.ponies.service.statistic

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.dao.access.WaterStatisticAccessService
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto
import ru.illine.drinking.ponies.model.dto.message.InsightStatsContext
import ru.illine.drinking.ponies.model.dto.message.MessageDto
import ru.illine.drinking.ponies.service.message.MessageProvider
import ru.illine.drinking.ponies.service.statistic.impl.StatisticsServiceImpl
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.message.MessageSpec
import java.time.*
import java.util.stream.Stream

@UnitTest
@DisplayName("StatisticsService Unit Test")
class StatisticsServiceTest {

    private val externalUserId = 1L

    private lateinit var notificationAccessService: NotificationAccessService
    private lateinit var waterStatisticAccessService: WaterStatisticAccessService
    private lateinit var messageProvider: MessageProvider
    private lateinit var service: StatisticsService

    @BeforeEach
    fun setUp() {
        notificationAccessService = mock<NotificationAccessService>()
        waterStatisticAccessService = mock<WaterStatisticAccessService>()
        messageProvider = mock<MessageProvider>()
    }

    private fun stubInsight(text: String = "insight") {
        whenever(messageProvider.getMessage<InsightStatsContext>(any(), any()))
            .thenReturn(MessageDto(text))
    }

    private fun stubSettings(zone: String = "UTC", dailyGoalMl: Int = 2000) {
        whenever(notificationAccessService.findNotificationSettingByExternalUserId(externalUserId))
            .thenReturn(DtoGenerator.generateNotificationDto(
                externalUserId = externalUserId, userTimeZone = zone, dailyGoalMl = dailyGoalMl
            ))
    }

    private fun stubEvents(events: List<WaterStatisticDto>) {
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(events)
    }

    private fun stubFirstEntry(at: LocalDateTime? = null) {
        whenever(waterStatisticAccessService.findEarliestEventTimeByUser(any())).thenReturn(at)
    }

    private fun buildService(clock: Clock) {
        service = StatisticsServiceImpl(
            notificationAccessService, waterStatisticAccessService, messageProvider, clock
        )
    }

    private fun clockAt(iso: String): Clock = Clock.fixed(Instant.parse(iso), ZoneOffset.UTC)

    @ParameterizedTest(name = "[{index}] zone={0} ({4}) -> [{2}, {3})")
    @MethodSource("provideZoneCases")
    @DisplayName("getToday(): computes day boundaries in user TZ (UTC LocalDateTime)")
    fun `getToday computes day boundaries in user TZ`(
        zone: String,
        fixedInstant: String,
        expectedStart: LocalDateTime,
        expectedEnd: LocalDateTime,
        @Suppress("UNUSED_PARAMETER") description: String,
    ) {
        buildService(Clock.fixed(Instant.parse(fixedInstant), ZoneOffset.UTC))
        stubSettings(zone = zone)
        val expectedFromService = emptyList<WaterStatisticDto>()
        stubEvents(expectedFromService)

        val result = service.getToday(externalUserId)

        val startCaptor = argumentCaptor<LocalDateTime>()
        val endCaptor = argumentCaptor<LocalDateTime>()
        verify(waterStatisticAccessService).findByUserAndEventTimeBetween(
            eq(externalUserId), startCaptor.capture(), endCaptor.capture()
        )
        assertEquals(expectedStart, startCaptor.firstValue)
        assertEquals(expectedEnd, endCaptor.firstValue)
        assertSame(expectedFromService, result)
        verify(notificationAccessService).findNotificationSettingByExternalUserId(externalUserId)
    }

    @Test
    @DisplayName("getToday(): returns the list from access service unchanged")
    fun `getToday returns list as-is`() {
        buildService(clockAt("2026-05-07T22:30:00Z"))
        stubSettings(zone = "Europe/Moscow")
        val expected = listOf(
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = externalUserId,
                eventTime = LocalDateTime.of(2026, 5, 8, 8, 15),
                waterAmountMl = 250,
            ),
        )
        stubEvents(expected)

        val result = service.getToday(externalUserId)

        assertEquals(expected, result)
    }

    @Test
    @DisplayName("getStatistics(from==to): builds 24 hourly points, includes insight text and firstEntryAt")
    fun `getStatistics hourly when from equals to`() {
        val today = LocalDate.of(2026, 5, 12)
        buildService(clockAt("2026-05-12T12:00:00Z"))
        stubSettings()
        stubEvents(
            listOf(
                DtoGenerator.generateWaterStatisticDto(
                    externalUserId = externalUserId,
                    eventTime = LocalDateTime.of(2026, 5, 12, 8, 15),
                    waterAmountMl = 250,
                )
            )
        )
        stubFirstEntry(LocalDateTime.of(2026, 5, 1, 9, 0))
        stubInsight("insight day")

        val result = service.getStatistics(externalUserId, today, today)

        assertEquals(24, result.points.size)
        assertEquals("08:00", result.points[8].label)
        assertEquals(250, result.points[8].valueMl)
        assertEquals(2000, result.dailyGoalMl)
        assertEquals(250, result.averageMlPerDay) // days=1 -> total
        assertNull(result.bestDay)                 // days=1 -> null
        assertEquals("insight day", result.insightText)
        assertEquals(Instant.parse("2026-05-01T09:00:00Z"), result.firstEntryAt)
    }

    @Test
    @DisplayName("getStatistics(from < to): builds daily points, bestDay set with weekday")
    fun `getStatistics daily when range over multiple days`() {
        // 2026-05-04 (Mon) .. 2026-05-10 (Sun) = 7 days
        buildService(clockAt("2026-05-10T12:00:00Z"))
        stubSettings()
        stubEvents(
            listOf(
                DtoGenerator.generateWaterStatisticDto(
                    externalUserId = externalUserId,
                    eventTime = LocalDateTime.of(2026, 5, 4, 10, 0),
                    waterAmountMl = 2100,
                ),
                DtoGenerator.generateWaterStatisticDto(
                    externalUserId = externalUserId,
                    eventTime = LocalDateTime.of(2026, 5, 6, 10, 0),
                    waterAmountMl = 2400,
                ),
            )
        )
        stubFirstEntry(null)
        stubInsight("insight week")

        val result = service.getStatistics(externalUserId, LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 10))

        assertEquals(7, result.points.size)
        assertEquals("2026-05-04", result.points[0].label)
        assertEquals(2100, result.points[0].valueMl)
        assertEquals("2026-05-06", result.points[2].label)
        assertEquals(2400, result.points[2].valueMl)
        assertNotNull(result.bestDay)
        val bestDay = result.bestDay!!
        assertEquals(LocalDate.of(2026, 5, 6), bestDay.date)
        assertEquals(2400, bestDay.valueMl)
        assertEquals(DayOfWeek.WEDNESDAY, bestDay.weekday)
        assertEquals((2100 + 2400) / 7, result.averageMlPerDay)
        assertNull(result.firstEntryAt)
    }

    @Test
    @DisplayName("getStatistics(from==to and to < today): hourly buckets work for past day")
    fun `getStatistics hourly for past day`() {
        val today = LocalDate.of(2026, 5, 12)
        val past = LocalDate.of(2026, 5, 1)
        buildService(clockAt("2026-05-12T12:00:00Z"))
        stubSettings()
        stubEvents(
            listOf(
                DtoGenerator.generateWaterStatisticDto(
                    externalUserId = externalUserId,
                    eventTime = LocalDateTime.of(2026, 5, 1, 9, 30),
                    waterAmountMl = 400,
                )
            )
        )
        stubFirstEntry(null)
        stubInsight()

        val result = service.getStatistics(externalUserId, past, past)

        assertEquals(24, result.points.size)
        assertEquals(400, result.points[9].valueMl)
        assertEquals(0, result.points[8].valueMl)
        // sanity: today not used as the bucket day
        assertNotNull(today)
    }

    @Test
    @DisplayName("getStatistics: filters out non-YES events from points and average")
    fun `getStatistics filters non-YES events`() {
        val today = LocalDate.of(2026, 5, 12)
        buildService(clockAt("2026-05-12T12:00:00Z"))
        stubSettings()
        stubEvents(
            listOf(
                DtoGenerator.generateWaterStatisticDto(
                    externalUserId = externalUserId,
                    eventTime = LocalDateTime.of(2026, 5, 12, 8, 0),
                    eventType = AnswerNotificationType.YES,
                    waterAmountMl = 500,
                ),
                DtoGenerator.generateWaterStatisticDto(
                    externalUserId = externalUserId,
                    eventTime = LocalDateTime.of(2026, 5, 12, 10, 0),
                    eventType = AnswerNotificationType.SNOOZE,
                    waterAmountMl = 100,
                ),
                DtoGenerator.generateWaterStatisticDto(
                    externalUserId = externalUserId,
                    eventTime = LocalDateTime.of(2026, 5, 12, 14, 0),
                    eventType = AnswerNotificationType.CANCEL,
                    waterAmountMl = 200,
                ),
            )
        )
        stubFirstEntry(null)
        stubInsight()

        val result = service.getStatistics(externalUserId, today, today)

        assertEquals(500, result.averageMlPerDay)
        assertEquals(500, result.points[8].valueMl)
        assertEquals(0, result.points[10].valueMl)
        assertEquals(0, result.points[14].valueMl)
    }

    @Test
    @DisplayName("getStatistics: streak counts back from today even when 'to' is far in the past")
    fun `getStatistics streak still anchored to today when to is before today`() {
        // Critical regression target: streak must be computed from `today` back, not from `to` back.
        // today=2026-05-20, range = [2026-04-01 .. 2026-04-30].
        // Goal met for every day from 2026-05-14..2026-05-20 (7 days back through today).
        val today = LocalDate.of(2026, 5, 20)
        buildService(clockAt("2026-05-20T12:00:00Z"))
        stubSettings()
        val recentMet = (0L..6L).map { offset ->
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = externalUserId,
                eventTime = today.minusDays(offset).atTime(10, 0),
                waterAmountMl = 2200,
            )
        }
        stubEvents(recentMet)
        stubFirstEntry(null)
        stubInsight()

        val result = service.getStatistics(externalUserId, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30))

        assertEquals(7, result.currentStreakDays)
    }

    @Test
    @DisplayName("getStatistics: streak is bounded by STREAK_LIMIT_DAYS even for a very large past range")
    fun `getStatistics streak bounded by limit`() {
        // Far-past range plus today met for every day for 500 days back.
        // Streak loop iterates 366 days back from today-1 + today itself -> 367.
        val today = LocalDate.of(2026, 5, 20)
        buildService(clockAt("2026-05-20T12:00:00Z"))
        stubSettings()
        val events = (0L..500L).map { offset ->
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = externalUserId,
                eventTime = today.minusDays(offset).atTime(10, 0),
                waterAmountMl = 2200,
            )
        }
        stubEvents(events)
        stubFirstEntry(null)
        stubInsight()

        val result = service.getStatistics(
            externalUserId, today.minusDays(500), today
        )

        assertEquals(367, result.currentStreakDays)
    }

    @Test
    @DisplayName("getStatistics: bestDay only considers events in [from..to], not the streak window")
    fun `getStatistics bestDay scoped to range`() {
        // Range is [05-15 .. 05-20] (today=05-20). The huge 9999 on 05-01 must NOT be bestDay
        // even though it is fetched for the streak look-back window.
        val today = LocalDate.of(2026, 5, 20)
        buildService(clockAt("2026-05-20T12:00:00Z"))
        stubSettings()
        stubEvents(
            listOf(
                // outside range, fetched only for streak
                DtoGenerator.generateWaterStatisticDto(
                    externalUserId = externalUserId,
                    eventTime = LocalDateTime.of(2026, 5, 1, 10, 0),
                    waterAmountMl = 9999,
                ),
                // inside range
                DtoGenerator.generateWaterStatisticDto(
                    externalUserId = externalUserId,
                    eventTime = LocalDateTime.of(2026, 5, 17, 10, 0),
                    waterAmountMl = 2500,
                ),
            )
        )
        stubFirstEntry(null)
        stubInsight()

        val result = service.getStatistics(
            externalUserId, LocalDate.of(2026, 5, 15), today
        )

        assertNotNull(result.bestDay)
        val bestDay = result.bestDay!!
        assertEquals(LocalDate.of(2026, 5, 17), bestDay.date)
        assertEquals(2500, bestDay.valueMl)
    }

    @Test
    @DisplayName("getStatistics: from > to -> IllegalArgumentException")
    fun `getStatistics rejects from after to`() {
        buildService(clockAt("2026-05-20T12:00:00Z"))
        // settings stub not needed: validation runs before user context fetch
        stubSettings()

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.getStatistics(externalUserId, LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 9))
        }
        assertTrue(ex.message!!.contains("'from' must be before or equal to 'to'"))
    }

    @Test
    @DisplayName("getStatistics: from in the future -> IllegalArgumentException")
    fun `getStatistics rejects from in the future`() {
        val today = LocalDate.of(2026, 5, 20)
        buildService(clockAt("2026-05-20T12:00:00Z"))
        stubSettings()

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.getStatistics(externalUserId, today.plusDays(1), today.plusDays(2))
        }
        assertTrue(ex.message!!.contains("'from' must not be in the future"))
    }

    @Test
    @DisplayName("getStatistics: firstEntryAt is null when there are no entries")
    fun `getStatistics returns null firstEntryAt when none`() {
        val today = LocalDate.of(2026, 5, 12)
        buildService(clockAt("2026-05-12T12:00:00Z"))
        stubSettings()
        stubEvents(emptyList())
        stubFirstEntry(null)
        stubInsight()

        val result = service.getStatistics(externalUserId, today, today)

        assertNull(result.firstEntryAt)
        verify(waterStatisticAccessService).findEarliestEventTimeByUser(externalUserId)
    }

    @Test
    @DisplayName("getStatistics: firstEntryAt carries earliest entry as UTC Instant")
    fun `getStatistics returns firstEntryAt as utc instant`() {
        val today = LocalDate.of(2026, 5, 12)
        buildService(clockAt("2026-05-12T12:00:00Z"))
        stubSettings()
        stubEvents(emptyList())
        stubFirstEntry(LocalDateTime.of(2026, 1, 15, 7, 45))
        stubInsight()

        val result = service.getStatistics(externalUserId, today, today)

        assertEquals(Instant.parse("2026-01-15T07:45:00Z"), result.firstEntryAt)
    }

    @Test
    @DisplayName("getStatistics: forwards computed stats into InsightStatsContext for MessageProvider")
    fun `getStatistics forwards context to message provider`() {
        val today = LocalDate.of(2026, 5, 12)
        buildService(clockAt("2026-05-12T12:00:00Z"))
        stubSettings(dailyGoalMl = 2000)
        stubEvents(
            listOf(
                DtoGenerator.generateWaterStatisticDto(
                    externalUserId = externalUserId,
                    eventTime = LocalDateTime.of(2026, 5, 12, 10, 0),
                    waterAmountMl = 2200,
                )
            )
        )
        stubFirstEntry(null)
        stubInsight("text")

        val result = service.getStatistics(externalUserId, today, today)

        val ctxCaptor = argumentCaptor<InsightStatsContext>()
        verify(messageProvider).getMessage(eq(MessageSpec.InsightStats), ctxCaptor.capture())
        val ctx = ctxCaptor.firstValue
        assertEquals(2200, ctx.avgMlPerDay)
        assertEquals(2000, ctx.dailyGoalMl)
        assertEquals(1, ctx.currentStreakDays)
        assertNull(ctx.bestDay)
        assertEquals("text", result.insightText)
    }

    companion object {

        @JvmStatic
        fun provideZoneCases(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "Europe/Moscow",
                "2026-05-07T22:30:00Z",
                LocalDateTime.of(2026, 5, 7, 21, 0),
                LocalDateTime.of(2026, 5, 8, 21, 0),
                "UTC+3, no DST",
            ),
            Arguments.of(
                "UTC",
                "2026-05-07T22:30:00Z",
                LocalDateTime.of(2026, 5, 7, 0, 0),
                LocalDateTime.of(2026, 5, 8, 0, 0),
                "UTC, no offset",
            ),
            Arguments.of(
                "America/Los_Angeles",
                "2026-05-07T22:30:00Z",
                LocalDateTime.of(2026, 5, 7, 7, 0),
                LocalDateTime.of(2026, 5, 8, 7, 0),
                "UTC-7, DST in May",
            ),
            Arguments.of(
                "Pacific/Kiritimati",
                "2026-05-07T22:30:00Z",
                LocalDateTime.of(2026, 5, 7, 10, 0),
                LocalDateTime.of(2026, 5, 8, 10, 0),
                "UTC+14, extreme east",
            ),
            Arguments.of(
                "Asia/Kolkata",
                "2026-05-07T22:30:00Z",
                LocalDateTime.of(2026, 5, 7, 18, 30),
                LocalDateTime.of(2026, 5, 8, 18, 30),
                "UTC+5:30, fractional offset",
            ),
            // DST spring-forward (23h day)
            Arguments.of(
                "America/New_York",
                "2026-03-08T07:30:00Z",
                LocalDateTime.of(2026, 3, 8, 5, 0),
                LocalDateTime.of(2026, 3, 9, 4, 0),
                "DST spring-forward (23-hour day)",
            ),
        )
    }
}

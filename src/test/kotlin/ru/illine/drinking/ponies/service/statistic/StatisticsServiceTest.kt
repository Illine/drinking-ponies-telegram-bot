package ru.illine.drinking.ponies.service.statistic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.dao.access.WaterStatisticAccessService
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.StatisticsPeriodType
import ru.illine.drinking.ponies.model.dto.InsightDto
import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto
import ru.illine.drinking.ponies.service.statistic.impl.StatisticsServiceImpl
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.stream.Stream

@UnitTest
@DisplayName("StatisticsService Unit Test")
class StatisticsServiceTest {

    private val userId = 1L

    private lateinit var notificationAccessService: NotificationAccessService
    private lateinit var waterStatisticAccessService: WaterStatisticAccessService
    private lateinit var insightService: InsightService
    private lateinit var service: StatisticsService

    @BeforeEach
    fun setUp() {
        notificationAccessService = mock(NotificationAccessService::class.java)
        waterStatisticAccessService = mock(WaterStatisticAccessService::class.java)
        insightService = mock(InsightService::class.java)
    }

    // region getToday

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
        val clock = Clock.fixed(Instant.parse(fixedInstant), ZoneOffset.UTC)
        service = StatisticsServiceImpl(notificationAccessService, waterStatisticAccessService, insightService, clock)

        val settings = DtoGenerator.generateNotificationDto(
            externalUserId = userId,
            userTimeZone = zone,
        )
        whenever(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(settings)

        val expectedFromService = emptyList<WaterStatisticDto>()
        whenever(
            waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any())
        ).thenReturn(expectedFromService)

        val result = service.getToday(userId)

        val startCaptor = argumentCaptor<LocalDateTime>()
        val endCaptor = argumentCaptor<LocalDateTime>()
        verify(waterStatisticAccessService).findByUserAndEventTimeBetween(
            eq(userId), startCaptor.capture(), endCaptor.capture()
        )
        assertEquals(expectedStart, startCaptor.firstValue)
        assertEquals(expectedEnd, endCaptor.firstValue)
        assertSame(expectedFromService, result)
        verify(notificationAccessService).findNotificationSettingByTelegramUserId(userId)
    }

    @Test
    @DisplayName("getToday(): returns the list from access service unchanged")
    fun `getToday returns list as-is`() {
        // 2026-05-07T22:30:00Z -> Europe/Moscow (UTC+3) day = 2026-05-08
        val clock = Clock.fixed(Instant.parse("2026-05-07T22:30:00Z"), ZoneOffset.UTC)
        service = StatisticsServiceImpl(notificationAccessService, waterStatisticAccessService, insightService, clock)

        val settings = DtoGenerator.generateNotificationDto(
            externalUserId = userId,
            userTimeZone = "Europe/Moscow",
        )
        whenever(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(settings)

        val expected = listOf(
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = userId,
                eventTime = LocalDateTime.of(2026, 5, 8, 8, 15),
                waterAmountMl = 250,
            ),
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = userId,
                eventTime = LocalDateTime.of(2026, 5, 8, 13, 0),
                waterAmountMl = 500,
            ),
        )
        whenever(
            waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any())
        ).thenReturn(expected)

        val result = service.getToday(userId)

        assertEquals(expected, result)
    }

    // endregion

    // region getStatistics

    @Test
    @DisplayName("getStatistics(DAY): builds 24 hourly points, glues insight streak/text into result")
    fun `getStatistics DAY glues insight and 24 points`() {
        val clock = clockAt("2026-05-12T12:00:00Z")
        val settings = DtoGenerator.generateNotificationDto(externalUserId = userId, userTimeZone = "UTC")
        whenever(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(settings)
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(
                listOf(
                    DtoGenerator.generateWaterStatisticDto(
                        externalUserId = userId,
                        eventTime = LocalDateTime.of(2026, 5, 12, 8, 15),
                        waterAmountMl = 250,
                    )
                )
            )
        whenever(insightService.build(eq(userId), any(), eq(2000), any(), eq(LocalDate.of(2026, 5, 12))))
            .thenReturn(InsightDto(currentStreakDays = 3, text = "Котик, ты пьёшь водицу 3 дней подряд - так держать!"))

        service = StatisticsServiceImpl(notificationAccessService, waterStatisticAccessService, insightService, clock)

        val result = service.getStatistics(userId, StatisticsPeriodType.DAY)

        assertEquals(StatisticsPeriodType.DAY, result.period)
        assertEquals(24, result.points.size)
        assertEquals("08:00", result.points[8].label)
        assertEquals(250, result.points[8].valueMl)
        assertEquals(2000, result.dailyGoalMl)
        assertEquals(250, result.averageMlPerDay) // DAY = total
        assertNull(result.bestDay)                  // DAY -> null
        assertNull(result.goalProgress)             // DAY -> null
        assertEquals(3, result.currentStreakDays)
        assertEquals("Котик, ты пьёшь водицу 3 дней подряд - так держать!", result.insightText)
        verify(insightService).build(eq(userId), any(), eq(2000), any(), eq(LocalDate.of(2026, 5, 12)))
    }

    @Test
    @DisplayName("getStatistics(WEEK): 7 day points, bestDay set, goalProgress numeric")
    fun `getStatistics WEEK glues weekly points and bestDay`() {
        // Wednesday 2026-05-06 -> Monday 2026-05-04 .. Sunday 2026-05-10
        val clock = clockAt("2026-05-06T12:00:00Z")
        val settings = DtoGenerator.generateNotificationDto(externalUserId = userId, userTimeZone = "UTC")
        whenever(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(settings)

        val events = listOf(
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = userId,
                eventTime = LocalDateTime.of(2026, 5, 4, 10, 0),
                waterAmountMl = 2100,
            ),
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = userId,
                eventTime = LocalDateTime.of(2026, 5, 6, 10, 0),
                waterAmountMl = 2400,
            ),
        )
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(events)
        whenever(insightService.build(eq(userId), any(), eq(2000), any(), eq(LocalDate.of(2026, 5, 6))))
            .thenReturn(InsightDto(currentStreakDays = 1, text = "insight"))

        service = StatisticsServiceImpl(notificationAccessService, waterStatisticAccessService, insightService, clock)

        val result = service.getStatistics(userId, StatisticsPeriodType.WEEK)

        assertEquals(StatisticsPeriodType.WEEK, result.period)
        assertEquals(7, result.points.size)
        assertEquals("2026-05-04", result.points[0].label)
        assertEquals(2100, result.points[0].valueMl)
        assertEquals("2026-05-06", result.points[2].label)
        assertEquals(2400, result.points[2].valueMl)
        assertNotNull(result.bestDay)
        assertEquals(LocalDate.of(2026, 5, 6), result.bestDay!!.date)
        assertEquals(2400, result.bestDay!!.valueMl)
        assertEquals(DayOfWeek.WEDNESDAY, result.bestDay!!.weekday)
        assertEquals((2100 + 2400) / 7, result.averageMlPerDay)
        assertNotNull(result.goalProgress)
        // 2 successful days out of 3 elapsed (Mon..Wed)
        assertEquals(2.0 / 3.0, result.goalProgress!!, 1e-9)
        assertEquals(1, result.currentStreakDays)
        assertEquals("insight", result.insightText)
    }

    @ParameterizedTest(name = "[{index}] {0}: MONTH on {1} -> {2} points")
    @MethodSource("provideMonthLengthCases")
    @DisplayName("getStatistics(MONTH): points.size matches month length (leap/non-leap and 30/31)")
    fun `getStatistics MONTH points size`(
        @Suppress("UNUSED_PARAMETER") description: String,
        nowIso: String,
        expectedPoints: Int,
    ) {
        val clock = clockAt(nowIso)
        val settings = DtoGenerator.generateNotificationDto(externalUserId = userId, userTimeZone = "UTC")
        whenever(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(settings)
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(insightService.build(eq(userId), any(), eq(2000), any(), any()))
            .thenReturn(InsightDto(currentStreakDays = 0, text = "x"))

        service = StatisticsServiceImpl(notificationAccessService, waterStatisticAccessService, insightService, clock)

        val result = service.getStatistics(userId, StatisticsPeriodType.MONTH)

        assertEquals(expectedPoints, result.points.size)
    }

    @Test
    @DisplayName("getStatistics(MONTH): empty period -> bestDay=null, avg=0, goalProgress=0.0 (not null)")
    fun `getStatistics MONTH empty period`() {
        val clock = clockAt("2026-05-15T12:00:00Z")
        val settings = DtoGenerator.generateNotificationDto(externalUserId = userId, userTimeZone = "UTC")
        whenever(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(settings)
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(insightService.build(eq(userId), any(), eq(2000), any(), any()))
            .thenReturn(InsightDto(currentStreakDays = 0, text = "Здесь пока пусто, котик. Сделай первый глоток."))

        service = StatisticsServiceImpl(notificationAccessService, waterStatisticAccessService, insightService, clock)

        val result = service.getStatistics(userId, StatisticsPeriodType.MONTH)

        assertNull(result.bestDay)
        assertEquals(0, result.averageMlPerDay)
        assertNotNull(result.goalProgress)
        assertEquals(0.0, result.goalProgress)
        assertEquals(0, result.currentStreakDays)
        assertEquals(31, result.points.size)
        assertTrue(result.points.all { it.valueMl == 0 })
    }

    @Test
    @DisplayName("getStatistics: filters out non-YES events (SNOOZE/CANCEL with waterAmountMl>0 ignored)")
    fun `getStatistics filters non-YES events`() {
        val clock = clockAt("2026-05-12T12:00:00Z")
        val settings = DtoGenerator.generateNotificationDto(externalUserId = userId, userTimeZone = "UTC")
        whenever(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(settings)

        // YES = 500, SNOOZE+CANCEL with positive ml must be excluded.
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any())).thenReturn(
            listOf(
                DtoGenerator.generateWaterStatisticDto(
                    externalUserId = userId,
                    eventTime = LocalDateTime.of(2026, 5, 12, 8, 0),
                    eventType = AnswerNotificationType.YES,
                    waterAmountMl = 500,
                ),
                DtoGenerator.generateWaterStatisticDto(
                    externalUserId = userId,
                    eventTime = LocalDateTime.of(2026, 5, 12, 10, 0),
                    eventType = AnswerNotificationType.SNOOZE,
                    waterAmountMl = 100,
                ),
                DtoGenerator.generateWaterStatisticDto(
                    externalUserId = userId,
                    eventTime = LocalDateTime.of(2026, 5, 12, 14, 0),
                    eventType = AnswerNotificationType.CANCEL,
                    waterAmountMl = 200,
                ),
            )
        )
        whenever(insightService.build(eq(userId), any(), eq(2000), any(), eq(LocalDate.of(2026, 5, 12))))
            .thenReturn(InsightDto(currentStreakDays = 0, text = "x"))

        service = StatisticsServiceImpl(notificationAccessService, waterStatisticAccessService, insightService, clock)

        val result = service.getStatistics(userId, StatisticsPeriodType.DAY)

        // Only YES (500ml) contributes; SNOOZE 100 + CANCEL 200 must be ignored.
        assertEquals(500, result.averageMlPerDay)
        assertEquals(500, result.points[8].valueMl)
        // No other hours touched
        assertEquals(0, result.points[10].valueMl)
        assertEquals(0, result.points[14].valueMl)
    }

    @Test
    @DisplayName("getStatistics: queries fetch with UTC-mapped bounds for user TZ (Asia/Yekaterinburg +5)")
    fun `getStatistics fetches with TZ aware bounds`() {
        // 2026-05-11T22:00Z -> user local 2026-05-12T03:00 (+5). today=2026-05-12.
        val clock = clockAt("2026-05-11T22:00:00Z")
        val settings = DtoGenerator.generateNotificationDto(
            externalUserId = userId, userTimeZone = "Asia/Yekaterinburg"
        )
        whenever(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(settings)
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(insightService.build(eq(userId), any(), eq(2000), any(), eq(LocalDate.of(2026, 5, 12))))
            .thenReturn(InsightDto(currentStreakDays = 0, text = "x"))

        service = StatisticsServiceImpl(notificationAccessService, waterStatisticAccessService, insightService, clock)

        service.getStatistics(userId, StatisticsPeriodType.DAY)

        // Local day 2026-05-12 in +5 = [2026-05-11T19:00, 2026-05-12T19:00) UTC
        val startCaptor = argumentCaptor<LocalDateTime>()
        val endCaptor = argumentCaptor<LocalDateTime>()
        verify(waterStatisticAccessService).findByUserAndEventTimeBetween(
            eq(userId), startCaptor.capture(), endCaptor.capture()
        )
        assertEquals(LocalDateTime.of(2026, 5, 11, 19, 0), startCaptor.firstValue)
        assertEquals(LocalDateTime.of(2026, 5, 12, 19, 0), endCaptor.firstValue)
    }

    @Test
    @DisplayName("getStatistics: TZ boundary - 19:30 UTC event for +5 user falls into next local day")
    fun `getStatistics TZ event crosses local midnight`() {
        // today in zone +5 at 2026-05-12T00:30 local
        val clock = clockAt("2026-05-11T19:30:00Z")
        val settings = DtoGenerator.generateNotificationDto(
            externalUserId = userId, userTimeZone = "Asia/Yekaterinburg"
        )
        whenever(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(settings)

        // Event at 19:30Z = 00:30 local on 2026-05-12 -> belongs to today's DAY chart at hour 00.
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any())).thenReturn(
            listOf(
                DtoGenerator.generateWaterStatisticDto(
                    externalUserId = userId,
                    eventTime = LocalDateTime.of(2026, 5, 11, 19, 30),
                    waterAmountMl = 300,
                )
            )
        )
        whenever(insightService.build(eq(userId), any(), eq(2000), any(), eq(LocalDate.of(2026, 5, 12))))
            .thenReturn(InsightDto(currentStreakDays = 0, text = "x"))

        service = StatisticsServiceImpl(notificationAccessService, waterStatisticAccessService, insightService, clock)

        val result = service.getStatistics(userId, StatisticsPeriodType.DAY)

        // Event must land in local hour 00 (00:30 in +5), not hour 19 UTC.
        assertEquals(300, result.points[0].valueMl)
        assertEquals(0, result.points[19].valueMl)
    }

    @Test
    @DisplayName("getStatistics: forwards the requested period to insightService.build (text/streak surface unchanged)")
    fun `getStatistics forwards period to insight service`() {
        // Insight templates branch by period (DAY/WEEK/MONTH phrasing differs), so the
        // requested period must be passed downstream to InsightService.
        val clock = clockAt("2026-05-12T12:00:00Z")
        val settings = DtoGenerator.generateNotificationDto(externalUserId = userId, userTimeZone = "UTC")
        whenever(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(settings)
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(insightService.build(eq(userId), any(), eq(2000), any(), eq(LocalDate.of(2026, 5, 12))))
            .thenReturn(InsightDto(currentStreakDays = 7, text = "fixed-insight"))

        service = StatisticsServiceImpl(notificationAccessService, waterStatisticAccessService, insightService, clock)

        val day = service.getStatistics(userId, StatisticsPeriodType.DAY)
        val week = service.getStatistics(userId, StatisticsPeriodType.WEEK)
        val month = service.getStatistics(userId, StatisticsPeriodType.MONTH)

        assertEquals("fixed-insight", day.insightText)
        assertEquals("fixed-insight", week.insightText)
        assertEquals("fixed-insight", month.insightText)
        assertEquals(7, day.currentStreakDays)
        verify(insightService).build(eq(userId), eq(StatisticsPeriodType.DAY), eq(2000), any(), eq(LocalDate.of(2026, 5, 12)))
        verify(insightService).build(eq(userId), eq(StatisticsPeriodType.WEEK), eq(2000), any(), eq(LocalDate.of(2026, 5, 12)))
        verify(insightService).build(eq(userId), eq(StatisticsPeriodType.MONTH), eq(2000), any(), eq(LocalDate.of(2026, 5, 12)))
    }

    @Test
    @DisplayName("getStatistics(WEEK): goalProgress denominator equals 7 on Sunday (full week elapsed)")
    fun `getStatistics WEEK goalProgress on Sunday counts full week`() {
        // Sunday 2026-05-10 -> Monday 2026-05-04 .. Sunday 2026-05-10 (elapsed = 7).
        // 4 successful days out of 7 elapsed -> 4/7.
        val clock = clockAt("2026-05-10T12:00:00Z")
        val settings = DtoGenerator.generateNotificationDto(externalUserId = userId, userTimeZone = "UTC")
        whenever(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(settings)

        val events = listOf(
            LocalDateTime.of(2026, 5, 4, 10, 0),
            LocalDateTime.of(2026, 5, 5, 10, 0),
            LocalDateTime.of(2026, 5, 7, 10, 0),
            LocalDateTime.of(2026, 5, 9, 10, 0),
        ).map { time ->
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = userId,
                eventTime = time,
                waterAmountMl = 2000,
            )
        }
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(events)
        whenever(insightService.build(eq(userId), any(), eq(2000), any(), eq(LocalDate.of(2026, 5, 10))))
            .thenReturn(InsightDto(currentStreakDays = 0, text = "x"))

        service = StatisticsServiceImpl(notificationAccessService, waterStatisticAccessService, insightService, clock)

        val result = service.getStatistics(userId, StatisticsPeriodType.WEEK)

        assertNotNull(result.goalProgress)
        assertEquals(4.0 / 7.0, result.goalProgress!!, 1e-9)
    }

    @Test
    @DisplayName("getStatistics: WEEK insight is computed once and currentStreakDays comes from insight (not double-fetched)")
    fun `getStatistics uses insight as single source of truth for streak`() {
        val clock = clockAt("2026-05-06T12:00:00Z")
        val settings = DtoGenerator.generateNotificationDto(externalUserId = userId, userTimeZone = "UTC")
        whenever(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(settings)
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(insightService.build(eq(userId), any(), eq(2000), any(), eq(LocalDate.of(2026, 5, 6))))
            .thenReturn(InsightDto(currentStreakDays = 42, text = "from-insight"))

        service = StatisticsServiceImpl(notificationAccessService, waterStatisticAccessService, insightService, clock)

        val result = service.getStatistics(userId, StatisticsPeriodType.WEEK)

        assertEquals(42, result.currentStreakDays)
        assertEquals("from-insight", result.insightText)
        verify(insightService).build(eq(userId), any(), eq(2000), any(), eq(LocalDate.of(2026, 5, 6)))
    }

    // endregion

    private fun clockAt(iso: String): Clock = Clock.fixed(Instant.parse(iso), ZoneOffset.UTC)

    companion object {

        @JvmStatic
        fun provideZoneCases(): Stream<Arguments> = Stream.of(
            // 2026-05-07T22:30:00Z -> Moscow local 2026-05-08T01:30 (UTC+3),
            // user-day = 2026-05-08, [00:00..24:00) Moscow = [2026-05-07T21:00, 2026-05-08T21:00) UTC
            Arguments.of(
                "Europe/Moscow",
                "2026-05-07T22:30:00Z",
                LocalDateTime.of(2026, 5, 7, 21, 0),
                LocalDateTime.of(2026, 5, 8, 21, 0),
                "UTC+3, no DST",
            ),
            // 2026-05-07T22:30:00Z in UTC -> user-day = 2026-05-07, [2026-05-07T00:00, 2026-05-08T00:00) UTC
            Arguments.of(
                "UTC",
                "2026-05-07T22:30:00Z",
                LocalDateTime.of(2026, 5, 7, 0, 0),
                LocalDateTime.of(2026, 5, 8, 0, 0),
                "UTC, no offset",
            ),
            // 2026-05-07T22:30:00Z -> LA local 2026-05-07T15:30 (PDT, UTC-7),
            // user-day = 2026-05-07, [00:00..24:00) LA = [2026-05-07T07:00, 2026-05-08T07:00) UTC
            Arguments.of(
                "America/Los_Angeles",
                "2026-05-07T22:30:00Z",
                LocalDateTime.of(2026, 5, 7, 7, 0),
                LocalDateTime.of(2026, 5, 8, 7, 0),
                "UTC-7, DST in May",
            ),
            // 2026-05-07T22:30:00Z -> Kiritimati local 2026-05-08T12:30 (UTC+14),
            // user-day = 2026-05-08, [00:00..24:00) Kiritimati = [2026-05-07T10:00, 2026-05-08T10:00) UTC
            Arguments.of(
                "Pacific/Kiritimati",
                "2026-05-07T22:30:00Z",
                LocalDateTime.of(2026, 5, 7, 10, 0),
                LocalDateTime.of(2026, 5, 8, 10, 0),
                "UTC+14, extreme east",
            ),
            // 2026-05-07T22:30:00Z -> Kolkata local 2026-05-08T04:00 (UTC+5:30),
            // user-day = 2026-05-08, [00:00..24:00) Kolkata = [2026-05-07T18:30, 2026-05-08T18:30) UTC
            Arguments.of(
                "Asia/Kolkata",
                "2026-05-07T22:30:00Z",
                LocalDateTime.of(2026, 5, 7, 18, 30),
                LocalDateTime.of(2026, 5, 8, 18, 30),
                "UTC+5:30, fractional offset",
            ),
            // DST spring-forward: 2026-03-08T07:30:00Z -> NY local 2026-03-08T03:30 EDT.
            // EST->EDT happened at 02:00 local on 2026-03-08, so the day in NY has only 23 hours.
            // user-day = 2026-03-08, start = 2026-03-08T00:00 EST = 2026-03-08T05:00 UTC,
            // end = (start of 2026-03-09 in NY) = 2026-03-09T00:00 EDT = 2026-03-09T04:00 UTC.
            // This verifies that production code uses zone-aware arithmetic (plusDays(1) on
            // the zoned date), not naive plusHours(24).
            Arguments.of(
                "America/New_York",
                "2026-03-08T07:30:00Z",
                LocalDateTime.of(2026, 3, 8, 5, 0),
                LocalDateTime.of(2026, 3, 9, 4, 0),
                "DST spring-forward (23-hour day)",
            ),
        )

        @JvmStatic
        fun provideMonthLengthCases(): Stream<Arguments> = Stream.of(
            Arguments.of("Leap Feb 2024", "2024-02-15T12:00:00Z", 29),
            Arguments.of("Non-leap Feb 2025", "2025-02-15T12:00:00Z", 28),
            Arguments.of("April (30)", "2026-04-15T12:00:00Z", 30),
            Arguments.of("May (31)", "2026-05-15T12:00:00Z", 31),
            Arguments.of("June (30)", "2026-06-15T12:00:00Z", 30),
            Arguments.of("July (31)", "2026-07-15T12:00:00Z", 31),
        )
    }
}

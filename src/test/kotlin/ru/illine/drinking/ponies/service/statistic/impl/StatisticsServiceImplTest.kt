package ru.illine.drinking.ponies.service.statistic.impl

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.dao.access.WaterStatisticAccessService
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.StatisticsPeriodType
import ru.illine.drinking.ponies.model.dto.InsightDto
import ru.illine.drinking.ponies.service.statistic.InsightService
import ru.illine.drinking.ponies.service.statistic.StatisticsService
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest
import java.time.*
import java.util.stream.Stream

@UnitTest
@DisplayName("StatisticsServiceImpl Unit Test")
class StatisticsServiceImplTest {

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
        whenever(insightService.build(eq(userId), eq(2000), any(), eq(LocalDate.of(2026, 5, 12))))
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
        verify(insightService).build(eq(userId), eq(2000), any(), eq(LocalDate.of(2026, 5, 12)))
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
        whenever(insightService.build(eq(userId), eq(2000), any(), eq(LocalDate.of(2026, 5, 6))))
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
        whenever(insightService.build(eq(userId), eq(2000), any(), any()))
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
        whenever(insightService.build(eq(userId), eq(2000), any(), any()))
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
        whenever(insightService.build(eq(userId), eq(2000), any(), eq(LocalDate.of(2026, 5, 12))))
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
        whenever(insightService.build(eq(userId), eq(2000), any(), eq(LocalDate.of(2026, 5, 12))))
            .thenReturn(InsightDto(currentStreakDays = 0, text = "x"))

        service = StatisticsServiceImpl(notificationAccessService, waterStatisticAccessService, insightService, clock)

        service.getStatistics(userId, StatisticsPeriodType.DAY)

        // Local day 2026-05-12 in +5 = [2026-05-11T19:00, 2026-05-12T19:00) UTC
        val startCaptor = org.mockito.kotlin.argumentCaptor<LocalDateTime>()
        val endCaptor = org.mockito.kotlin.argumentCaptor<LocalDateTime>()
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
        whenever(insightService.build(eq(userId), eq(2000), any(), eq(LocalDate.of(2026, 5, 12))))
            .thenReturn(InsightDto(currentStreakDays = 0, text = "x"))

        service = StatisticsServiceImpl(notificationAccessService, waterStatisticAccessService, insightService, clock)

        val result = service.getStatistics(userId, StatisticsPeriodType.DAY)

        // Event must land in local hour 00 (00:30 in +5), not hour 19 UTC.
        assertEquals(300, result.points[0].valueMl)
        assertEquals(0, result.points[19].valueMl)
    }

    @Test
    @DisplayName("getStatistics: insight contract is identical for DAY/WEEK/MONTH on the same dataset and clock")
    fun `getStatistics insight is independent of period`() {
        // Same clock + same fetch result for every period -> insightService.build must receive
        // identical (userId, dailyGoalMl, zone, today) arguments and the returned streak/text
        // must surface unchanged regardless of the period the user requested.
        val clock = clockAt("2026-05-12T12:00:00Z")
        val settings = DtoGenerator.generateNotificationDto(externalUserId = userId, userTimeZone = "UTC")
        whenever(notificationAccessService.findNotificationSettingByTelegramUserId(userId)).thenReturn(settings)
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(insightService.build(eq(userId), eq(2000), any(), eq(LocalDate.of(2026, 5, 12))))
            .thenReturn(InsightDto(currentStreakDays = 7, text = "fixed-insight"))

        service = StatisticsServiceImpl(notificationAccessService, waterStatisticAccessService, insightService, clock)

        val day = service.getStatistics(userId, StatisticsPeriodType.DAY)
        val week = service.getStatistics(userId, StatisticsPeriodType.WEEK)
        val month = service.getStatistics(userId, StatisticsPeriodType.MONTH)

        assertEquals("fixed-insight", day.insightText)
        assertEquals("fixed-insight", week.insightText)
        assertEquals("fixed-insight", month.insightText)
        assertEquals(7, day.currentStreakDays)
        assertEquals(7, week.currentStreakDays)
        assertEquals(7, month.currentStreakDays)
        // Each call delegates to the same insightService.build signature.
        verify(insightService, times(3)).build(eq(userId), eq(2000), any(), eq(LocalDate.of(2026, 5, 12)))
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
        whenever(insightService.build(eq(userId), eq(2000), any(), eq(LocalDate.of(2026, 5, 10))))
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
        whenever(insightService.build(eq(userId), eq(2000), any(), eq(LocalDate.of(2026, 5, 6))))
            .thenReturn(InsightDto(currentStreakDays = 42, text = "from-insight"))

        service = StatisticsServiceImpl(notificationAccessService, waterStatisticAccessService, insightService, clock)

        val result = service.getStatistics(userId, StatisticsPeriodType.WEEK)

        assertEquals(42, result.currentStreakDays)
        assertEquals("from-insight", result.insightText)
        verify(insightService).build(eq(userId), eq(2000), any(), eq(LocalDate.of(2026, 5, 6)))
    }

    private fun clockAt(iso: String): Clock = Clock.fixed(Instant.parse(iso), ZoneOffset.UTC)

    companion object {

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

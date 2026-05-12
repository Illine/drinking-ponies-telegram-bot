package ru.illine.drinking.ponies.service.statistic

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import ru.illine.drinking.ponies.dao.access.WaterStatisticAccessService
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.StatisticsPeriodType
import ru.illine.drinking.ponies.model.dto.message.InsightStatsContext
import ru.illine.drinking.ponies.model.dto.message.MessageDto
import ru.illine.drinking.ponies.service.message.MessageProvider
import ru.illine.drinking.ponies.service.statistic.impl.InsightServiceImpl
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.message.MessageSpec
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@UnitTest
@DisplayName("InsightService Unit Test")
class InsightServiceTest {

    private val userId = 1L
    private val dailyGoal = 2000

    // 2026-05-12 is a Tuesday - week starts Monday 2026-05-11.
    private val today = LocalDate.of(2026, 5, 12)

    private lateinit var waterStatisticAccessService: WaterStatisticAccessService
    private lateinit var messageProvider: MessageProvider
    private lateinit var service: InsightServiceImpl

    @BeforeEach
    fun setUp() {
        waterStatisticAccessService = Mockito.mock(WaterStatisticAccessService::class.java)
        messageProvider = Mockito.mock(MessageProvider::class.java)
        service = InsightServiceImpl(waterStatisticAccessService, messageProvider)
    }

    @Test
    @DisplayName("build(): single fetch range covers 366 days back through today + 1")
    fun `build performs single fetch covering 366 days`() {
        val zone = ZoneOffset.UTC
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(messageProvider.getMessage<InsightStatsContext>(any(), any())).thenReturn(MessageDto("text"))

        service.build(userId, StatisticsPeriodType.WEEK, dailyGoal, zone, today)

        val startCaptor = argumentCaptor<LocalDateTime>()
        val endCaptor = argumentCaptor<LocalDateTime>()
        Mockito.verify(waterStatisticAccessService).findByUserAndEventTimeBetween(
            eq(userId), startCaptor.capture(), endCaptor.capture()
        )
        // 366 days back from 2026-05-12 -> 2025-05-11 at start of UTC day
        Assertions.assertEquals(LocalDateTime.of(2025, 5, 11, 0, 0), startCaptor.firstValue)
        // exclusive end = start of tomorrow
        Assertions.assertEquals(LocalDateTime.of(2026, 5, 13, 0, 0), endCaptor.firstValue)
    }

    @Test
    @DisplayName("build(): filters out non-YES events from streak and insight calculations")
    fun `build filters out non-YES events`() {
        val events = listOf(
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = userId,
                eventTime = LocalDateTime.of(2026, 5, 12, 9, 0),
                eventType = AnswerNotificationType.SNOOZE,
                waterAmountMl = 1000,
            ),
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = userId,
                eventTime = LocalDateTime.of(2026, 5, 12, 12, 0),
                eventType = AnswerNotificationType.CANCEL,
                waterAmountMl = 1000,
            ),
        )
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(events)
        whenever(messageProvider.getMessage<InsightStatsContext>(any(), any())).thenReturn(MessageDto("empty"))

        val result = service.build(userId, StatisticsPeriodType.WEEK, dailyGoal, ZoneOffset.UTC, today)

        Assertions.assertEquals(0, result.currentStreakDays)
        val ctxCaptor = argumentCaptor<InsightStatsContext>()
        Mockito.verify(messageProvider).getMessage(eq(MessageSpec.InsightStats), ctxCaptor.capture())
        val ctx = ctxCaptor.firstValue
        Assertions.assertEquals(0, ctx.avgMlPerDay)
        Assertions.assertEquals(0, ctx.currentStreakDays)
        Assertions.assertNull(ctx.bestDay)
    }

    @Test
    @DisplayName("build(): empty data -> streak=0, avg=0, bestDay=null in InsightStatsContext")
    fun `build empty insight context`() {
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(messageProvider.getMessage<InsightStatsContext>(any(), any())).thenReturn(MessageDto("ok"))

        val result = service.build(userId, StatisticsPeriodType.WEEK, dailyGoal, ZoneOffset.UTC, today)

        Assertions.assertEquals(0, result.currentStreakDays)
        val ctxCaptor = argumentCaptor<InsightStatsContext>()
        Mockito.verify(messageProvider).getMessage(eq(MessageSpec.InsightStats), ctxCaptor.capture())
        val ctx = ctxCaptor.firstValue
        Assertions.assertEquals(StatisticsPeriodType.WEEK, ctx.period)
        Assertions.assertEquals(0, ctx.avgMlPerDay)
        Assertions.assertEquals(0, ctx.currentStreakDays)
        Assertions.assertNull(ctx.bestDay)
        Assertions.assertEquals(dailyGoal, ctx.dailyGoalMl)
    }

    @Test
    @DisplayName("build(): only today met (no prior history) -> streak=1")
    fun `build streak only today met yields one`() {
        val zone = ZoneOffset.UTC
        val event = DtoGenerator.generateWaterStatisticDto(
            externalUserId = userId,
            eventTime = today.atTime(10, 0),
            waterAmountMl = 2000,
        )
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(listOf(event))
        whenever(messageProvider.getMessage<InsightStatsContext>(any(), any())).thenReturn(MessageDto("ok"))

        val result = service.build(userId, StatisticsPeriodType.WEEK, dailyGoal, zone, today)

        Assertions.assertEquals(1, result.currentStreakDays)
    }

    @Test
    @DisplayName("build(): today + 2 prior days complete -> streak=3 (counts today when met)")
    fun `build streak counts today when met`() {
        val zone = ZoneOffset.UTC
        val events = (0..2).map { offset ->
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = userId,
                eventTime = today.minusDays(offset.toLong()).atTime(10, 0),
                waterAmountMl = 2000,
            )
        }
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(events)
        whenever(messageProvider.getMessage<InsightStatsContext>(any(), any())).thenReturn(MessageDto("ok"))

        val result = service.build(userId, StatisticsPeriodType.WEEK, dailyGoal, zone, today)

        Assertions.assertEquals(3, result.currentStreakDays)
    }

    @Test
    @DisplayName("build(): today not met but yesterday met -> streak=1 (does not break on today)")
    fun `build streak skips today when not met but continues from yesterday`() {
        val zone = ZoneOffset.UTC
        val events = listOf(
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = userId,
                eventTime = today.atTime(10, 0),
                waterAmountMl = 500,
            ),
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = userId,
                eventTime = today.minusDays(1).atTime(10, 0),
                waterAmountMl = 2200,
            ),
        )
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(events)
        whenever(messageProvider.getMessage<InsightStatsContext>(any(), any())).thenReturn(MessageDto("ok"))

        val result = service.build(userId, StatisticsPeriodType.WEEK, dailyGoal, zone, today)

        Assertions.assertEquals(1, result.currentStreakDays)
    }

    @Test
    @DisplayName("build(): gap on day N+1 stops the streak at N+1 days (counting today + N back)")
    fun `build streak stops at gap`() {
        val zone = ZoneOffset.UTC
        val events = (0..4).map { offset ->
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = userId,
                eventTime = today.minusDays(offset.toLong()).atTime(10, 0),
                waterAmountMl = 2000,
            )
        }
        val additional = DtoGenerator.generateWaterStatisticDto(
            externalUserId = userId,
            eventTime = today.minusDays(6L).atTime(10, 0),
            waterAmountMl = 2000,
        )
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(events + additional)
        whenever(messageProvider.getMessage<InsightStatsContext>(any(), any())).thenReturn(MessageDto("ok"))

        val result = service.build(userId, StatisticsPeriodType.WEEK, dailyGoal, zone, today)

        Assertions.assertEquals(5, result.currentStreakDays)
    }

    @Test
    @DisplayName("build(): full year (366 consecutive days at goal) -> streak=366 (no off-by-one)")
    fun `build streak full year is 366`() {
        val zone = ZoneOffset.UTC
        val events = (0..365).map { offset ->
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = userId,
                eventTime = today.minusDays(offset.toLong()).atTime(10, 0),
                waterAmountMl = 2000,
            )
        }
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(events)
        whenever(messageProvider.getMessage<InsightStatsContext>(any(), any())).thenReturn(MessageDto("ok"))

        val result = service.build(userId, StatisticsPeriodType.WEEK, dailyGoal, zone, today)

        Assertions.assertEquals(366, result.currentStreakDays)
    }

    @Test
    @DisplayName("build(): TZ shift moves event into next local day for streak counting (Asia/Yekaterinburg +5)")
    fun `build respects user TZ for streak`() {
        val zone = ZoneId.of("Asia/Yekaterinburg")
        val event = DtoGenerator.generateWaterStatisticDto(
            externalUserId = userId,
            eventTime = LocalDateTime.of(2026, 5, 11, 19, 30),
            waterAmountMl = 2000,
        )
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(listOf(event))
        whenever(messageProvider.getMessage<InsightStatsContext>(any(), any())).thenReturn(MessageDto("ok"))

        val result = service.build(userId, StatisticsPeriodType.WEEK, dailyGoal, zone, today)

        Assertions.assertEquals(1, result.currentStreakDays)
    }

    @Test
    @DisplayName("build(DAY): window is today only - bestDay=null, avg=consumed today")
    fun `build DAY window is today only`() {
        val zone = ZoneOffset.UTC
        // Today: 1500ml. Earlier this week: 9999ml on Monday - must NOT leak into DAY insight.
        val events = listOf(
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = userId,
                eventTime = today.atTime(10, 0),
                waterAmountMl = 1500,
            ),
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = userId,
                eventTime = today.minusDays(1).atTime(10, 0),
                waterAmountMl = 9999,
            ),
        )
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(events)
        whenever(messageProvider.getMessage<InsightStatsContext>(any(), any())).thenReturn(MessageDto("ok"))

        service.build(userId, StatisticsPeriodType.DAY, dailyGoal, zone, today)

        val ctxCaptor = argumentCaptor<InsightStatsContext>()
        Mockito.verify(messageProvider).getMessage(eq(MessageSpec.InsightStats), ctxCaptor.capture())
        val ctx = ctxCaptor.firstValue
        Assertions.assertEquals(StatisticsPeriodType.DAY, ctx.period)
        Assertions.assertEquals(1500, ctx.avgMlPerDay)
        Assertions.assertNull(ctx.bestDay, "bestDay must be null on DAY view")
    }

    @Test
    @DisplayName("build(WEEK): window is Monday through today inclusive - avg uses elapsed days only")
    fun `build WEEK window is monday through today`() {
        val zone = ZoneOffset.UTC
        // today=Tue 2026-05-12. Monday=2026-05-11. Sunday before (=2026-05-10) and earlier must be excluded.
        val events = listOf(
            // Monday in-week: 1000ml
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = userId,
                eventTime = LocalDate.of(2026, 5, 11).atTime(10, 0),
                waterAmountMl = 1000,
            ),
            // Tuesday (today): 1500ml -> bestDay candidate
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = userId,
                eventTime = today.atTime(10, 0),
                waterAmountMl = 1500,
            ),
            // Last Sunday (previous week): must NOT leak in
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = userId,
                eventTime = LocalDate.of(2026, 5, 10).atTime(10, 0),
                waterAmountMl = 9999,
            ),
        )
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(events)
        whenever(messageProvider.getMessage<InsightStatsContext>(any(), any())).thenReturn(MessageDto("ok"))

        service.build(userId, StatisticsPeriodType.WEEK, dailyGoal, zone, today)

        val ctxCaptor = argumentCaptor<InsightStatsContext>()
        Mockito.verify(messageProvider).getMessage(eq(MessageSpec.InsightStats), ctxCaptor.capture())
        val ctx = ctxCaptor.firstValue
        Assertions.assertEquals(StatisticsPeriodType.WEEK, ctx.period)
        // avg = (1000 + 1500) / 2 elapsed days = 1250
        Assertions.assertEquals(1250, ctx.avgMlPerDay)
        Assertions.assertNotNull(ctx.bestDay)
        Assertions.assertEquals(1500, ctx.bestDay!!.valueMl)
        Assertions.assertEquals(today, ctx.bestDay!!.date)
    }

    @Test
    @DisplayName("build(MONTH): window is 1st through today inclusive - bestDay carries calendar date")
    fun `build MONTH window is first of month through today`() {
        val zone = ZoneOffset.UTC
        // today=2026-05-12. May 1..12 elapsed (12 days). April values must NOT leak.
        val events = listOf(
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = userId,
                eventTime = LocalDate.of(2026, 5, 7).atTime(10, 0),
                waterAmountMl = 600,
            ),
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = userId,
                eventTime = LocalDate.of(2026, 5, 12).atTime(10, 0),
                waterAmountMl = 1500,
            ),
            // April: must NOT pollute month window
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = userId,
                eventTime = LocalDate.of(2026, 4, 30).atTime(10, 0),
                waterAmountMl = 9999,
            ),
        )
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(events)
        whenever(messageProvider.getMessage<InsightStatsContext>(any(), any())).thenReturn(MessageDto("ok"))

        service.build(userId, StatisticsPeriodType.MONTH, dailyGoal, zone, today)

        val ctxCaptor = argumentCaptor<InsightStatsContext>()
        Mockito.verify(messageProvider).getMessage(eq(MessageSpec.InsightStats), ctxCaptor.capture())
        val ctx = ctxCaptor.firstValue
        Assertions.assertEquals(StatisticsPeriodType.MONTH, ctx.period)
        // avg = (600 + 1500) / 12 elapsed days = 175 (integer division)
        Assertions.assertEquals(175, ctx.avgMlPerDay)
        Assertions.assertNotNull(ctx.bestDay)
        Assertions.assertEquals(1500, ctx.bestDay!!.valueMl)
        Assertions.assertEquals(LocalDate.of(2026, 5, 12), ctx.bestDay!!.date)
    }

    @Test
    @DisplayName("build(): returns InsightDto with messageProvider text and computed streak")
    fun `build returns text from message provider`() {
        val zone = ZoneOffset.UTC
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(messageProvider.getMessage<InsightStatsContext>(any(), any()))
            .thenReturn(MessageDto("Здесь пока пусто, котик. Сделай первый глоток."))

        val result = service.build(userId, StatisticsPeriodType.DAY, dailyGoal, zone, today)

        Assertions.assertEquals("Здесь пока пусто, котик. Сделай первый глоток.", result.text)
        Assertions.assertEquals(0, result.currentStreakDays)
        Mockito.verify(messageProvider).getMessage(eq(MessageSpec.InsightStats), anyOrNull<InsightStatsContext>())
    }

    @Test
    @DisplayName("build(): bestDay invariant - days with valueMl=0 are never selected (DPTB-126 guard)")
    fun `build best day skips zero days`() {
        val zone = ZoneOffset.UTC
        // Mix of zero-ml YES events (recorded but no water drunk) and real ones.
        // Without the .filter { it.second > 0 } guard in renderInsight(), maxByOrNull would still
        // pick the real day (1500 > 0), so we also need an all-zero-data scenario below.
        val events = listOf(
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = userId,
                eventTime = LocalDate.of(2026, 5, 7).atTime(10, 0),
                waterAmountMl = 0,
            ),
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = userId,
                eventTime = LocalDate.of(2026, 5, 10).atTime(10, 0),
                waterAmountMl = 1500,
            ),
        )
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(events)
        whenever(messageProvider.getMessage<InsightStatsContext>(any(), any())).thenReturn(MessageDto("ok"))

        service.build(userId, StatisticsPeriodType.MONTH, dailyGoal, zone, today)

        val ctxCaptor = argumentCaptor<InsightStatsContext>()
        Mockito.verify(messageProvider).getMessage(eq(MessageSpec.InsightStats), ctxCaptor.capture())
        val ctx = ctxCaptor.firstValue
        Assertions.assertNotNull(ctx.bestDay)
        // 2026-05-10, not 2026-05-07 (zero ml) and not 2026-05-01 (no events at all).
        Assertions.assertEquals(LocalDate.of(2026, 5, 10), ctx.bestDay!!.date)
        Assertions.assertEquals(1500, ctx.bestDay!!.valueMl)
    }

    @Test
    @DisplayName("build(): bestDay invariant - all-zero days produce bestDay=null, not a zero-valued bestDay")
    fun `build best day is null when all elapsed days are zero`() {
        val zone = ZoneOffset.UTC
        // Single YES event with waterAmountMl=0 inside the month window.
        // Without the .filter { it.second > 0 } guard, maxByOrNull over elapsedTotals
        // would pick the chronologically first elapsed day with valueMl=0 - a nonsense bestDay.
        val events = listOf(
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = userId,
                eventTime = LocalDate.of(2026, 5, 7).atTime(10, 0),
                waterAmountMl = 0,
            ),
        )
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(events)
        whenever(messageProvider.getMessage<InsightStatsContext>(any(), any())).thenReturn(MessageDto("ok"))

        service.build(userId, StatisticsPeriodType.MONTH, dailyGoal, zone, today)

        val ctxCaptor = argumentCaptor<InsightStatsContext>()
        Mockito.verify(messageProvider).getMessage(eq(MessageSpec.InsightStats), ctxCaptor.capture())
        val ctx = ctxCaptor.firstValue
        Assertions.assertNull(ctx.bestDay, "bestDay must be null when no elapsed day has valueMl > 0")
    }

    @Test
    @DisplayName("build(): bestDay tie-breaks by earlier date when both days have the same valueMl")
    fun `build best day tie breaks chronologically`() {
        val zone = ZoneOffset.UTC
        // Two days in May with the same value - the earlier date must win.
        val events = listOf(
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = userId,
                eventTime = LocalDate.of(2026, 5, 3).atTime(10, 0),
                waterAmountMl = 1500,
            ),
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = userId,
                eventTime = LocalDate.of(2026, 5, 10).atTime(10, 0),
                waterAmountMl = 1500,
            ),
        )
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(events)
        whenever(messageProvider.getMessage<InsightStatsContext>(any(), any())).thenReturn(MessageDto("ok"))

        service.build(userId, StatisticsPeriodType.MONTH, dailyGoal, zone, today)

        val ctxCaptor = argumentCaptor<InsightStatsContext>()
        Mockito.verify(messageProvider).getMessage(eq(MessageSpec.InsightStats), ctxCaptor.capture())
        val ctx = ctxCaptor.firstValue
        Assertions.assertNotNull(ctx.bestDay)
        Assertions.assertEquals(LocalDate.of(2026, 5, 3), ctx.bestDay!!.date)
        Assertions.assertTrue(ctx.bestDay!!.valueMl == 1500)
    }
}
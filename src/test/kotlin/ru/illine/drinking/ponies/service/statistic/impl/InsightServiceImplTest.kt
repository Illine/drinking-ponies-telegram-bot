package ru.illine.drinking.ponies.service.statistic.impl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.*
import ru.illine.drinking.ponies.dao.access.WaterStatisticAccessService
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.dto.message.InsightStatsContext
import ru.illine.drinking.ponies.model.dto.message.MessageDto
import ru.illine.drinking.ponies.service.message.MessageProvider
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.message.MessageSpec
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@UnitTest
@DisplayName("InsightServiceImpl Unit Test")
class InsightServiceImplTest {

    private val userId = 1L
    private val dailyGoal = 2000
    private val today = LocalDate.of(2026, 5, 12)

    private lateinit var waterStatisticAccessService: WaterStatisticAccessService
    private lateinit var messageProvider: MessageProvider
    private lateinit var service: InsightServiceImpl

    @BeforeEach
    fun setUp() {
        waterStatisticAccessService = mock(WaterStatisticAccessService::class.java)
        messageProvider = mock(MessageProvider::class.java)
        service = InsightServiceImpl(waterStatisticAccessService, messageProvider)
    }

    @Test
    @DisplayName("build(): single fetch range covers 366 days back through today + 1")
    fun `build performs single fetch covering 366 days`() {
        val zone = ZoneOffset.UTC
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(messageProvider.getMessage<InsightStatsContext>(any(), any())).thenReturn(MessageDto("text"))

        service.build(userId, dailyGoal, zone, today)

        val startCaptor = argumentCaptor<LocalDateTime>()
        val endCaptor = argumentCaptor<LocalDateTime>()
        verify(waterStatisticAccessService).findByUserAndEventTimeBetween(
            eq(userId), startCaptor.capture(), endCaptor.capture()
        )
        // 366 days back from 2026-05-12 -> 2025-05-11 at start of UTC day
        assertEquals(LocalDateTime.of(2025, 5, 11, 0, 0), startCaptor.firstValue)
        // exclusive end = start of tomorrow
        assertEquals(LocalDateTime.of(2026, 5, 13, 0, 0), endCaptor.firstValue)
    }

    @Test
    @DisplayName("build(): filters out non-YES events from streak and insight calculations")
    fun `build filters out non-YES events`() {
        // Today: only SNOOZE/CANCEL events totalling 2000ml. None should count -> streak=0, empty insight context.
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

        val result = service.build(userId, dailyGoal, ZoneOffset.UTC, today)

        assertEquals(0, result.currentStreakDays)
        val ctxCaptor = argumentCaptor<InsightStatsContext>()
        verify(messageProvider).getMessage(eq(MessageSpec.InsightStats), ctxCaptor.capture())
        val ctx = ctxCaptor.firstValue
        assertEquals(0, ctx.avgMlPerDay)
        assertEquals(0, ctx.currentStreakDays)
        // No YES events -> no bestDay
        assertEquals(null, ctx.bestDay)
    }

    @Test
    @DisplayName("build(): empty data -> streak=0, avg=0, bestDay=null in InsightStatsContext")
    fun `build empty insight context`() {
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(messageProvider.getMessage<InsightStatsContext>(any(), any())).thenReturn(MessageDto("ok"))

        val result = service.build(userId, dailyGoal, ZoneOffset.UTC, today)

        assertEquals(0, result.currentStreakDays)
        val ctxCaptor = argumentCaptor<InsightStatsContext>()
        verify(messageProvider).getMessage(eq(MessageSpec.InsightStats), ctxCaptor.capture())
        val ctx = ctxCaptor.firstValue
        assertEquals(0, ctx.avgMlPerDay)
        assertEquals(0, ctx.currentStreakDays)
        assertEquals(null, ctx.bestDay)
        assertEquals(dailyGoal, ctx.dailyGoalMl)
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

        val result = service.build(userId, dailyGoal, zone, today)

        assertEquals(1, result.currentStreakDays)
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

        val result = service.build(userId, dailyGoal, zone, today)

        assertEquals(3, result.currentStreakDays)
    }

    @Test
    @DisplayName("build(): today not met but yesterday met -> streak=1 (does not break on today)")
    fun `build streak skips today when not met but continues from yesterday`() {
        val zone = ZoneOffset.UTC
        // Today: 500ml (below goal). Yesterday: 2200ml (meets goal). Day-before-yesterday: 0.
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

        val result = service.build(userId, dailyGoal, zone, today)

        assertEquals(1, result.currentStreakDays)
    }

    @Test
    @DisplayName("build(): gap on day N+1 stops the streak at N+1 days (counting today + N back)")
    fun `build streak stops at gap`() {
        val zone = ZoneOffset.UTC
        // Today + 4 days back all meet goal. Then a gap. Should yield streak=5.
        val events = (0..4).map { offset ->
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = userId,
                eventTime = today.minusDays(offset.toLong()).atTime(10, 0),
                waterAmountMl = 2000,
            )
        }
        // Day 5 back has no events (gap). Day 6 has full goal but should not contribute.
        val additional = DtoGenerator.generateWaterStatisticDto(
            externalUserId = userId,
            eventTime = today.minusDays(6L).atTime(10, 0),
            waterAmountMl = 2000,
        )
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(events + additional)
        whenever(messageProvider.getMessage<InsightStatsContext>(any(), any())).thenReturn(MessageDto("ok"))

        val result = service.build(userId, dailyGoal, zone, today)

        assertEquals(5, result.currentStreakDays)
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

        val result = service.build(userId, dailyGoal, zone, today)

        assertEquals(366, result.currentStreakDays)
    }

    @Test
    @DisplayName("build(): TZ shift moves event into next local day for streak counting (Asia/Yekaterinburg +5)")
    fun `build respects user TZ for streak`() {
        val zone = ZoneId.of("Asia/Yekaterinburg")
        // Event at 19:30 UTC on May 11 -> local 00:30 on May 12 -> counts toward today
        val event = DtoGenerator.generateWaterStatisticDto(
            externalUserId = userId,
            eventTime = LocalDateTime.of(2026, 5, 11, 19, 30),
            waterAmountMl = 2000,
        )
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(listOf(event))
        whenever(messageProvider.getMessage<InsightStatsContext>(any(), any())).thenReturn(MessageDto("ok"))

        val result = service.build(userId, dailyGoal, zone, today)

        assertEquals(1, result.currentStreakDays)
    }

    @Test
    @DisplayName("build(): insight window is last 7 days regardless of older data (independence from period)")
    fun `build insight window is fixed 7 days`() {
        val zone = ZoneOffset.UTC
        // Today and last 6 days all 2000ml -> all 7 days hit goal.
        // Plus far-in-the-past noise that should NOT alter insight window avg/bestDay.
        val recent = (0..6).map { offset ->
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = userId,
                eventTime = today.minusDays(offset.toLong()).atTime(10, 0),
                waterAmountMl = 2000,
            )
        }
        val ancient = DtoGenerator.generateWaterStatisticDto(
            externalUserId = userId,
            eventTime = today.minusDays(200L).atTime(10, 0),
            waterAmountMl = 9999,
        )
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(recent + ancient)
        whenever(messageProvider.getMessage<InsightStatsContext>(any(), any())).thenReturn(MessageDto("ok"))

        service.build(userId, dailyGoal, zone, today)

        val ctxCaptor = argumentCaptor<InsightStatsContext>()
        verify(messageProvider).getMessage(eq(MessageSpec.InsightStats), ctxCaptor.capture())
        val ctx = ctxCaptor.firstValue
        // avg = (7 * 2000) / 7 = 2000; ancient 9999 must NOT pollute the window
        assertEquals(2000, ctx.avgMlPerDay)
        assertTrue(
            ctx.bestDay != null && ctx.bestDay!!.valueMl == 2000,
            "bestDay should reflect window only; got ${ctx.bestDay}"
        )
    }

    @Test
    @DisplayName("build(): returns InsightDto with messageProvider text and computed streak")
    fun `build returns text from message provider`() {
        val zone = ZoneOffset.UTC
        whenever(waterStatisticAccessService.findByUserAndEventTimeBetween(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(messageProvider.getMessage<InsightStatsContext>(any(), any()))
            .thenReturn(MessageDto("Здесь пока пусто, котик. Сделай первый глоток."))

        val result = service.build(userId, dailyGoal, zone, today)

        assertEquals("Здесь пока пусто, котик. Сделай первый глоток.", result.text)
        assertEquals(0, result.currentStreakDays)
        verify(messageProvider).getMessage(eq(MessageSpec.InsightStats), anyOrNull<InsightStatsContext>())
    }
}

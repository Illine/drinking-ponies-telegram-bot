package ru.illine.drinking.ponies.util.statistics

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.illine.drinking.ponies.model.base.StatisticsPeriodType
import ru.illine.drinking.ponies.model.dto.StatisticsPointDto
import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.stream.Stream

@UnitTest
@DisplayName("StatisticsAggregator Unit Test")
class StatisticsAggregatorTest {

    private val utcZone = ZoneId.of("UTC")
    private val yekZone = ZoneId.of("Asia/Yekaterinburg") // +5

    @Test
    @DisplayName("sumByLocalDate(): groups events by local date and sums waterAmountMl")
    fun `sumByLocalDate groups by local date`() {
        val events = listOf(
            waterEvent(LocalDateTime.of(2026, 5, 12, 8, 0), 250),
            waterEvent(LocalDateTime.of(2026, 5, 12, 18, 0), 500),
            waterEvent(LocalDateTime.of(2026, 5, 13, 9, 0), 300),
        )

        val result = StatisticsAggregator.sumByLocalDate(events, utcZone)

        assertEquals(2, result.size)
        assertEquals(750, result[LocalDate.of(2026, 5, 12)])
        assertEquals(300, result[LocalDate.of(2026, 5, 13)])
    }

    @Test
    @DisplayName("sumByLocalDate(): event near midnight UTC shifts to next local date for +5 zone")
    fun `sumByLocalDate shifts day for positive offset zone`() {
        // 2026-05-11T19:30Z -> 2026-05-12T00:30 in +5
        val events = listOf(waterEvent(LocalDateTime.of(2026, 5, 11, 19, 30), 250))

        val result = StatisticsAggregator.sumByLocalDate(events, yekZone)

        assertEquals(1, result.size)
        assertEquals(250, result[LocalDate.of(2026, 5, 12)])
        assertNull(result[LocalDate.of(2026, 5, 11)])
    }

    @Test
    @DisplayName("sumByLocalDate(): empty events -> empty map")
    fun `sumByLocalDate empty`() {
        val result = StatisticsAggregator.sumByLocalDate(emptyList(), utcZone)

        assertTrue(result.isEmpty())
    }

    @Test
    @DisplayName("aggregateByHour(): always returns 24 points with HH:00 labels")
    fun `aggregateByHour returns 24 points`() {
        val result = StatisticsAggregator.aggregateByHour(emptyList(), utcZone)

        assertEquals(24, result.size)
        assertEquals("00:00", result.first().label)
        assertEquals("23:00", result.last().label)
        assertTrue(result.all { it.valueMl == 0 })
    }

    @Test
    @DisplayName("aggregateByHour(): event at 08:15 local -> bucket 08:00 (sum across same hour)")
    fun `aggregateByHour buckets by local hour`() {
        val events = listOf(
            waterEvent(LocalDateTime.of(2026, 5, 12, 8, 15), 250),
            waterEvent(LocalDateTime.of(2026, 5, 12, 8, 45), 300),
            waterEvent(LocalDateTime.of(2026, 5, 12, 13, 0), 500),
        )

        val result = StatisticsAggregator.aggregateByHour(events, utcZone)

        assertEquals(550, result[8].valueMl)
        assertEquals(500, result[13].valueMl)
        assertEquals("08:00", result[8].label)
        // Other hours stay zero
        assertEquals(0, result[0].valueMl)
        assertEquals(0, result[23].valueMl)
    }

    @Test
    @DisplayName("aggregateByHour(): TZ shift moves event into local hour, not UTC hour")
    fun `aggregateByHour respects user zone`() {
        // 03:30 UTC -> 08:30 in +5 -> hour 08
        val events = listOf(waterEvent(LocalDateTime.of(2026, 5, 12, 3, 30), 250))

        val result = StatisticsAggregator.aggregateByHour(events, yekZone)

        assertEquals(250, result[8].valueMl)
        assertEquals(0, result[3].valueMl)
    }

    @Test
    @DisplayName("aggregateByDay(): builds dense series with zero filling")
    fun `aggregateByDay fills zeros for missing days`() {
        val byDate = mapOf(
            LocalDate.of(2026, 5, 4) to 1800,
            LocalDate.of(2026, 5, 6) to 2400,
        )

        val result = StatisticsAggregator.aggregateByDay(byDate, LocalDate.of(2026, 5, 4), 7)

        assertEquals(7, result.size)
        assertEquals("2026-05-04", result[0].label)
        assertEquals(1800, result[0].valueMl)
        assertEquals(0, result[1].valueMl)
        assertEquals(2400, result[2].valueMl)
        assertEquals(0, result[6].valueMl)
        assertEquals("2026-05-10", result[6].label)
    }

    @Test
    @DisplayName("aggregateByDay(): empty period -> N points with valueMl=0")
    fun `aggregateByDay empty period returns zeros`() {
        val result = StatisticsAggregator.aggregateByDay(emptyMap(), LocalDate.of(2026, 5, 4), 7)

        assertEquals(7, result.size)
        assertTrue(result.all { it.valueMl == 0 })
    }

    @ParameterizedTest(name = "[{index}] {3}: period={0}, total={1} -> avg={2}")
    @MethodSource("provideAverageCases")
    @DisplayName("averageMlPerDay(): DAY returns total, WEEK/MONTH returns total/points.size (floor)")
    fun `averageMlPerDay table`(
        period: StatisticsPeriodType, points: List<StatisticsPointDto>, expected: Int,
        @Suppress("UNUSED_PARAMETER") description: String,
    ) {
        val result = StatisticsAggregator.averageMlPerDay(period, points)

        assertEquals(expected, result)
    }

    @Test
    @DisplayName("bestDay(): DAY returns null even with non-empty data")
    fun `bestDay DAY returns null`() {
        val byDate = mapOf(LocalDate.of(2026, 5, 12) to 2400)

        val result = StatisticsAggregator.bestDay(StatisticsPeriodType.DAY, byDate)

        assertNull(result)
    }

    @Test
    @DisplayName("bestDay(): WEEK with all zeros (or empty) -> null")
    fun `bestDay all zeros returns null`() {
        val resultEmpty = StatisticsAggregator.bestDay(StatisticsPeriodType.WEEK, emptyMap())
        val resultAllZero = StatisticsAggregator.bestDay(
            StatisticsPeriodType.WEEK,
            mapOf(LocalDate.of(2026, 5, 4) to 0, LocalDate.of(2026, 5, 5) to 0)
        )

        assertNull(resultEmpty)
        assertNull(resultAllZero)
    }

    @Test
    @DisplayName("bestDay(): WEEK picks max date and extracts weekday")
    fun `bestDay WEEK picks max with weekday`() {
        // 2026-05-06 is Wednesday
        val byDate = mapOf(
            LocalDate.of(2026, 5, 4) to 1800,
            LocalDate.of(2026, 5, 5) to 2100,
            LocalDate.of(2026, 5, 6) to 2400,
            LocalDate.of(2026, 5, 7) to 1500,
        )

        val result = StatisticsAggregator.bestDay(StatisticsPeriodType.WEEK, byDate)

        assertNotNull(result)
        assertEquals(LocalDate.of(2026, 5, 6), result!!.date)
        assertEquals(2400, result.valueMl)
        assertEquals(DayOfWeek.WEDNESDAY, result.weekday)
    }

    @Test
    @DisplayName("bestDay(): MONTH picks max date with correct weekday extracted")
    fun `bestDay MONTH picks max with weekday`() {
        // 2026-05-15 is a Friday.
        val byDate = mapOf(
            LocalDate.of(2026, 5, 1) to 1500,
            LocalDate.of(2026, 5, 10) to 2400,
            LocalDate.of(2026, 5, 15) to 3000,
            LocalDate.of(2026, 5, 31) to 1800,
        )

        val result = StatisticsAggregator.bestDay(StatisticsPeriodType.MONTH, byDate)

        assertNotNull(result)
        assertEquals(LocalDate.of(2026, 5, 15), result!!.date)
        assertEquals(3000, result.valueMl)
        assertEquals(DayOfWeek.FRIDAY, result.weekday)
    }

    @Test
    @DisplayName("bestDay(): tie returns one of the maxima (impl uses maxByOrNull on map entries)")
    fun `bestDay tie returns a maximum`() {
        val byDate = mapOf(
            LocalDate.of(2026, 5, 4) to 2400,
            LocalDate.of(2026, 5, 6) to 2400,
            LocalDate.of(2026, 5, 7) to 1800,
        )

        val result = StatisticsAggregator.bestDay(StatisticsPeriodType.WEEK, byDate)

        assertNotNull(result)
        assertEquals(2400, result!!.valueMl)
        // Map.entries iteration order is not contractual; just assert tie value is one of the candidates.
        assertTrue(result.date == LocalDate.of(2026, 5, 4) || result.date == LocalDate.of(2026, 5, 6))
    }

    @Test
    @DisplayName("goalProgress(): DAY returns null")
    fun `goalProgress DAY null`() {
        val points = listOf(StatisticsPointDto("08:00", 250))

        val result = StatisticsAggregator.goalProgress(
            StatisticsPeriodType.DAY, points, 2000,
            LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 12)
        )

        assertNull(result)
    }

    @Test
    @DisplayName("goalProgress(): empty period -> 0.0 (not null)")
    fun `goalProgress empty period returns zero`() {
        val result = StatisticsAggregator.goalProgress(
            StatisticsPeriodType.WEEK, emptyList(), 2000,
            LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 6)
        )

        assertNotNull(result)
        assertEquals(0.0, result)
    }

    @Test
    @DisplayName("goalProgress(): MONTH on the 5th = 5 successful / 5 elapsed = 1.0 (not 5/31)")
    fun `goalProgress MONTH uses elapsed days as denominator`() {
        // May has 31 days; today is May 5th; first 5 days all met the goal, rest are future zeros.
        val points = listOf(
            StatisticsPointDto("2026-05-01", 2100),
            StatisticsPointDto("2026-05-02", 2000),
            StatisticsPointDto("2026-05-03", 2500),
            StatisticsPointDto("2026-05-04", 2200),
            StatisticsPointDto("2026-05-05", 2000),
        ) + (6..31).map { StatisticsPointDto("2026-05-%02d".format(it), 0) }

        val result = StatisticsAggregator.goalProgress(
            StatisticsPeriodType.MONTH, points, 2000,
            LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 5)
        )

        assertNotNull(result)
        assertEquals(1.0, result)
    }

    @Test
    @DisplayName("goalProgress(): WEEK with one successful out of 7 elapsed -> ~0.1428")
    fun `goalProgress WEEK partial success`() {
        val points = listOf(
            StatisticsPointDto("2026-05-04", 2100),
            StatisticsPointDto("2026-05-05", 1500),
            StatisticsPointDto("2026-05-06", 0),
            StatisticsPointDto("2026-05-07", 0),
            StatisticsPointDto("2026-05-08", 0),
            StatisticsPointDto("2026-05-09", 0),
            StatisticsPointDto("2026-05-10", 0),
        )

        val result = StatisticsAggregator.goalProgress(
            StatisticsPeriodType.WEEK, points, 2000,
            LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 10)
        )

        assertNotNull(result)
        assertTrue(result!! > 0.14 && result < 0.15, "expected ~0.1428 but was $result")
    }

    @Test
    @DisplayName("goalProgress(): MONTH February 2025 (non-leap) with 1 success out of 28 days -> 1/28")
    fun `goalProgress non-leap February`() {
        val daysInMonth = 28
        val points = listOf(StatisticsPointDto("2025-02-01", 2100)) +
                (2..daysInMonth).map { StatisticsPointDto("2025-02-%02d".format(it), 0) }

        val result = StatisticsAggregator.goalProgress(
            StatisticsPeriodType.MONTH, points, 2000,
            LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 28)
        )

        assertNotNull(result)
        assertEquals(1.0 / daysInMonth, result!!, 1e-9)
    }

    private fun waterEvent(eventTime: LocalDateTime, ml: Int): WaterStatisticDto =
        DtoGenerator.generateWaterStatisticDto(eventTime = eventTime, waterAmountMl = ml)

    companion object {

        @JvmStatic
        fun provideAverageCases(): Stream<Arguments> = Stream.of(
            // DAY: returns total regardless of point count
            Arguments.of(
                StatisticsPeriodType.DAY,
                listOf(StatisticsPointDto("08:00", 250), StatisticsPointDto("13:00", 500)),
                750,
                "DAY = total",
            ),
            // WEEK: floor division
            Arguments.of(
                StatisticsPeriodType.WEEK,
                listOf(
                    StatisticsPointDto("2026-05-04", 1800),
                    StatisticsPointDto("2026-05-05", 2100),
                    StatisticsPointDto("2026-05-06", 2400),
                    StatisticsPointDto("2026-05-07", 1500),
                    StatisticsPointDto("2026-05-08", 0),
                    StatisticsPointDto("2026-05-09", 0),
                    StatisticsPointDto("2026-05-10", 0),
                ),
                (1800 + 2100 + 2400 + 1500) / 7,
                "WEEK with partial fill",
            ),
            // MONTH 28 days, all zero
            Arguments.of(
                StatisticsPeriodType.MONTH,
                (1..28).map { StatisticsPointDto("2025-02-%02d".format(it), 0) },
                0,
                "MONTH all zeros",
            ),
            // WEEK empty -> defensive guard returns 0 (not div-by-zero)
            Arguments.of(
                StatisticsPeriodType.WEEK,
                emptyList<StatisticsPointDto>(),
                0,
                "WEEK empty returns zero",
            ),
        )
    }
}

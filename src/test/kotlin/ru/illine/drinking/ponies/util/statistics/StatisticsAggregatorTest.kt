package ru.illine.drinking.ponies.util.statistics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.illine.drinking.ponies.model.dto.StatisticsPointDto
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
    private val yekZone = ZoneId.of("Asia/Yekaterinburg")

    @Test
    @DisplayName("sumByLocalDate(): groups events by local date and sums waterAmountMl")
    fun `sumByLocalDate groups by local date`() {
        val events =
            listOf(
                DtoGenerator.generateWaterEvent(LocalDateTime.of(2026, 5, 12, 8, 0), 250),
                DtoGenerator.generateWaterEvent(LocalDateTime.of(2026, 5, 12, 18, 0), 500),
                DtoGenerator.generateWaterEvent(LocalDateTime.of(2026, 5, 13, 9, 0), 300),
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
        val events = listOf(DtoGenerator.generateWaterEvent(LocalDateTime.of(2026, 5, 11, 19, 30), 250))

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
    @DisplayName("aggregateByHour(): all 24 labels are HH:00 in order")
    fun `aggregateByHour labels in order`() {
        val result = StatisticsAggregator.aggregateByHour(emptyList(), utcZone)

        result.forEachIndexed { i, p ->
            assertEquals("%02d:00".format(i), p.label)
        }
    }

    @Test
    @DisplayName("aggregateByHour(): event at 08:15 local -> bucket 08:00 (sum across same hour)")
    fun `aggregateByHour buckets by local hour`() {
        val events =
            listOf(
                DtoGenerator.generateWaterEvent(LocalDateTime.of(2026, 5, 12, 8, 15), 250),
                DtoGenerator.generateWaterEvent(LocalDateTime.of(2026, 5, 12, 8, 45), 300),
                DtoGenerator.generateWaterEvent(LocalDateTime.of(2026, 5, 12, 13, 0), 500),
            )

        val result = StatisticsAggregator.aggregateByHour(events, utcZone)

        assertEquals(550, result[8].valueMl)
        assertEquals(500, result[13].valueMl)
        assertEquals("08:00", result[8].label)
        assertEquals(0, result[0].valueMl)
        assertEquals(0, result[23].valueMl)
    }

    @Test
    @DisplayName("aggregateByHour(): TZ shift moves event into local hour, not UTC hour")
    fun `aggregateByHour respects user zone`() {
        // 03:30 UTC -> 08:30 in +5 -> hour 08
        val events = listOf(DtoGenerator.generateWaterEvent(LocalDateTime.of(2026, 5, 12, 3, 30), 250))

        val result = StatisticsAggregator.aggregateByHour(events, yekZone)

        assertEquals(250, result[8].valueMl)
        assertEquals(0, result[3].valueMl)
    }

    @Test
    @DisplayName("aggregateByDay(): builds dense series with zero filling")
    fun `aggregateByDay fills zeros for missing days`() {
        val byDate =
            mapOf(
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

    @ParameterizedTest(name = "[{index}] {3}: days={0}, total={1} -> avg={2}")
    @MethodSource("provideAverageCases")
    @DisplayName("averageMlPerDay(): returns floor(total / days)")
    fun `averageMlPerDay table`(
        days: Int,
        points: List<StatisticsPointDto>,
        expected: Int,
        @Suppress("UNUSED_PARAMETER") description: String,
    ) {
        val result = StatisticsAggregator.averageMlPerDay(days, points)

        assertEquals(expected, result)
    }

    @Test
    @DisplayName("averageMlPerDay(): days=1 returns total (division by 1)")
    fun `averageMlPerDay days one returns total`() {
        val points = listOf(StatisticsPointDto("08:00", 250), StatisticsPointDto("13:00", 500))

        val result = StatisticsAggregator.averageMlPerDay(1, points)

        assertEquals(750, result)
    }

    @Test
    @DisplayName("bestDay(): days=1 returns null even with non-empty data")
    fun `bestDay days one returns null`() {
        val byDate = mapOf(LocalDate.of(2026, 5, 12) to 2400)

        val result = StatisticsAggregator.bestDay(1, byDate)

        assertNull(result)
    }

    @Test
    @DisplayName("bestDay(): empty byDate -> null")
    fun `bestDay empty returns null`() {
        val result = StatisticsAggregator.bestDay(7, emptyMap())

        assertNull(result)
    }

    @Test
    @DisplayName("bestDay(): all-zero values -> null")
    fun `bestDay all zeros returns null`() {
        val byDate =
            mapOf(
                LocalDate.of(2026, 5, 4) to 0,
                LocalDate.of(2026, 5, 5) to 0,
            )

        val result = StatisticsAggregator.bestDay(7, byDate)

        assertNull(result)
    }

    @Test
    @DisplayName("bestDay(): picks max value with weekday")
    fun `bestDay picks max with weekday`() {
        // 2026-05-06 is Wednesday
        val byDate =
            mapOf(
                LocalDate.of(2026, 5, 4) to 1800,
                LocalDate.of(2026, 5, 5) to 2100,
                LocalDate.of(2026, 5, 6) to 2400,
                LocalDate.of(2026, 5, 7) to 1500,
            )

        val result = StatisticsAggregator.bestDay(7, byDate)

        assertNotNull(result)
        assertEquals(LocalDate.of(2026, 5, 6), result!!.date)
        assertEquals(2400, result.valueMl)
        assertEquals(DayOfWeek.WEDNESDAY, result.weekday)
    }

    @Test
    @DisplayName("bestDay(): tie returns chronologically first maximum")
    fun `bestDay tie returns earliest`() {
        val byDate =
            mapOf(
                LocalDate.of(2026, 5, 6) to 2400,
                LocalDate.of(2026, 5, 4) to 2400,
                LocalDate.of(2026, 5, 7) to 1800,
            )

        val result = StatisticsAggregator.bestDay(7, byDate)

        assertNotNull(result)
        assertEquals(LocalDate.of(2026, 5, 4), result!!.date)
        assertEquals(2400, result.valueMl)
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideCalculateStreakCases")
    @DisplayName("calculateStreak(): computes streak from byDate against today and dailyGoalMl")
    fun `calculateStreak table`(
        byDate: Map<LocalDate, Int>,
        today: LocalDate,
        dailyGoalMl: Int,
        @Suppress("UNUSED_PARAMETER") description: String,
        expected: Int,
    ) {
        val result = StatisticsAggregator.calculateStreak(byDate, today, dailyGoalMl = dailyGoalMl)

        assertEquals(expected, result)
    }

    companion object {
        @JvmStatic
        fun provideAverageCases(): Stream<Arguments> =
            Stream.of(
                // days=1, total=750
                Arguments.of(
                    1,
                    listOf(StatisticsPointDto("08:00", 250), StatisticsPointDto("13:00", 500)),
                    750,
                    "days=1 returns total",
                ),
                // days=7, floor division
                Arguments.of(
                    7,
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
                    "days=7 with partial fill (floor division)",
                ),
                // days=28, all zeros
                Arguments.of(
                    28,
                    (1..28).map { StatisticsPointDto("2025-02-%02d".format(it), 0) },
                    0,
                    "all zeros",
                ),
                // days=2, total=999 -> 999/2 = 499 (floor)
                Arguments.of(
                    2,
                    listOf(
                        StatisticsPointDto("2026-05-04", 500),
                        StatisticsPointDto("2026-05-05", 499),
                    ),
                    499,
                    "floor division non-zero",
                ),
            )

        @JvmStatic
        fun provideCalculateStreakCases(): Stream<Arguments> {
            val today = LocalDate.of(2026, 5, 12)
            return Stream.of(
                Arguments.of(
                    (0..2).associate { today.minusDays(it.toLong()) to 2000 },
                    today,
                    2000,
                    "today met and consecutive days back -> counts today plus chain",
                    3,
                ),
                Arguments.of(
                    emptyMap<LocalDate, Int>(),
                    today,
                    2000,
                    "empty byDate returns 0",
                    0,
                ),
                Arguments.of(
                    mapOf(
                        today to 2000,
                        today.minusDays(1) to 2000,
                        today.minusDays(2) to 2000,
                        today.minusDays(3) to 500,
                        today.minusDays(4) to 2000,
                    ),
                    today,
                    2000,
                    "below goal on a day breaks the chain back",
                    3,
                ),
                Arguments.of(
                    (0..500L).associate { today.minusDays(it) to 2000 },
                    today,
                    2000,
                    "bounded by STREAK_LIMIT_DAYS (366) plus today -> 367",
                    367,
                ),
                Arguments.of(
                    mapOf(
                        today to 500,
                        today.minusDays(1) to 2000,
                        today.minusDays(2) to 2000,
                    ),
                    today,
                    2000,
                    "today below goal -> streak=0 (chain broken now, prior days ignored)",
                    0,
                ),
                Arguments.of(
                    mapOf(today.minusDays(1) to 2000),
                    today,
                    2000,
                    "today missing entirely -> streak=0",
                    0,
                ),
                Arguments.of(
                    mapOf(today to 2200),
                    today,
                    2000,
                    "only today met -> streak=1",
                    1,
                ),
            )
        }
    }
}

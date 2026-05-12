package ru.illine.drinking.ponies.util.statistics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.illine.drinking.ponies.model.base.StatisticsPeriodType
import ru.illine.drinking.ponies.test.tag.UnitTest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.stream.Stream

@UnitTest
@DisplayName("StatisticsPeriodHelper Unit Test")
class StatisticsPeriodHelperTest {

    @ParameterizedTest(name = "[{index}] DAY anchor={0} -> [{1}, {2})")
    @MethodSource("provideDayBounds")
    @DisplayName("periodBounds(DAY): returns anchor..anchor+1")
    fun `periodBounds DAY returns anchor and anchor plus one`(
        anchor: LocalDate, expectedStart: LocalDate, expectedEnd: LocalDate,
    ) {
        val (start, end) = StatisticsPeriodHelper.periodBounds(StatisticsPeriodType.DAY, anchor)

        assertEquals(expectedStart, start)
        assertEquals(expectedEnd, end)
    }

    @ParameterizedTest(name = "[{index}] WEEK anchor={0} ({3}) -> [{1}, {2})")
    @MethodSource("provideWeekBounds")
    @DisplayName("periodBounds(WEEK): returns Monday..next Monday (ISO week)")
    fun `periodBounds WEEK returns iso week`(
        anchor: LocalDate, expectedStart: LocalDate, expectedEnd: LocalDate,
        @Suppress("UNUSED_PARAMETER") description: String,
    ) {
        val (start, end) = StatisticsPeriodHelper.periodBounds(StatisticsPeriodType.WEEK, anchor)

        assertEquals(expectedStart, start)
        assertEquals(expectedEnd, end)
    }

    @ParameterizedTest(name = "[{index}] MONTH anchor={0} ({3}) -> [{1}, {2})")
    @MethodSource("provideMonthBounds")
    @DisplayName("periodBounds(MONTH): returns first..first of next month")
    fun `periodBounds MONTH returns calendar month`(
        anchor: LocalDate, expectedStart: LocalDate, expectedEnd: LocalDate,
        @Suppress("UNUSED_PARAMETER") description: String,
    ) {
        val (start, end) = StatisticsPeriodHelper.periodBounds(StatisticsPeriodType.MONTH, anchor)

        assertEquals(expectedStart, start)
        assertEquals(expectedEnd, end)
    }

    @ParameterizedTest(name = "[{index}] period={0} startLocal={1} -> daysIn={2} ({3})")
    @MethodSource("provideDaysInCases")
    @DisplayName("daysIn(): returns period length in days")
    fun `daysIn returns days for period`(
        period: StatisticsPeriodType, startLocal: LocalDate, expectedDays: Int,
        @Suppress("UNUSED_PARAMETER") description: String,
    ) {
        val result = StatisticsPeriodHelper.daysIn(period, startLocal)

        assertEquals(expectedDays, result)
    }

    @Test
    @DisplayName("toLocal(): converts UTC LocalDateTime to user zone LocalDateTime")
    fun `toLocal converts utc to user zone`() {
        // 03:30 UTC -> +5 zone = 08:30 local
        val utc = LocalDateTime.of(2026, 5, 12, 3, 30)
        val zone = ZoneId.of("Asia/Yekaterinburg") // +5

        val result = StatisticsPeriodHelper.toLocal(utc, zone)

        assertEquals(LocalDateTime.of(2026, 5, 12, 8, 30), result)
    }

    @Test
    @DisplayName("toLocal(): crosses day boundary in positive zone")
    fun `toLocal crosses day boundary`() {
        // 19:30 UTC -> +5 zone = next day 00:30 local
        val utc = LocalDateTime.of(2026, 5, 11, 19, 30)
        val zone = ZoneId.of("Asia/Yekaterinburg") // +5

        val result = StatisticsPeriodHelper.toLocal(utc, zone)

        assertEquals(LocalDateTime.of(2026, 5, 12, 0, 30), result)
    }

    @ParameterizedTest(name = "[{index}] zone={0} localDay={1} ({4}) -> [{2}, {3})")
    @MethodSource("provideLocalDayBoundsCases")
    @DisplayName("localDayBoundsToUtc(): zone-aware UTC bounds for local day")
    fun `localDayBoundsToUtc returns zone aware utc bounds`(
        zone: String, localDay: LocalDate,
        expectedStartUtc: LocalDateTime, expectedEndUtc: LocalDateTime,
        @Suppress("UNUSED_PARAMETER") description: String,
    ) {
        val (startUtc, endUtc) = StatisticsPeriodHelper.localDayBoundsToUtc(
            localDay, localDay.plusDays(1), ZoneId.of(zone)
        )

        assertEquals(expectedStartUtc, startUtc)
        assertEquals(expectedEndUtc, endUtc)
    }

    companion object {

        @JvmStatic
        fun provideDayBounds(): Stream<Arguments> = Stream.of(
            Arguments.of(LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 13)),
            // Leap day boundary - day after Feb 29 is March 1
            Arguments.of(LocalDate.of(2024, 2, 29), LocalDate.of(2024, 2, 29), LocalDate.of(2024, 3, 1)),
        )

        @JvmStatic
        fun provideWeekBounds(): Stream<Arguments> = Stream.of(
            // Wednesday 2026-05-06 -> Monday 2026-05-04..2026-05-11
            Arguments.of(
                LocalDate.of(2026, 5, 6),
                LocalDate.of(2026, 5, 4),
                LocalDate.of(2026, 5, 11),
                "Wednesday anchor",
            ),
            // Monday anchor is idempotent
            Arguments.of(
                LocalDate.of(2026, 5, 4),
                LocalDate.of(2026, 5, 4),
                LocalDate.of(2026, 5, 11),
                "Monday idempotent",
            ),
            // Sunday anchor 2026-05-10 -> Monday 2026-05-04..2026-05-11
            Arguments.of(
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 4),
                LocalDate.of(2026, 5, 11),
                "Sunday last day of week",
            ),
            // Cross-month week: 2026-04-29 (Wed) is part of week Apr 27 .. May 4
            Arguments.of(
                LocalDate.of(2026, 4, 29),
                LocalDate.of(2026, 4, 27),
                LocalDate.of(2026, 5, 4),
                "Week spans month boundary",
            ),
        )

        @JvmStatic
        fun provideMonthBounds(): Stream<Arguments> = Stream.of(
            Arguments.of(
                LocalDate.of(2026, 5, 15),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1),
                "May (31 days)",
            ),
            // Leap February 2024
            Arguments.of(
                LocalDate.of(2024, 2, 15),
                LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 3, 1),
                "Leap Feb 2024",
            ),
            // Non-leap February 2025
            Arguments.of(
                LocalDate.of(2025, 2, 28),
                LocalDate.of(2025, 2, 1),
                LocalDate.of(2025, 3, 1),
                "Non-leap Feb 2025",
            ),
            // Year boundary: Dec 2025
            Arguments.of(
                LocalDate.of(2025, 12, 15),
                LocalDate.of(2025, 12, 1),
                LocalDate.of(2026, 1, 1),
                "December spans year boundary",
            ),
        )

        @JvmStatic
        fun provideDaysInCases(): Stream<Arguments> = Stream.of(
            Arguments.of(StatisticsPeriodType.DAY, LocalDate.of(2026, 5, 12), 1, "DAY"),
            Arguments.of(StatisticsPeriodType.WEEK, LocalDate.of(2026, 5, 4), 7, "WEEK"),
            // Leap February
            Arguments.of(StatisticsPeriodType.MONTH, LocalDate.of(2024, 2, 1), 29, "Feb 2024 (leap)"),
            // Non-leap February
            Arguments.of(StatisticsPeriodType.MONTH, LocalDate.of(2025, 2, 1), 28, "Feb 2025"),
            // April (30 days)
            Arguments.of(StatisticsPeriodType.MONTH, LocalDate.of(2026, 4, 1), 30, "April (30)"),
            // May (31)
            Arguments.of(StatisticsPeriodType.MONTH, LocalDate.of(2026, 5, 1), 31, "May (31)"),
            // June (30)
            Arguments.of(StatisticsPeriodType.MONTH, LocalDate.of(2026, 6, 1), 30, "June (30)"),
            // July (31)
            Arguments.of(StatisticsPeriodType.MONTH, LocalDate.of(2026, 7, 1), 31, "July (31)"),
        )

        @JvmStatic
        fun provideLocalDayBoundsCases(): Stream<Arguments> = Stream.of(
            // UTC
            Arguments.of(
                "UTC",
                LocalDate.of(2026, 5, 12),
                LocalDateTime.of(2026, 5, 12, 0, 0),
                LocalDateTime.of(2026, 5, 13, 0, 0),
                "UTC",
            ),
            // Asia/Yekaterinburg (+5)
            Arguments.of(
                "Asia/Yekaterinburg",
                LocalDate.of(2026, 5, 12),
                LocalDateTime.of(2026, 5, 11, 19, 0),
                LocalDateTime.of(2026, 5, 12, 19, 0),
                "UTC+5",
            ),
            // Pacific/Kiritimati (+14)
            Arguments.of(
                "Pacific/Kiritimati",
                LocalDate.of(2026, 5, 12),
                LocalDateTime.of(2026, 5, 11, 10, 0),
                LocalDateTime.of(2026, 5, 12, 10, 0),
                "UTC+14 extreme",
            ),
            // Etc/GMT+12 is UTC-12 (POSIX sign-flip).
            Arguments.of(
                "Etc/GMT+12",
                LocalDate.of(2026, 5, 12),
                LocalDateTime.of(2026, 5, 12, 12, 0),
                LocalDateTime.of(2026, 5, 13, 12, 0),
                "UTC-12 extreme",
            ),
            // Asia/Kolkata (+5:30) fractional
            Arguments.of(
                "Asia/Kolkata",
                LocalDate.of(2026, 5, 12),
                LocalDateTime.of(2026, 5, 11, 18, 30),
                LocalDateTime.of(2026, 5, 12, 18, 30),
                "UTC+5:30 fractional",
            ),
            // DST spring-forward NY: 2026-03-08 is a 23-hour day in NY.
            // Start of 2026-03-08 in NY = 00:00 EST = 05:00 UTC.
            // End of day = start of 2026-03-09 in NY = 00:00 EDT = 04:00 UTC.
            // Total span = 23h, proving zone-aware arithmetic (not naive plusHours(24)).
            Arguments.of(
                "America/New_York",
                LocalDate.of(2026, 3, 8),
                LocalDateTime.of(2026, 3, 8, 5, 0),
                LocalDateTime.of(2026, 3, 9, 4, 0),
                "DST spring-forward (23-hour day)",
            ),
            // DST fall-back NY: 2026-11-01 is a 25-hour day in NY.
            // Start of 2026-11-01 = 00:00 EDT = 04:00 UTC.
            // End of day = start of 2026-11-02 = 00:00 EST = 05:00 UTC.
            // Total span = 25h.
            Arguments.of(
                "America/New_York",
                LocalDate.of(2026, 11, 1),
                LocalDateTime.of(2026, 11, 1, 4, 0),
                LocalDateTime.of(2026, 11, 2, 5, 0),
                "DST fall-back (25-hour day)",
            ),
        )
    }
}

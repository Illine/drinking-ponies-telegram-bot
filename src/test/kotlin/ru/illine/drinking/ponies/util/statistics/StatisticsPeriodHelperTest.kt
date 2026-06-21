package ru.illine.drinking.ponies.util.statistics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.illine.drinking.ponies.test.tag.UnitTest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.stream.Stream

@UnitTest
@DisplayName("StatisticsPeriodHelper Unit Test")
class StatisticsPeriodHelperTest {
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

    @Test
    @DisplayName("toLocal(): UTC zone is a no-op")
    fun `toLocal utc zone is identity`() {
        val utc = LocalDateTime.of(2026, 5, 12, 10, 15)

        val result = StatisticsPeriodHelper.toLocal(utc, ZoneId.of("UTC"))

        assertEquals(utc, result)
    }

    @ParameterizedTest(name = "[{index}] zone={0} localDay={1} ({4}) -> [{2}, {3})")
    @MethodSource("provideLocalDayBoundsCases")
    @DisplayName("localDayBoundsToUtc(): zone-aware UTC bounds for local day")
    fun `localDayBoundsToUtc returns zone aware utc bounds`(
        zone: String,
        localDay: LocalDate,
        expectedStartUtc: LocalDateTime,
        expectedEndUtc: LocalDateTime,
        @Suppress("UNUSED_PARAMETER") description: String,
    ) {
        val (startUtc, endUtc) =
            StatisticsPeriodHelper.localDayBoundsToUtc(
                localDay,
                localDay.plusDays(1),
                ZoneId.of(zone),
            )

        assertEquals(expectedStartUtc, startUtc)
        assertEquals(expectedEndUtc, endUtc)
    }

    @Test
    @DisplayName("toUtcInstant(): treats LocalDateTime as UTC and returns matching Instant")
    fun `toUtcInstant treats local as utc`() {
        val ldt = LocalDateTime.of(2026, 5, 12, 10, 15, 30)

        val result = ldt.toUtcInstant()

        assertEquals(Instant.parse("2026-05-12T10:15:30Z"), result)
    }

    @Test
    @DisplayName("toUtcInstant(): midnight maps to ...T00:00:00Z")
    fun `toUtcInstant midnight`() {
        val ldt = LocalDateTime.of(2026, 1, 1, 0, 0)

        val result = ldt.toUtcInstant()

        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), result)
    }

    companion object {
        @JvmStatic
        fun provideLocalDayBoundsCases(): Stream<Arguments> =
            Stream.of(
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
                Arguments.of(
                    "America/New_York",
                    LocalDate.of(2026, 3, 8),
                    LocalDateTime.of(2026, 3, 8, 5, 0),
                    LocalDateTime.of(2026, 3, 9, 4, 0),
                    "DST spring-forward (23-hour day)",
                ),
                // DST fall-back NY: 2026-11-01 is a 25-hour day in NY.
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

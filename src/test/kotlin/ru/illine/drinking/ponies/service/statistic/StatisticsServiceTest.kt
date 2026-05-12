package ru.illine.drinking.ponies.service.statistic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
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
import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto
import ru.illine.drinking.ponies.service.statistic.impl.StatisticsServiceImpl
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest
import java.time.Clock
import java.time.Instant
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
    }
}

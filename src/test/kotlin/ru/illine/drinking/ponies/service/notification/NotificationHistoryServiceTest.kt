package ru.illine.drinking.ponies.service.notification

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.dao.access.WaterStatisticAccessService
import ru.illine.drinking.ponies.exception.NotificationHistoryEntryNotEditableException
import ru.illine.drinking.ponies.exception.NotificationHistoryEntryNotFoundException
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.NotificationHistoryStatus
import ru.illine.drinking.ponies.model.base.WaterEntrySourceType
import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto
import ru.illine.drinking.ponies.service.notification.impl.NotificationHistoryServiceImpl
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.stream.Stream

@UnitTest
@DisplayName("NotificationHistoryService Unit Test")
class NotificationHistoryServiceTest {
    private lateinit var notificationAccessService: NotificationAccessService
    private lateinit var waterStatisticAccessService: WaterStatisticAccessService
    private lateinit var service: NotificationHistoryService

    @BeforeEach
    fun setUp() {
        notificationAccessService = mock<NotificationAccessService>()
        waterStatisticAccessService = mock<WaterStatisticAccessService>()
        service =
            NotificationHistoryServiceImpl(
                notificationAccessService,
                waterStatisticAccessService,
                Clock.fixed(NOW, ZoneOffset.UTC),
            )
    }

    private fun stubZone(zone: String = "UTC") {
        whenever(notificationAccessService.findNotificationSettingByExternalUserId(EXTERNAL_USER_ID))
            .thenReturn(
                DtoGenerator.generateNotificationDto(externalUserId = EXTERNAL_USER_ID, userTimeZone = zone),
            )
    }

    private fun stubEvents(vararg events: WaterStatisticDto) {
        whenever(
            waterStatisticAccessService.findByUserAndTypesAndEventTimeBetween(any(), any(), any(), any(), any()),
        ).thenReturn(events.toList())
    }

    private fun stubStoredEntry(entry: WaterStatisticDto?) {
        whenever(waterStatisticAccessService.findByIdAndUser(ENTRY_ID, EXTERNAL_USER_ID)).thenReturn(entry)
    }

    // The access layer returns the record as it was persisted, so the stub echoes the merged DTO back.
    private fun stubSaveEcho() {
        whenever(waterStatisticAccessService.save(any())).thenAnswer { it.getArgument<WaterStatisticDto>(0) }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideZoneBoundaryCases")
    @DisplayName("getHistory(): reads the range as civil days of the user timezone")
    fun `getHistory computes day bounds in user TZ`(
        zone: String,
        from: LocalDate,
        to: LocalDate,
        expectedStart: LocalDateTime,
        expectedEnd: LocalDateTime,
    ) {
        stubZone(zone)
        stubEvents()

        service.getHistory(EXTERNAL_USER_ID, from, to)

        verify(waterStatisticAccessService).findByUserAndTypesAndEventTimeBetween(
            eq(EXTERNAL_USER_ID),
            any(),
            any(),
            eq(expectedStart),
            eq(expectedEnd),
        )
    }

    @Test
    @DisplayName("getHistory(): asks the access layer only for journal sources and event types")
    fun `getHistory narrows the query to journal entries`() {
        stubZone()
        stubEvents()

        service.getHistory(EXTERNAL_USER_ID, LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 4))

        // Snoozed answers and manual entries are cut off by the query itself, not in memory.
        verify(waterStatisticAccessService).findByUserAndTypesAndEventTimeBetween(
            eq(EXTERNAL_USER_ID),
            argThat { toSet() == setOf(WaterEntrySourceType.NOTIFICATION) },
            argThat { toSet() == setOf(AnswerNotificationType.YES, AnswerNotificationType.CANCEL) },
            any(),
            any(),
        )
    }

    @Test
    @DisplayName("getHistory(): groups entries by their local date, not by the UTC one")
    fun `getHistory groups by local date`() {
        stubZone("Europe/Moscow")
        stubEvents(
            // 00:30 of May 4th locally
            entry(id = 1L, eventTime = LocalDateTime.of(2026, 5, 3, 21, 30, 0)),
            // 23:59 of May 4th locally
            entry(id = 2L, eventTime = LocalDateTime.of(2026, 5, 4, 20, 59, 0)),
            // 00:00 of May 5th locally
            entry(
                id = 3L,
                eventTime = LocalDateTime.of(2026, 5, 4, 21, 0, 0),
                eventType = AnswerNotificationType.CANCEL,
            ),
        )

        val actual = service.getHistory(EXTERNAL_USER_ID, LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 5))

        assertEquals(listOf(LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 5)), actual.days.map { it.date })
        assertEquals(listOf(1L, 2L), actual.days[0].events.map { it.id })
        assertEquals(listOf(3L), actual.days[1].events.map { it.id })
        assertEquals(Instant.parse("2026-05-03T21:30:00Z"), actual.days[0].events[0].eventTime)
        assertEquals(NotificationHistoryStatus.CONFIRMED, actual.days[0].events[0].status)
        assertEquals(NotificationHistoryStatus.MISSED, actual.days[1].events[0].status)
    }

    @Test
    @DisplayName("getHistory(): groups by the offset effective at the event, across a DST switch")
    fun `getHistory groups across DST switch`() {
        stubZone("America/New_York")
        stubEvents(
            // 23:30 of March 7th locally (UTC-5, before the switch)
            entry(id = 1L, eventTime = LocalDateTime.of(2026, 3, 8, 4, 30, 0)),
            // 00:30 of March 9th locally (UTC-4, after the switch)
            entry(id = 2L, eventTime = LocalDateTime.of(2026, 3, 9, 4, 30, 0)),
        )

        val actual = service.getHistory(EXTERNAL_USER_ID, LocalDate.of(2026, 3, 7), LocalDate.of(2026, 3, 9))

        assertEquals(listOf(LocalDate.of(2026, 3, 7), LocalDate.of(2026, 3, 9)), actual.days.map { it.date })
        assertEquals(listOf(1L), actual.days[0].events.map { it.id })
        assertEquals(listOf(2L), actual.days[1].events.map { it.id })
    }

    @Test
    @DisplayName("getHistory(): returns no days when the user has no matching entries")
    fun `getHistory returns empty days`() {
        stubZone()
        stubEvents()

        val actual = service.getHistory(EXTERNAL_USER_ID, LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 10))

        assertTrue(actual.days.isEmpty())
    }

    @Test
    @DisplayName("getHistory(): returns days in ascending order")
    fun `getHistory sorts days ascending`() {
        stubZone()
        stubEvents(
            entry(id = 2L, eventTime = LocalDateTime.of(2026, 5, 5, 10, 0, 0)),
            entry(id = 1L, eventTime = LocalDateTime.of(2026, 5, 4, 10, 0, 0)),
        )

        val actual = service.getHistory(EXTERNAL_USER_ID, LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 5))

        assertEquals(listOf(LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 5)), actual.days.map { it.date })
    }

    @ParameterizedTest(name = "[{index}] {0}, eventTime={1} -> editable={2}")
    @MethodSource("provideEditWindowCases")
    @DisplayName("getHistory(): marks an entry editable by the calendar date of the user timezone")
    fun `getHistory marks entries by the calendar edit window`(
        zone: String,
        eventTime: LocalDateTime,
        expectedEditable: Boolean,
    ) {
        stubZone(zone)
        stubEvents(entry(eventTime = eventTime))

        val actual = service.getHistory(EXTERNAL_USER_ID, WINDOW_CASE_FROM, WINDOW_CASE_TO)

        val events = actual.days.single().events
        assertEquals(expectedEditable, events.single().editable)
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideLocalDayEdges")
    @DisplayName("getHistory(): gives every entry of one local day the same editable flag")
    fun `getHistory keeps the editable flag uniform inside a day`(
        zone: String,
        dayOutsideWindow: List<LocalDateTime>,
        dayInsideWindow: List<LocalDateTime>,
    ) {
        stubZone(zone)
        stubEvents(
            // The first and the last second of May 2nd locally - the whole day is one day too old
            entry(id = 1L, eventTime = dayOutsideWindow[0]),
            entry(id = 2L, eventTime = dayOutsideWindow[1]),
            // The first and the last second of May 3rd locally - the oldest day still inside the window
            entry(id = 3L, eventTime = dayInsideWindow[0]),
            entry(id = 4L, eventTime = dayInsideWindow[1]),
        )

        val actual = service.getHistory(EXTERNAL_USER_ID, LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 3))

        // A day is indivisible: the client draws a single lock per day, so a day never mixes both flags.
        assertEquals(listOf(LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 3)), actual.days.map { it.date })
        assertEquals(listOf(false, false), actual.days[0].events.map { it.editable })
        assertEquals(listOf(true, true), actual.days[1].events.map { it.editable })
    }

    @Test
    @DisplayName("getHistory(): rejects a range whose start is after its end")
    fun `getHistory rejects reversed range`() {
        assertThrows<IllegalArgumentException> {
            service.getHistory(EXTERNAL_USER_ID, LocalDate.of(2026, 5, 11), LocalDate.of(2026, 5, 10))
        }

        verifyNoInteractions(waterStatisticAccessService)
    }

    @Test
    @DisplayName("getHistory(): rejects a range longer than the allowed maximum")
    fun `getHistory rejects too long range`() {
        val from = LocalDate.of(2026, 1, 1)

        assertThrows<IllegalArgumentException> {
            service.getHistory(
                EXTERNAL_USER_ID,
                from,
                from.plusDays(NotificationHistoryServiceImpl.MAX_RANGE_DAYS),
            )
        }

        verifyNoInteractions(waterStatisticAccessService)
    }

    @Test
    @DisplayName("getHistory(): rejects a range beyond the supported year")
    fun `getHistory rejects a range beyond the supported year`() {
        assertThrows<IllegalArgumentException> {
            service.getHistory(EXTERNAL_USER_ID, LocalDate.MAX.minusDays(1), LocalDate.MAX)
        }

        verifyNoInteractions(waterStatisticAccessService)
    }

    @Test
    @DisplayName("getHistory(): rejects a range before the supported year")
    fun `getHistory rejects a range before the supported year`() {
        // The zone is stubbed on purpose: shifting the start of such a range into UTC underflows with a
        // DateTimeException, which is a server error, so the range has to be turned down as an invalid one.
        stubZone("Europe/Moscow")

        assertThrows<IllegalArgumentException> {
            service.getHistory(EXTERNAL_USER_ID, LocalDate.MIN, LocalDate.MIN.plusDays(1))
        }

        verifyNoInteractions(waterStatisticAccessService)
    }

    @Test
    @DisplayName("getHistory(): returns no days for a range that lies entirely in the future")
    fun `getHistory returns no days for a future range`() {
        stubZone()
        stubEvents()

        val actual = service.getHistory(EXTERNAL_USER_ID, LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 31))

        assertEquals(0, actual.days.size)
        verify(waterStatisticAccessService)
            .findByUserAndTypesAndEventTimeBetween(any(), any(), any(), any(), any())
    }

    @Test
    @DisplayName("getHistory(): accepts the longest allowed range")
    fun `getHistory accepts the longest allowed range`() {
        stubZone()
        stubEvents()
        val from = LocalDate.of(2026, 1, 1)

        val actual =
            service.getHistory(
                EXTERNAL_USER_ID,
                from,
                from.plusDays(NotificationHistoryServiceImpl.MAX_RANGE_DAYS - 1),
            )

        assertTrue(actual.days.isEmpty())
        verify(waterStatisticAccessService)
            .findByUserAndTypesAndEventTimeBetween(any(), any(), any(), any(), any())
    }

    @Test
    @DisplayName("updateEntry(): stores the confirmed amount and returns the updated entry")
    fun `updateEntry stores confirmed amount`() {
        val stored =
            entry(
                eventTime = LocalDateTime.of(2026, 5, 10, 8, 0, 0),
                eventType = AnswerNotificationType.CANCEL,
                waterAmountMl = 0,
            )
        stubStoredEntry(stored)
        stubSaveEcho()

        val actual = service.updateEntry(EXTERNAL_USER_ID, ENTRY_ID, NotificationHistoryStatus.CONFIRMED, 300)

        // Only the status and the volume change, every other field of the stored record is kept as is.
        verify(waterStatisticAccessService)
            .save(stored.copy(eventType = AnswerNotificationType.YES, waterAmountMl = 300))
        assertEquals(
            DtoGenerator.generateNotificationHistoryEvent(
                id = ENTRY_ID,
                eventTime = Instant.parse("2026-05-10T08:00:00Z"),
                status = NotificationHistoryStatus.CONFIRMED,
                amountMl = 300,
            ),
            actual,
        )
        // The record arrives with its owner, so the settings are never read on this path.
        verifyNoInteractions(notificationAccessService)
    }

    @ParameterizedTest(name = "[{index}] requested amountMl={0} is ignored")
    @NullSource
    @ValueSource(ints = [300])
    @DisplayName("updateEntry(): stores zero millilitres for a missed entry")
    fun `updateEntry zeroes amount on missed`(amountMl: Int?) {
        val stored = entry(eventTime = LocalDateTime.of(2026, 5, 10, 8, 0, 0), waterAmountMl = 300)
        stubStoredEntry(stored)
        stubSaveEcho()

        val actual = service.updateEntry(EXTERNAL_USER_ID, ENTRY_ID, NotificationHistoryStatus.MISSED, amountMl)

        verify(waterStatisticAccessService)
            .save(stored.copy(eventType = AnswerNotificationType.CANCEL, waterAmountMl = 0))
        assertEquals(0, actual.amountMl)
        assertEquals(NotificationHistoryStatus.MISSED, actual.status)
    }

    @ParameterizedTest(name = "[{index}] amountMl={0}")
    @ValueSource(ints = [50, 1000])
    @DisplayName("updateEntry(): accepts the inclusive bounds of the allowed amount range")
    fun `updateEntry accepts the amount range bounds`(amountMl: Int) {
        val stored =
            entry(
                eventTime = LocalDateTime.of(2026, 5, 10, 8, 0, 0),
                eventType = AnswerNotificationType.CANCEL,
                waterAmountMl = 0,
            )
        stubStoredEntry(stored)
        stubSaveEcho()

        val actual = service.updateEntry(EXTERNAL_USER_ID, ENTRY_ID, NotificationHistoryStatus.CONFIRMED, amountMl)

        assertEquals(amountMl, actual.amountMl)
    }

    @Test
    @DisplayName("updateEntry(): rejects a confirmed status without an amount")
    fun `updateEntry rejects confirmed without amount`() {
        assertThrows<IllegalArgumentException> {
            service.updateEntry(EXTERNAL_USER_ID, ENTRY_ID, NotificationHistoryStatus.CONFIRMED, null)
        }

        verify(waterStatisticAccessService, never()).save(any())
    }

    // The service is the only place bounding the volume: the request DTO carries no constraint annotations,
    // because a missed entry ignores the amount and clients send the whole form snapshot, zero included.
    @ParameterizedTest(name = "[{index}] amountMl={0}")
    @ValueSource(ints = [49, 1001])
    @DisplayName("updateEntry(): rejects a confirmed amount outside the allowed range")
    fun `updateEntry rejects an amount out of range`(amountMl: Int) {
        assertThrows<IllegalArgumentException> {
            service.updateEntry(EXTERNAL_USER_ID, ENTRY_ID, NotificationHistoryStatus.CONFIRMED, amountMl)
        }

        verifyNoInteractions(waterStatisticAccessService)
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideInvisibleEntries")
    @DisplayName("updateEntry(): reports an entry the journal never shows as missing")
    fun `updateEntry rejects invisible entries`(stored: WaterStatisticDto?) {
        stubStoredEntry(stored)

        assertThrows<NotificationHistoryEntryNotFoundException> {
            service.updateEntry(EXTERNAL_USER_ID, ENTRY_ID, NotificationHistoryStatus.CONFIRMED, 300)
        }

        verify(waterStatisticAccessService, never()).save(any())
    }

    // Same cases as the 'editable' flag of getHistory above: the flag and the refusal to update are one
    // predicate, so an entry the journal shows as locked is exactly the entry the update refuses.
    @ParameterizedTest(name = "[{index}] {0}, eventTime={1} -> editable={2}")
    @MethodSource("provideEditWindowCases")
    @DisplayName("updateEntry(): refuses exactly the entries the journal marks read-only")
    fun `updateEntry follows the editable flag`(
        zone: String,
        eventTime: LocalDateTime,
        expectedEditable: Boolean,
    ) {
        stubStoredEntry(entry(eventTime = eventTime, userTimeZone = zone))
        stubSaveEcho()

        if (expectedEditable) {
            val actual = service.updateEntry(EXTERNAL_USER_ID, ENTRY_ID, NotificationHistoryStatus.MISSED, null)

            assertTrue(actual.editable)
            assertEquals(0, actual.amountMl)
            verify(waterStatisticAccessService).save(any())
        } else {
            assertThrows<NotificationHistoryEntryNotEditableException> {
                service.updateEntry(EXTERNAL_USER_ID, ENTRY_ID, NotificationHistoryStatus.MISSED, null)
            }

            verify(waterStatisticAccessService, never()).save(any())
        }
        // The window is measured in the timezone carried by the entry itself, without a lookup of the settings:
        // the last second of May 2nd in New York, for one, is already May 3rd in UTC.
        verifyNoInteractions(notificationAccessService)
    }

    companion object {
        private const val EXTERNAL_USER_ID = 1L
        private const val ENTRY_ID = 42L
        private val NOW = Instant.parse("2026-05-10T12:00:00Z")
        private val DEFAULT_EVENT_TIME = LocalDateTime.of(2026, 5, 10, 8, 0, 0)

        // Wide enough to cover every edit window case below; the access layer is stubbed, so it does not filter.
        private val WINDOW_CASE_FROM = LocalDate.of(2026, 5, 1)
        private val WINDOW_CASE_TO = LocalDate.of(2026, 5, 10)

        // A persisted entry always arrives with its owner, so it carries the very timezone the settings hold.
        private fun entry(
            id: Long = ENTRY_ID,
            eventTime: LocalDateTime = DEFAULT_EVENT_TIME,
            eventType: AnswerNotificationType = AnswerNotificationType.YES,
            waterAmountMl: Int = 250,
            source: WaterEntrySourceType = WaterEntrySourceType.NOTIFICATION,
            userTimeZone: String = "UTC",
        ): WaterStatisticDto =
            DtoGenerator.generateWaterStatisticDto(
                id = id,
                externalUserId = EXTERNAL_USER_ID,
                eventTime = eventTime,
                eventType = eventType,
                waterAmountMl = waterAmountMl,
                source = source,
                userTimeZone = userTimeZone,
            )

        @JvmStatic
        fun provideZoneBoundaryCases(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    Named.of("UTC - no offset", "UTC"),
                    LocalDate.of(2026, 5, 4),
                    LocalDate.of(2026, 5, 10),
                    LocalDateTime.of(2026, 5, 4, 0, 0),
                    LocalDateTime.of(2026, 5, 11, 0, 0),
                ),
                Arguments.of(
                    Named.of("Europe/Moscow - UTC+3, no DST", "Europe/Moscow"),
                    LocalDate.of(2026, 5, 4),
                    LocalDate.of(2026, 5, 10),
                    LocalDateTime.of(2026, 5, 3, 21, 0),
                    LocalDateTime.of(2026, 5, 10, 21, 0),
                ),
                Arguments.of(
                    Named.of("Asia/Kolkata - UTC+5:30, fractional offset", "Asia/Kolkata"),
                    LocalDate.of(2026, 5, 4),
                    LocalDate.of(2026, 5, 10),
                    LocalDateTime.of(2026, 5, 3, 18, 30),
                    LocalDateTime.of(2026, 5, 10, 18, 30),
                ),
                Arguments.of(
                    Named.of("America/New_York - DST spring-forward", "America/New_York"),
                    LocalDate.of(2026, 3, 7),
                    LocalDate.of(2026, 3, 9),
                    LocalDateTime.of(2026, 3, 7, 5, 0),
                    LocalDateTime.of(2026, 3, 10, 4, 0),
                ),
                Arguments.of(
                    Named.of("America/New_York - DST fall-back", "America/New_York"),
                    LocalDate.of(2026, 10, 31),
                    LocalDate.of(2026, 11, 2),
                    LocalDateTime.of(2026, 10, 31, 4, 0),
                    LocalDateTime.of(2026, 11, 3, 5, 0),
                ),
            )

        // With the clock fixed at 2026-05-10T12:00:00Z the local day is May 10th in every zone below,
        // so the oldest editable local date is May 3rd and May 2nd is already locked.
        // Event times are stored in UTC; the cases pair the last second of May 2nd with the first second
        // of May 3rd of each zone, which is where the window flips.
        @JvmStatic
        fun provideEditWindowCases(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    Named.of("UTC - last second of May 2nd", "UTC"),
                    LocalDateTime.of(2026, 5, 2, 23, 59, 59),
                    false,
                ),
                Arguments.of(
                    Named.of("UTC - first second of May 3rd", "UTC"),
                    LocalDateTime.of(2026, 5, 3, 0, 0, 0),
                    true,
                ),
                Arguments.of(
                    Named.of("UTC - last second of May 3rd", "UTC"),
                    LocalDateTime.of(2026, 5, 3, 23, 59, 59),
                    true,
                ),
                Arguments.of(
                    Named.of("UTC - the current moment", "UTC"),
                    LocalDateTime.of(2026, 5, 10, 12, 0, 0),
                    true,
                ),
                Arguments.of(
                    Named.of("Europe/Moscow - May 2nd 23:59:59 local", "Europe/Moscow"),
                    LocalDateTime.of(2026, 5, 2, 20, 59, 59),
                    false,
                ),
                Arguments.of(
                    Named.of("Europe/Moscow - May 3rd 00:00 local, still May 2nd in UTC", "Europe/Moscow"),
                    LocalDateTime.of(2026, 5, 2, 21, 0, 0),
                    true,
                ),
                Arguments.of(
                    Named.of("Asia/Kolkata - May 2nd 23:59:59 local, half-hour offset", "Asia/Kolkata"),
                    LocalDateTime.of(2026, 5, 2, 18, 29, 59),
                    false,
                ),
                Arguments.of(
                    Named.of("Asia/Kolkata - May 3rd 00:00 local, half-hour offset", "Asia/Kolkata"),
                    LocalDateTime.of(2026, 5, 2, 18, 30, 0),
                    true,
                ),
                Arguments.of(
                    Named.of("America/New_York - May 2nd 23:59:59 local, already May 3rd in UTC", "America/New_York"),
                    LocalDateTime.of(2026, 5, 3, 3, 59, 59),
                    false,
                ),
                Arguments.of(
                    Named.of("America/New_York - May 3rd 00:00 local", "America/New_York"),
                    LocalDateTime.of(2026, 5, 3, 4, 0, 0),
                    true,
                ),
            )

        // The first and the last second of May 2nd and of May 3rd, in UTC, per zone: two whole local days
        // around the edge of the edit window.
        @JvmStatic
        fun provideLocalDayEdges(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    Named.of("UTC - no offset", "UTC"),
                    listOf(LocalDateTime.of(2026, 5, 2, 0, 0, 0), LocalDateTime.of(2026, 5, 2, 23, 59, 59)),
                    listOf(LocalDateTime.of(2026, 5, 3, 0, 0, 0), LocalDateTime.of(2026, 5, 3, 23, 59, 59)),
                ),
                Arguments.of(
                    Named.of("Europe/Moscow - UTC+3", "Europe/Moscow"),
                    listOf(LocalDateTime.of(2026, 5, 1, 21, 0, 0), LocalDateTime.of(2026, 5, 2, 20, 59, 59)),
                    listOf(LocalDateTime.of(2026, 5, 2, 21, 0, 0), LocalDateTime.of(2026, 5, 3, 20, 59, 59)),
                ),
                Arguments.of(
                    Named.of("Asia/Kolkata - UTC+5:30, fractional offset", "Asia/Kolkata"),
                    listOf(LocalDateTime.of(2026, 5, 1, 18, 30, 0), LocalDateTime.of(2026, 5, 2, 18, 29, 59)),
                    listOf(LocalDateTime.of(2026, 5, 2, 18, 30, 0), LocalDateTime.of(2026, 5, 3, 18, 29, 59)),
                ),
            )

        @JvmStatic
        fun provideInvisibleEntries(): Stream<Named<WaterStatisticDto?>> =
            Stream.of(
                Named.of<WaterStatisticDto?>("no such entry, or it belongs to another user", null),
                Named.of<WaterStatisticDto?>("manual entry", entry(source = WaterEntrySourceType.MANUAL)),
                Named.of<WaterStatisticDto?>("snoozed entry", entry(eventType = AnswerNotificationType.SNOOZE)),
            )
    }
}

package ru.illine.drinking.ponies.service.statistic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import ru.illine.drinking.ponies.dao.access.WaterStatisticAccessService
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.WaterEntrySourceType
import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto
import ru.illine.drinking.ponies.service.statistic.impl.WaterStatisticServiceImpl
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.water.WaterEntryConstants
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@UnitTest
@DisplayName("WaterStatisticService Unit Test")
class WaterStatisticServiceTest {

    private val fixedNow = LocalDateTime.of(2025, 1, 1, 14, 0, 0)
    private val fixedClock = Clock.fixed(fixedNow.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private lateinit var waterStatisticAccessService: WaterStatisticAccessService
    private lateinit var service: WaterStatisticService

    @BeforeEach
    fun setUp() {
        waterStatisticAccessService = mock(WaterStatisticAccessService::class.java)
        service = WaterStatisticServiceImpl(waterStatisticAccessService, fixedClock)
    }

    @Test
    @DisplayName("recordEvent(): saves statistic with correct fields and source=NOTIFICATION")
    fun `recordEvent saves statistic with correct fields`() {
        val telegramUser = DtoGenerator.generateNotificationDto().telegramUser
        val captor = argumentCaptor<WaterStatisticDto>()

        service.recordEvent(telegramUser, AnswerNotificationType.YES, 250)

        verify(waterStatisticAccessService).save(captor.capture())
        val saved = captor.firstValue
        assertEquals(telegramUser, saved.telegramUser)
        assertEquals(AnswerNotificationType.YES, saved.eventType)
        assertEquals(250, saved.waterAmountMl)
        assertEquals(fixedNow, saved.eventTime)
        assertEquals(WaterEntrySourceType.NOTIFICATION, saved.source)
    }

    @Test
    @DisplayName("recordEvent(): defaults waterAmountMl to 0 and uses source=NOTIFICATION")
    fun `recordEvent defaults waterAmountMl to zero`() {
        val telegramUser = DtoGenerator.generateNotificationDto().telegramUser
        val captor = argumentCaptor<WaterStatisticDto>()

        service.recordEvent(telegramUser, AnswerNotificationType.SNOOZE)

        verify(waterStatisticAccessService).save(captor.capture())
        assertEquals(0, captor.firstValue.waterAmountMl)
        assertEquals(AnswerNotificationType.SNOOZE, captor.firstValue.eventType)
        assertEquals(WaterEntrySourceType.NOTIFICATION, captor.firstValue.source)
    }

    @Test
    @DisplayName("recordEvents(): saves all statistics with correct eventType and source=NOTIFICATION")
    fun `recordEvents saves all statistics`() {
        val users = listOf(
            DtoGenerator.generateNotificationDto(externalUserId = 1L).telegramUser,
            DtoGenerator.generateNotificationDto(externalUserId = 2L).telegramUser
        )
        val captor = argumentCaptor<Collection<WaterStatisticDto>>()

        service.recordEvents(users, AnswerNotificationType.CANCEL)

        verify(waterStatisticAccessService).saveAll(captor.capture())
        val saved = captor.firstValue.toList()
        assertEquals(2, saved.size)
        assertEquals(users[0], saved[0].telegramUser)
        assertEquals(users[1], saved[1].telegramUser)
        saved.forEach {
            assertEquals(AnswerNotificationType.CANCEL, it.eventType)
            assertEquals(0, it.waterAmountMl)
            assertEquals(fixedNow, it.eventTime)
            assertEquals(WaterEntrySourceType.NOTIFICATION, it.source)
        }
    }

    @Test
    @DisplayName("manualRecordEvent(): saves statistic with source=MANUAL, eventType=YES and converts consumedAt to UTC LocalDateTime")
    fun `manualRecordEvent saves statistic with manual source`() {
        val externalUserId = 12345L
        val consumedAt = fixedClock.instant().minus(2, ChronoUnit.HOURS)
        val captor = argumentCaptor<WaterStatisticDto>()

        service.manualRecordEvent(externalUserId, consumedAt, 250)

        verify(waterStatisticAccessService).save(captor.capture())
        val saved = captor.firstValue
        assertEquals(externalUserId, saved.telegramUser.externalUserId)
        assertEquals(AnswerNotificationType.YES, saved.eventType)
        assertEquals(250, saved.waterAmountMl)
        assertEquals(WaterEntrySourceType.MANUAL, saved.source)
        assertEquals(LocalDateTime.ofInstant(consumedAt, ZoneOffset.UTC), saved.eventTime)
    }

    @ParameterizedTest(name = "[{index}] amount={0} ml is accepted (boundary)")
    @ValueSource(ints = [50, 250, 1000])
    @DisplayName("manualRecordEvent(): accepts amount within [MIN_ML, MAX_ML]")
    fun `manualRecordEvent accepts amount within bounds`(amount: Int) {
        val captor = argumentCaptor<WaterStatisticDto>()

        service.manualRecordEvent(1L, fixedClock.instant(), amount)

        verify(waterStatisticAccessService).save(captor.capture())
        assertEquals(amount, captor.firstValue.waterAmountMl)
    }

    @ParameterizedTest(name = "[{index}] amount={0} ml is rejected")
    @ValueSource(ints = [Int.MIN_VALUE, -1, 0, 49, 1001, Int.MAX_VALUE])
    @DisplayName("manualRecordEvent(): rejects amount outside [MIN_ML, MAX_ML]")
    fun `manualRecordEvent rejects amount out of bounds`(amount: Int) {
        assertThrows(IllegalArgumentException::class.java) {
            service.manualRecordEvent(1L, fixedClock.instant(), amount)
        }
        verify(waterStatisticAccessService, never()).save(any())
    }

    @Test
    @DisplayName("manualRecordEvent(): rejects consumedAt strictly in the future")
    fun `manualRecordEvent rejects future consumedAt`() {
        val future = fixedClock.instant().plus(1, ChronoUnit.SECONDS)
        assertThrows(IllegalArgumentException::class.java) {
            service.manualRecordEvent(1L, future, 250)
        }
        verify(waterStatisticAccessService, never()).save(any())
    }

    @Test
    @DisplayName("manualRecordEvent(): accepts consumedAt exactly equal to now (boundary)")
    fun `manualRecordEvent accepts consumedAt at now`() {
        val captor = argumentCaptor<WaterStatisticDto>()

        service.manualRecordEvent(1L, fixedClock.instant(), 250)

        verify(waterStatisticAccessService).save(captor.capture())
        assertEquals(fixedNow, captor.firstValue.eventTime)
    }

    @Test
    @DisplayName("manualRecordEvent(): accepts consumedAt exactly MAX_DAYS_AGO ago (boundary)")
    fun `manualRecordEvent accepts consumedAt at max days ago`() {
        val edge = fixedClock.instant().minus(WaterEntryConstants.MAX_DAYS_AGO, ChronoUnit.DAYS)
        val captor = argumentCaptor<WaterStatisticDto>()

        service.manualRecordEvent(1L, edge, 250)

        verify(waterStatisticAccessService).save(captor.capture())
        assertEquals(LocalDateTime.ofInstant(edge, ZoneOffset.UTC), captor.firstValue.eventTime)
    }

    @Test
    @DisplayName("manualRecordEvent(): rejects consumedAt older than MAX_DAYS_AGO")
    fun `manualRecordEvent rejects consumedAt older than max days`() {
        val tooOld = fixedClock.instant()
            .minus(WaterEntryConstants.MAX_DAYS_AGO, ChronoUnit.DAYS)
            .minus(1, ChronoUnit.SECONDS)
        assertThrows(IllegalArgumentException::class.java) {
            service.manualRecordEvent(1L, tooOld, 250)
        }
        verify(waterStatisticAccessService, never()).save(any())
    }

}

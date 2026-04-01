package ru.illine.drinking.ponies.service.statistic

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.argumentCaptor
import org.junit.jupiter.api.Assertions.assertEquals
import ru.illine.drinking.ponies.dao.access.WaterStatisticAccessService
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto
import ru.illine.drinking.ponies.service.statistic.impl.WaterStatisticServiceImpl
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset

@UnitTest
@DisplayName("WaterStatisticService Unit Test")
class WaterStatisticServiceTest {

    private val fixedNow = LocalDateTime.of(2025, 1, 1, 14, 0, 0)
    private val fixedClock = Clock.fixed(fixedNow.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private lateinit var waterStatisticAccessService: WaterStatisticAccessService
    private lateinit var service: WaterStatisticServiceImpl

    @BeforeEach
    fun setUp() {
        waterStatisticAccessService = mock(WaterStatisticAccessService::class.java)
        service = WaterStatisticServiceImpl(waterStatisticAccessService, fixedClock)
    }

    @Test
    @DisplayName("recordEvent(): saves statistic with correct eventType and waterAmountMl")
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
    }

    @Test
    @DisplayName("recordEvent(): defaults waterAmountMl to 0")
    fun `recordEvent defaults waterAmountMl to zero`() {
        val telegramUser = DtoGenerator.generateNotificationDto().telegramUser
        val captor = argumentCaptor<WaterStatisticDto>()

        service.recordEvent(telegramUser, AnswerNotificationType.SNOOZE)

        verify(waterStatisticAccessService).save(captor.capture())
        assertEquals(0, captor.firstValue.waterAmountMl)
    }

    @Test
    @DisplayName("recordEvents(): saves all statistics with correct eventType")
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
        saved.forEach {
            assertEquals(AnswerNotificationType.CANCEL, it.eventType)
            assertEquals(0, it.waterAmountMl)
            assertEquals(fixedNow, it.eventTime)
        }
    }

}
package ru.illine.drinking.ponies.dao.access

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.function.ThrowingSupplier
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlConfig
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.WaterAmountType
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest
import java.time.LocalDateTime

@SpringIntegrationTest
@DisplayName("WaterStatisticAccessService Spring Integration Test")
@Sql(
    scripts = ["classpath:sql/access/WaterStatisticAccessService.sql"],
    config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
    scripts = ["classpath:sql/clear.sql"],
    config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class WaterStatisticAccessServiceTest @Autowired constructor(
    private val accessService: WaterStatisticAccessService,
) {

    private val DEFAULT_EXTERNAL_USER_ID = 1L
    private val SECOND_EXTERNAL_USER_ID = 2L
    private val NOT_EXISTED_USER_ID = 0L

    @Test
    @DisplayName("save(): returns a saved record with id")
    fun `successful save`() {
        val expectedWaterAmount = WaterAmountType.ML_150.amountMl
        val dto = DtoGenerator.generateWaterStatisticDto(
            externalUserId = DEFAULT_EXTERNAL_USER_ID,
            waterAmountMl = expectedWaterAmount
        )

        val actual = assertDoesNotThrow(ThrowingSupplier { accessService.save(dto) })

        assertNotNull(actual.id)
        assertNotNull(actual.eventTime)
        assertEquals(DEFAULT_EXTERNAL_USER_ID, actual.telegramUser.externalUserId)
        assertEquals(AnswerNotificationType.YES, actual.eventType)
        assertEquals(expectedWaterAmount, actual.waterAmountMl)
    }

    @Test
    @DisplayName("save(): throws IllegalArgumentException when user not found")
    fun `failure save user not found`() {
        val dto = DtoGenerator.generateWaterStatisticDto(externalUserId = NOT_EXISTED_USER_ID)

        assertThrows<IllegalArgumentException> { accessService.save(dto) }
    }

    @Test
    @DisplayName("saveAll(): returns a list of saved records with correct data")
    fun `successful saveAll`() {
        val statistics = listOf(
            DtoGenerator.generateWaterStatisticDto(externalUserId = DEFAULT_EXTERNAL_USER_ID, eventType = AnswerNotificationType.YES, waterAmountMl = 250),
            DtoGenerator.generateWaterStatisticDto(externalUserId = SECOND_EXTERNAL_USER_ID, eventType = AnswerNotificationType.CANCEL, waterAmountMl = 0)
        )

        val actual = assertDoesNotThrow(ThrowingSupplier { accessService.saveAll(statistics) })

        assertEquals(2, actual.size)

        val sorted = actual.sortedBy { it.telegramUser.externalUserId }
        
        assertEquals(DEFAULT_EXTERNAL_USER_ID, sorted[0].telegramUser.externalUserId)
        assertEquals(AnswerNotificationType.YES, sorted[0].eventType)
        assertEquals(250, sorted[0].waterAmountMl)
        assertEquals(SECOND_EXTERNAL_USER_ID, sorted[1].telegramUser.externalUserId)
        assertEquals(AnswerNotificationType.CANCEL, sorted[1].eventType)
        assertEquals(0, sorted[1].waterAmountMl)
    }

    @Test
    @DisplayName("saveAll(): returns empty list for empty input")
    fun `successful saveAll empty`() {
        val actual = assertDoesNotThrow(ThrowingSupplier<List<*>> { accessService.saveAll(emptyList()) })

        assertTrue(actual.isEmpty())
    }

    @Test
    @DisplayName("saveAll(): skips unknown users and returns empty list")
    fun `successful saveAll skips unknown users`() {
        val statistics = listOf(DtoGenerator.generateWaterStatisticDto(externalUserId = NOT_EXISTED_USER_ID))

        val actual = assertDoesNotThrow(ThrowingSupplier<List<*>> { accessService.saveAll(statistics) })

        assertTrue(actual.isEmpty())
    }

    @Test
    @DisplayName("saveAll(): saves only known users and skips unknown")
    fun `successful saveAll partial`() {
        val statistics = listOf(
            DtoGenerator.generateWaterStatisticDto(externalUserId = DEFAULT_EXTERNAL_USER_ID),
            DtoGenerator.generateWaterStatisticDto(externalUserId = NOT_EXISTED_USER_ID)
        )

        val actual = assertDoesNotThrow(ThrowingSupplier { accessService.saveAll(statistics) })

        assertEquals(1, actual.size)
        assertEquals(DEFAULT_EXTERNAL_USER_ID, actual[0].telegramUser.externalUserId)
    }

    @Test
    @DisplayName("findByUserAndEventTimeBetween(): returns records inside the half-open range")
    fun `successful findByUserAndEventTimeBetween returns records inside range`() {
        val baseTime = LocalDateTime.of(2025, 6, 15, 10, 0)
        accessService.save(
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = DEFAULT_EXTERNAL_USER_ID,
                eventTime = baseTime,
                waterAmountMl = WaterAmountType.ML_150.amountMl
            )
        )
        accessService.save(
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = DEFAULT_EXTERNAL_USER_ID,
                eventTime = baseTime.plusHours(2),
                waterAmountMl = WaterAmountType.ML_250.amountMl
            )
        )

        val actual = assertDoesNotThrow(
            ThrowingSupplier {
                accessService.findByUserAndEventTimeBetween(
                    DEFAULT_EXTERNAL_USER_ID,
                    baseTime,
                    baseTime.plusHours(3)
                )
            }
        )

        assertEquals(2, actual.size)
        assertEquals(WaterAmountType.ML_150.amountMl, actual[0].waterAmountMl)
        assertEquals(WaterAmountType.ML_250.amountMl, actual[1].waterAmountMl)
        assertEquals(DEFAULT_EXTERNAL_USER_ID, actual[0].telegramUser.externalUserId)
    }

    @Test
    @DisplayName("findByUserAndEventTimeBetween(): excludes records on endExclusive boundary")
    fun `successful findByUserAndEventTimeBetween excludes endExclusive`() {
        val start = LocalDateTime.of(2025, 6, 15, 10, 0)
        val end = LocalDateTime.of(2025, 6, 15, 12, 0)
        // exactly at endExclusive - must be excluded
        accessService.save(
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = DEFAULT_EXTERNAL_USER_ID,
                eventTime = end
            )
        )
        // before endExclusive - must be included
        accessService.save(
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = DEFAULT_EXTERNAL_USER_ID,
                eventTime = end.minusSeconds(1)
            )
        )

        val actual = assertDoesNotThrow(
            ThrowingSupplier {
                accessService.findByUserAndEventTimeBetween(DEFAULT_EXTERNAL_USER_ID, start, end)
            }
        )

        assertEquals(1, actual.size)
        assertEquals(end.minusSeconds(1), actual[0].eventTime)
    }

    @Test
    @DisplayName("findByUserAndEventTimeBetween(): returns empty list when no records match")
    fun `successful findByUserAndEventTimeBetween returns empty when out of range`() {
        accessService.save(
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = DEFAULT_EXTERNAL_USER_ID,
                eventTime = LocalDateTime.of(2025, 6, 15, 10, 0)
            )
        )

        val actual = assertDoesNotThrow(
            ThrowingSupplier {
                accessService.findByUserAndEventTimeBetween(
                    DEFAULT_EXTERNAL_USER_ID,
                    LocalDateTime.of(2025, 7, 1, 0, 0),
                    LocalDateTime.of(2025, 7, 2, 0, 0)
                )
            }
        )

        assertTrue(actual.isEmpty())
    }

    @Test
    @DisplayName("findByUserAndEventTimeBetween(): returns only records of the requested user")
    fun `successful findByUserAndEventTimeBetween filters by user`() {
        val time = LocalDateTime.of(2025, 6, 15, 10, 0)
        accessService.save(
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = DEFAULT_EXTERNAL_USER_ID,
                eventTime = time
            )
        )
        accessService.save(
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = SECOND_EXTERNAL_USER_ID,
                eventTime = time
            )
        )

        val actual = assertDoesNotThrow(
            ThrowingSupplier {
                accessService.findByUserAndEventTimeBetween(
                    DEFAULT_EXTERNAL_USER_ID,
                    time.minusMinutes(1),
                    time.plusMinutes(1)
                )
            }
        )

        assertEquals(1, actual.size)
        assertEquals(DEFAULT_EXTERNAL_USER_ID, actual[0].telegramUser.externalUserId)
    }

    @Test
    @DisplayName("findByUserAndEventTimeBetween(): returns empty list for unknown user")
    fun `successful findByUserAndEventTimeBetween unknown user`() {
        val actual = assertDoesNotThrow(
            ThrowingSupplier {
                accessService.findByUserAndEventTimeBetween(
                    NOT_EXISTED_USER_ID,
                    LocalDateTime.of(2025, 6, 15, 0, 0),
                    LocalDateTime.of(2025, 6, 16, 0, 0)
                )
            }
        )

        assertTrue(actual.isEmpty())
    }

    @Test
    @DisplayName("findByUserAndEventTimeBetween(): returns records ordered by eventTime asc")
    fun `successful findByUserAndEventTimeBetween orders ascending`() {
        val baseTime = LocalDateTime.of(2025, 6, 15, 10, 0)
        accessService.save(
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = DEFAULT_EXTERNAL_USER_ID,
                eventTime = baseTime.plusHours(2),
                waterAmountMl = WaterAmountType.ML_450.amountMl
            )
        )
        accessService.save(
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = DEFAULT_EXTERNAL_USER_ID,
                eventTime = baseTime,
                waterAmountMl = WaterAmountType.ML_150.amountMl
            )
        )
        accessService.save(
            DtoGenerator.generateWaterStatisticDto(
                externalUserId = DEFAULT_EXTERNAL_USER_ID,
                eventTime = baseTime.plusHours(1),
                waterAmountMl = WaterAmountType.ML_250.amountMl
            )
        )

        val actual = accessService.findByUserAndEventTimeBetween(
            DEFAULT_EXTERNAL_USER_ID, baseTime, baseTime.plusHours(3)
        )

        assertEquals(3, actual.size)
        assertEquals(baseTime, actual[0].eventTime)
        assertEquals(baseTime.plusHours(1), actual[1].eventTime)
        assertEquals(baseTime.plusHours(2), actual[2].eventTime)
    }

}

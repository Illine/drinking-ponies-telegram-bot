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
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest

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
        val dto = DtoGenerator.generateWaterStatisticDto(externalUserId = DEFAULT_EXTERNAL_USER_ID)

        val actual = assertDoesNotThrow(ThrowingSupplier { accessService.save(dto) })

        assertNotNull(actual.id)
        assertEquals(DEFAULT_EXTERNAL_USER_ID, actual.telegramUser.externalUserId)
        assertEquals(AnswerNotificationType.YES, actual.eventType)
        assertEquals(250, actual.waterAmountMl)
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

}

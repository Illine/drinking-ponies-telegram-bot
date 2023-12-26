package ru.illine.drinking.ponies.dao.access

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.function.ThrowingSupplier
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlConfig
import ru.illine.drinking.ponies.test.generator.DtoGenerator.Companion.generateNotificationDto
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest
import java.time.LocalDateTime

@SpringIntegrationTest
@DisplayName("NotificationAccessService Spring Integration Test")
@Sql(
    scripts = ["classpath:sql/access/NotificationAccessService.sql"],
    config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
    scripts = ["classpath:sql/clear.sql"],
    config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class NotificationAccessServiceTest @Autowired constructor(
    private val accessService: NotificationAccessService,
) {

    private val DEFAULT_ID = 1L
    private val NOT_EXISTED_USER_ID = 0L
    private val DEFAULT_USER_ID = 1L
    private val DELETED_USER_ID = 2L
    private val WITHOUT_NOTIFICATION_ATTEMPTS = 0

    //  -----------------------   successful tests   -------------------------

    // findAll

    @Test
    @DisplayName("findAll(): returns a not empty set")
    fun `successful findAll`() {
        assertFalse(accessService.findAll().isEmpty())
    }

    // findByUserId

    @Test
    @DisplayName("findByUserId(): returns a found record")
    fun `successful findByUserId`() {
        assertDoesNotThrow { accessService.findByUserId(DEFAULT_USER_ID) }
    }

    // existsByUserId

    @Test
    @DisplayName("existsByUserId(): returns a true")
    fun `successful existsByUserId true`() {
        val actual =
            assertDoesNotThrow(
                ThrowingSupplier {
                    accessService.existsByUserId(DEFAULT_USER_ID)
                }
            )
        assertTrue(actual)
    }

    @Test
    @DisplayName("existsByUserId(): returns a false")
    fun `successful existsByUserId false`() {
        val actual =
            assertDoesNotThrow(
                ThrowingSupplier {
                    accessService.existsByUserId(NOT_EXISTED_USER_ID)
                }
            )
        assertFalse(actual)
    }

    // save

    @Test
    @DisplayName("save(): returns a new record")
    fun `successful save new`() {
        val notificationDto = generateNotificationDto()

        val actual =
            assertDoesNotThrow(
                ThrowingSupplier {
                    accessService.save(notificationDto)
                }
            )
        assertNotNull(actual.id)
        assertNotEquals(DEFAULT_USER_ID, actual.userId)
    }

    @Test
    @DisplayName("save(): returns an existed record")
    fun `successful save update`() {
        val notificationDto = generateNotificationDto(userId = DEFAULT_USER_ID)

        val actual =
            assertDoesNotThrow(
                ThrowingSupplier {
                    accessService.save(notificationDto)
                }
            )
        assertEquals(DEFAULT_ID, actual.id)
        assertEquals(DEFAULT_USER_ID, actual.userId)
    }

    // updateTimeOfLastNotification

    @Test
    @DisplayName("updateTimeOfLastNotification(): returns an updated record")
    fun `successful updateTimeOfLastNotification`() {
        val time = LocalDateTime.now()

        val actual =
            assertDoesNotThrow(
                ThrowingSupplier {
                    accessService.updateTimeOfLastNotification(DEFAULT_USER_ID, time)
                }
            )

        assertEquals(time, actual.timeOfLastNotification)
        assertEquals(WITHOUT_NOTIFICATION_ATTEMPTS, actual.notificationAttempts)
    }

    // updateNotifications

    @Test
    @DisplayName("updateTimeOfLastNotification(): returns an updated set of records")
    fun `successful updateNotifications`() {
        val existed = accessService.findByUserId(DEFAULT_USER_ID)
        val previousUpdated = existed.updated

        val forUpdate = setOf(existed)

        val actual =
            assertDoesNotThrow(
                ThrowingSupplier {
                    accessService.updateNotifications(forUpdate)
                }
            )

        assertNotEquals(previousUpdated, actual.first().updated)
    }

    // enableByUserId

    @Test
    @DisplayName("enableByUserId(): changed 'deleted' flag as true")
    fun `successful enableByUserId`() {
        assertDoesNotThrow(
                ThrowingSupplier {
                    accessService.enableByUserId(DELETED_USER_ID)
                }
            )
        val actual = accessService.findByUserId(DELETED_USER_ID)
        assertFalse(actual.deleted)
    }

    // disableByUserId

    @Test
    @DisplayName("disableByUserId(): changed 'deleted' flag as false")
    fun `successful disableByUserId`() {
        assertDoesNotThrow(
            ThrowingSupplier {
                accessService.disableByUserId(DEFAULT_USER_ID)
            }
        )
        val actual = accessService.findByUserId(DEFAULT_USER_ID)
        assertTrue(actual.deleted)
    }

    //  -----------------------   failure tests   -------------------------

    // findByUserId

    @Test
    @DisplayName("findByUserId(): throws IllegalArgumentException when record bot found by userId")
    fun `failure findByUserId not found`() {
        assertThrows<IllegalArgumentException> { accessService.findByUserId(NOT_EXISTED_USER_ID) }
    }

    // updateTimeOfLastNotification

    @Test
    @DisplayName("updateTimeOfLastNotification(): throws IllegalArgumentException when record bot found by userId")
    fun `failure updateTimeOfLastNotification not found`() {
        val time = LocalDateTime.now()
        assertThrows<IllegalArgumentException> { accessService.updateTimeOfLastNotification(NOT_EXISTED_USER_ID, time) }
    }
}
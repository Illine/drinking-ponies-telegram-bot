package ru.illine.drinking.ponies.dao.access

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.function.ThrowingSupplier
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlConfig
import ru.illine.drinking.ponies.model.base.IntervalNotificationType
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest
import java.time.LocalDateTime
import java.time.LocalTime

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
    private val DEFAULT_TELEGRAM_USER_ID = 1L
    private val DISABLED_TELEGRAM_USER_ID = 2L
    private val WITHOUT_NOTIFICATION_ATTEMPTS = 0

    @Test
    @DisplayName("findAllNotificationSettings(): returns a not empty set")
    fun `successful findAllNotificationSettings`() {
        assertFalse(accessService.findAllNotificationSettings().isEmpty())
    }

    @Test
    @DisplayName("findNotificationSettingByTelegramUserId(): returns a found record")
    fun `successful findNotificationSettingByTelegramUserId`() {
        assertDoesNotThrow { accessService.findNotificationSettingByTelegramUserId(DEFAULT_TELEGRAM_USER_ID) }
    }

    @Test
    @DisplayName("existsByTelegramUserId(): returns a true")
    fun `successful existsByTelegramUserId true`() {
        val actual =
            assertDoesNotThrow(
                ThrowingSupplier {
                    accessService.existsByTelegramUserId(DEFAULT_TELEGRAM_USER_ID)
                }
            )
        assertTrue(actual)
    }

    @Test
    @DisplayName("existsByTelegramUserId(): returns a false")
    fun `successful existsByTelegramUserId false`() {
        val actual =
            assertDoesNotThrow(
                ThrowingSupplier {
                    accessService.existsByTelegramUserId(NOT_EXISTED_USER_ID)
                }
            )
        assertFalse(actual)
    }

    @Test
    @DisplayName("save(): returns a new record")
    fun `successful save new`() {
        val dto = DtoGenerator.generateNotificationDto()

        val actual =
            assertDoesNotThrow(
                ThrowingSupplier {
                    accessService.save(dto.telegramUser, dto.telegramChat, dto)
                }
            )
        assertNotNull(actual.id)
        assertNotEquals(DEFAULT_TELEGRAM_USER_ID, actual.externalUserId)
    }

    @Test
    @DisplayName("save(): returns an existed record")
    fun `successful save update`() {
        val dto = DtoGenerator.generateNotificationDto(externalUserId = DEFAULT_TELEGRAM_USER_ID)

        val actual =
            assertDoesNotThrow(
                ThrowingSupplier {
                    accessService.save(dto.telegramUser, dto.telegramChat, dto)
                }
            )
        assertEquals(DEFAULT_ID, actual.id)
        assertEquals(DEFAULT_TELEGRAM_USER_ID, actual.externalUserId)
    }

    @Test
    @DisplayName("save(): reuses existing chat entity when externalChatId already exists")
    fun `successful save with existing chat`() {
        val dto = DtoGenerator.generateNotificationDto(
            externalUserId = DEFAULT_TELEGRAM_USER_ID,
            externalChatId = DEFAULT_TELEGRAM_USER_ID
        )

        val actual =
            assertDoesNotThrow(
                ThrowingSupplier {
                    accessService.save(dto.telegramUser, dto.telegramChat, dto)
                }
            )
        assertEquals(DEFAULT_TELEGRAM_USER_ID, actual.externalUserId)
    }

    @Test
    @DisplayName("updateTimeOfLastNotification(): returns an updated record")
    fun `successful updateTimeOfLastNotification`() {
        val time = LocalDateTime.now()

        val actual =
            assertDoesNotThrow(
                ThrowingSupplier {
                    accessService.updateTimeOfLastNotification(DEFAULT_TELEGRAM_USER_ID, time)
                }
            )

        assertEquals(time, actual.timeOfLastNotification)
        assertEquals(WITHOUT_NOTIFICATION_ATTEMPTS, actual.notificationAttempts)
    }

    @Test
    @DisplayName("updateNotificationSettings(): returns an updated set of records")
    fun `successful updateNotificationSettings`() {
        val existed = accessService.findNotificationSettingByTelegramUserId(DEFAULT_TELEGRAM_USER_ID)

        val actual =
            assertDoesNotThrow(
                ThrowingSupplier {
                    accessService.updateNotificationSettings(setOf(existed))
                }
            )

        assertFalse(actual.isEmpty())
        assertEquals(existed.id, actual.first().id)
    }

    @Test
    @DisplayName("enableNotifications(): changed 'enabled' flag as true")
    fun `successful enableNotifications`() {
        assertDoesNotThrow(
            ThrowingSupplier {
                accessService.enableNotifications(DISABLED_TELEGRAM_USER_ID)
            }
        )
        assertTrue(accessService.isEnabledNotifications(DISABLED_TELEGRAM_USER_ID))
    }

    @Test
    @DisplayName("disableNotifications(): changed 'enabled' flag as false")
    fun `successful disableNotifications`() {
        assertDoesNotThrow(
            ThrowingSupplier {
                accessService.disableNotifications(DEFAULT_TELEGRAM_USER_ID)
            }
        )
        assertFalse(accessService.isEnabledNotifications(DEFAULT_TELEGRAM_USER_ID))
    }

    @Test
    @DisplayName("updateNotificationSettings(): updates interval when it differs from current")
    fun `successful updateNotificationSettings changes interval`() {
        val newInterval = IntervalNotificationType.HALF_HOUR

        val actual = assertDoesNotThrow(
            ThrowingSupplier {
                accessService.updateNotificationSettings(DEFAULT_TELEGRAM_USER_ID, newInterval)
            }
        )

        assertEquals(newInterval, actual.notificationInterval)
    }

    @Test
    @DisplayName("updateNotificationSettings(): does not update when interval is the same")
    fun `successful updateNotificationSettings same interval`() {
        val sameInterval = IntervalNotificationType.TWO_HOURS

        val actual = assertDoesNotThrow(
            ThrowingSupplier {
                accessService.updateNotificationSettings(DEFAULT_TELEGRAM_USER_ID, sameInterval)
            }
        )

        assertEquals(sameInterval, actual.notificationInterval)
    }

    @Test
    @DisplayName("changeQuietMode(): updates quiet mode without exception")
    fun `successful changeQuietMode`() {
        assertDoesNotThrow(
            ThrowingSupplier {
                accessService.changeQuietMode(DEFAULT_TELEGRAM_USER_ID, LocalTime.of(22, 0), LocalTime.of(8, 0))
            }
        )
    }

    @Test
    @DisplayName("disableQuietMode(): disables quiet mode without exception")
    fun `successful disableQuietMode`() {
        assertDoesNotThrow(
            ThrowingSupplier {
                accessService.disableQuietMode(DEFAULT_TELEGRAM_USER_ID)
            }
        )
    }

    @Test
    @DisplayName("findNotificationSettingByTelegramUserId(): throws IllegalArgumentException when record not found by telegramUserId")
    fun `failure findNotificationSettingByTelegramUserId not found`() {
        assertThrows<IllegalArgumentException> { accessService.findNotificationSettingByTelegramUserId(NOT_EXISTED_USER_ID) }
    }

    @Test
    @DisplayName("updateTimeOfLastNotification(): throws IllegalArgumentException when record not found by telegramUserId")
    fun `failure updateTimeOfLastNotification not found`() {
        val time = LocalDateTime.now()
        assertThrows<IllegalArgumentException> { accessService.updateTimeOfLastNotification(NOT_EXISTED_USER_ID, time) }
    }

    @Test
    @DisplayName("updateNotificationSettings(): throws IllegalArgumentException when record not found by telegramUserId")
    fun `failure updateNotificationSettings not found`() {
        assertThrows<IllegalArgumentException> {
            accessService.updateNotificationSettings(NOT_EXISTED_USER_ID, IntervalNotificationType.HOUR)
        }
    }
}

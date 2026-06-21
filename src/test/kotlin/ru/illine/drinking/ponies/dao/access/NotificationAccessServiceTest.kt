package ru.illine.drinking.ponies.dao.access

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.function.ThrowingSupplier
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlConfig
import ru.illine.drinking.ponies.exception.NotificationSettingsNotFoundException
import ru.illine.drinking.ponies.model.base.IntervalNotificationType
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest
import ru.illine.drinking.ponies.test.util.ClockHelperTest
import java.time.Clock
import java.time.LocalDateTime
import java.time.LocalTime

@SpringIntegrationTest
@DisplayName("NotificationAccessService Spring Integration Test")
@Sql(
    scripts = ["classpath:sql/access/NotificationAccessService.sql"],
    config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
)
@Sql(
    scripts = ["classpath:sql/clear.sql"],
    config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
)
class NotificationAccessServiceTest
    @Autowired
    constructor(
        private val accessService: NotificationAccessService,
        private val clock: Clock,
    ) {
        private fun getMutableClock() = clock as ClockHelperTest.MutableClock

        @BeforeEach
        fun resetClock() {
            getMutableClock().setTime(ClockHelperTest.DEFAULT_TIME)
        }

        @Test
        @DisplayName("findAllNotificationSettings(): returns a not empty set")
        fun `successful findAllNotificationSettings`() {
            assertFalse(accessService.findAllNotificationSettings().isEmpty())
        }

        @Test
        @DisplayName("findNotificationSettingByExternalUserId(): returns a found record")
        fun `successful findNotificationSettingByExternalUserId`() {
            assertDoesNotThrow { accessService.findNotificationSettingByExternalUserId(DEFAULT_EXTERNAL_USER_ID) }
        }

        @Test
        @DisplayName("existsByExternalUserId(): returns a true")
        fun `successful existsByExternalUserId true`() {
            val actual =
                assertDoesNotThrow(
                    ThrowingSupplier {
                        accessService.existsByExternalUserId(DEFAULT_EXTERNAL_USER_ID)
                    },
                )
            assertTrue(actual)
        }

        @Test
        @DisplayName("existsByExternalUserId(): returns a false")
        fun `successful existsByExternalUserId false`() {
            val actual =
                assertDoesNotThrow(
                    ThrowingSupplier {
                        accessService.existsByExternalUserId(NOT_EXISTED_USER_ID)
                    },
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
                    },
                )
            assertNotNull(actual.id)
            assertNotEquals(DEFAULT_EXTERNAL_USER_ID, actual.externalUserId)
        }

        @Test
        @DisplayName("save(): returns an existed record")
        fun `successful save update`() {
            val dto = DtoGenerator.generateNotificationDto(externalUserId = DEFAULT_EXTERNAL_USER_ID)

            val actual =
                assertDoesNotThrow(
                    ThrowingSupplier {
                        accessService.save(dto.telegramUser, dto.telegramChat, dto)
                    },
                )
            assertEquals(DEFAULT_ID, actual.id)
            assertEquals(DEFAULT_EXTERNAL_USER_ID, actual.externalUserId)
        }

        @Test
        @DisplayName("save(): reuses existing chat entity when externalChatId already exists")
        fun `successful save with existing chat`() {
            val dto =
                DtoGenerator.generateNotificationDto(
                    externalUserId = DEFAULT_EXTERNAL_USER_ID,
                    externalChatId = DEFAULT_EXTERNAL_USER_ID,
                )

            val actual =
                assertDoesNotThrow(
                    ThrowingSupplier {
                        accessService.save(dto.telegramUser, dto.telegramChat, dto)
                    },
                )
            assertEquals(DEFAULT_EXTERNAL_USER_ID, actual.externalUserId)
        }

        @Test
        @DisplayName("updateTimeOfLastNotification(): returns an updated record")
        fun `successful updateTimeOfLastNotification`() {
            val time = LocalDateTime.now()

            val actual =
                assertDoesNotThrow(
                    ThrowingSupplier {
                        accessService.updateTimeOfLastNotification(DEFAULT_EXTERNAL_USER_ID, time)
                    },
                )

            assertEquals(time, actual.timeOfLastNotification)
            assertEquals(WITHOUT_NOTIFICATION_ATTEMPTS, actual.notificationAttempts)
        }

        @Test
        @DisplayName("updateNotificationSettings(): returns an updated set of records")
        fun `successful updateNotificationSettings`() {
            val existed = accessService.findNotificationSettingByExternalUserId(DEFAULT_EXTERNAL_USER_ID)

            val actual =
                assertDoesNotThrow(
                    ThrowingSupplier {
                        accessService.updateNotificationSettings(setOf(existed))
                    },
                )

            assertFalse(actual.isEmpty())
            assertEquals(existed.id, actual.first().id)
        }

        @Test
        @DisplayName("updateNotificationsEnabled(): changed 'enabled' flag as true")
        fun `successful updateNotificationsEnabled`() {
            assertDoesNotThrow(
                ThrowingSupplier {
                    accessService.updateNotificationsEnabled(DISABLED_EXTERNAL_USER_ID)
                },
            )
            assertTrue(accessService.findIsEnabledNotificationsByExternalUserId(DISABLED_EXTERNAL_USER_ID))
        }

        @Test
        @DisplayName("updateNotificationsDisabled(): changed 'enabled' flag as false")
        fun `successful updateNotificationsDisabled`() {
            assertDoesNotThrow(
                ThrowingSupplier {
                    accessService.updateNotificationsDisabled(DEFAULT_EXTERNAL_USER_ID)
                },
            )
            assertFalse(accessService.findIsEnabledNotificationsByExternalUserId(DEFAULT_EXTERNAL_USER_ID))
        }

        @Test
        @DisplayName("updateNotificationSettings(): updates interval when it differs from current")
        fun `successful updateNotificationSettings changes interval`() {
            val newInterval = IntervalNotificationType.HALF_HOUR

            val actual =
                assertDoesNotThrow(
                    ThrowingSupplier {
                        accessService.updateNotificationSettings(DEFAULT_EXTERNAL_USER_ID, newInterval)
                    },
                )

            assertEquals(newInterval, actual.notificationInterval)
        }

        @Test
        @DisplayName("updateNotificationSettings(): does not update when interval is the same")
        fun `successful updateNotificationSettings same interval`() {
            val sameInterval = IntervalNotificationType.TWO_HOURS

            val actual =
                assertDoesNotThrow(
                    ThrowingSupplier {
                        accessService.updateNotificationSettings(DEFAULT_EXTERNAL_USER_ID, sameInterval)
                    },
                )

            assertEquals(sameInterval, actual.notificationInterval)
        }

        @Test
        @DisplayName("updateQuietMode(): persists quiet mode start and end times")
        fun `successful updateQuietMode`() {
            val expectedStart = LocalTime.of(22, 0)
            val expectedEnd = LocalTime.of(8, 0)

            assertDoesNotThrow(
                ThrowingSupplier {
                    accessService.updateQuietMode(DEFAULT_EXTERNAL_USER_ID, expectedStart, expectedEnd)
                },
            )

            val actual = accessService.findNotificationSettingByExternalUserId(DEFAULT_EXTERNAL_USER_ID)
            assertEquals(expectedStart, actual.quietModeStart)
            assertEquals(expectedEnd, actual.quietModeEnd)
        }

        @Test
        @DisplayName("updateQuietModeDisabled(): clears quiet mode start and end times")
        fun `successful updateQuietModeDisabled`() {
            accessService.updateQuietMode(DEFAULT_EXTERNAL_USER_ID, LocalTime.of(22, 0), LocalTime.of(8, 0))

            assertDoesNotThrow(
                ThrowingSupplier {
                    accessService.updateQuietModeDisabled(DEFAULT_EXTERNAL_USER_ID)
                },
            )

            val actual = accessService.findNotificationSettingByExternalUserId(DEFAULT_EXTERNAL_USER_ID)
            assertNull(actual.quietModeStart)
            assertNull(actual.quietModeEnd)
        }

        @Test
        @DisplayName("updateTimezone(): persists new timezone for existing user")
        fun `successful updateTimezone`() {
            val newTimezone = "America/New_York"

            assertDoesNotThrow(
                ThrowingSupplier {
                    accessService.updateTimezone(DEFAULT_EXTERNAL_USER_ID, newTimezone)
                },
            )

            val actual = accessService.findNotificationSettingByExternalUserId(DEFAULT_EXTERNAL_USER_ID)
            assertEquals(newTimezone, actual.telegramUser.userTimeZone)
        }

        @Test
        @DisplayName("updateTimezone(): throws IllegalArgumentException when user does not exist")
        fun `failure updateTimezone not found`() {
            assertThrows<IllegalArgumentException> {
                accessService.updateTimezone(NOT_EXISTED_USER_ID, "Europe/Berlin")
            }
        }

        @Test
        @DisplayName(
            "findNotificationSettingByExternalUserId(): throws NotificationSettingsNotFoundException when record not found by externalUserId",
        )
        fun `failure findNotificationSettingByExternalUserId not found`() {
            assertThrows<NotificationSettingsNotFoundException> {
                accessService.findNotificationSettingByExternalUserId(NOT_EXISTED_USER_ID)
            }
        }

        @Test
        @DisplayName(
            "updateTimeOfLastNotification(): throws NotificationSettingsNotFoundException when record not found by externalUserId",
        )
        fun `failure updateTimeOfLastNotification not found`() {
            val time = LocalDateTime.now()
            assertThrows<NotificationSettingsNotFoundException> {
                accessService.updateTimeOfLastNotification(NOT_EXISTED_USER_ID, time)
            }
        }

        @Test
        @DisplayName(
            "updateNotificationSettings(): resets timeOfLastNotification and notificationAttempts when interval changes",
        )
        fun `updateNotificationSettings resets timer and attempts on interval change`() {
            getMutableClock().setTime("2025-06-15T14:00:00Z")
            val expectedTime = LocalDateTime.now(clock)

            val actual =
                assertDoesNotThrow(
                    ThrowingSupplier {
                        accessService.updateNotificationSettings(
                            DEFAULT_EXTERNAL_USER_ID,
                            IntervalNotificationType.HALF_HOUR,
                        )
                    },
                )

            assertEquals(IntervalNotificationType.HALF_HOUR, actual.notificationInterval)
            assertEquals(expectedTime, actual.timeOfLastNotification)
            assertEquals(WITHOUT_NOTIFICATION_ATTEMPTS, actual.notificationAttempts)
        }

        @Test
        @DisplayName(
            "updateNotificationSettings(): does not reset timeOfLastNotification and notificationAttempts when interval is the same",
        )
        fun `updateNotificationSettings keeps timer and attempts on same interval`() {
            val before = accessService.findNotificationSettingByExternalUserId(DEFAULT_EXTERNAL_USER_ID)

            val actual =
                assertDoesNotThrow(
                    ThrowingSupplier {
                        accessService.updateNotificationSettings(
                            DEFAULT_EXTERNAL_USER_ID,
                            IntervalNotificationType.TWO_HOURS,
                        )
                    },
                )

            assertEquals(before.timeOfLastNotification, actual.timeOfLastNotification)
            assertEquals(before.notificationAttempts, actual.notificationAttempts)
        }

        @Test
        @DisplayName(
            "updateNotificationSettings(): throws NotificationSettingsNotFoundException when record not found by externalUserId",
        )
        fun `failure updateNotificationSettings not found`() {
            assertThrows<NotificationSettingsNotFoundException> {
                accessService.updateNotificationSettings(NOT_EXISTED_USER_ID, IntervalNotificationType.HOUR)
            }
        }

        @Test
        @DisplayName("updateNotificationSettings(): clears active pauseUntil when interval changes")
        fun `successful updateNotificationSettings clears pauseUntil on interval change`() {
            getMutableClock().setTime("2025-06-15T10:00:00Z")
            val pauseUntil = LocalDateTime.of(2025, 6, 15, 14, 0)
            accessService.updatePause(DEFAULT_EXTERNAL_USER_ID, pauseUntil)

            val actual =
                assertDoesNotThrow(
                    ThrowingSupplier {
                        accessService.updateNotificationSettings(
                            DEFAULT_EXTERNAL_USER_ID,
                            IntervalNotificationType.HALF_HOUR,
                        )
                    },
                )

            assertEquals(IntervalNotificationType.HALF_HOUR, actual.notificationInterval)
            assertNull(actual.pauseUntil)
            assertEquals(LocalDateTime.now(clock), actual.timeOfLastNotification)
        }

        @Test
        @DisplayName("updatePause(): sets pauseUntil and shifts timeOfLastNotification to pauseUntil minus interval")
        fun `successful updatePause sets pauseUntil and shifts timeOfLastNotification`() {
            val pauseUntil = LocalDateTime.of(2025, 6, 15, 14, 0)
            // SQL fixture sets DEFAULT_EXTERNAL_USER_ID with TWO_HOURS interval (120 minutes)
            val expectedTimeOfLastNotification = pauseUntil.minusMinutes(IntervalNotificationType.TWO_HOURS.minutes)

            val actual =
                assertDoesNotThrow(
                    ThrowingSupplier {
                        accessService.updatePause(DEFAULT_EXTERNAL_USER_ID, pauseUntil)
                    },
                )

            assertEquals(pauseUntil, actual.pauseUntil)
            assertEquals(expectedTimeOfLastNotification, actual.timeOfLastNotification)
        }

        @Test
        @DisplayName("updatePause(): does NOT reset notificationAttempts when pause is set")
        fun `successful updatePause keeps notificationAttempts`() {
            val pauseUntil = LocalDateTime.of(2025, 6, 15, 14, 0)
            // SQL fixture seeds notification_attempts = 1 for DEFAULT_EXTERNAL_USER_ID
            val before = accessService.findNotificationSettingByExternalUserId(DEFAULT_EXTERNAL_USER_ID)

            val actual =
                assertDoesNotThrow(
                    ThrowingSupplier {
                        accessService.updatePause(DEFAULT_EXTERNAL_USER_ID, pauseUntil)
                    },
                )

            assertEquals(before.notificationAttempts, actual.notificationAttempts)
        }

        @Test
        @DisplayName("updatePause(): with null pauseUntil resets pauseUntil to null and timeOfLastNotification to now")
        fun `successful updatePause cancel resets to now`() {
            getMutableClock().setTime("2025-06-15T14:00:00Z")
            val expectedTime = LocalDateTime.now(clock)
            // First put user into paused state
            accessService.updatePause(DEFAULT_EXTERNAL_USER_ID, LocalDateTime.of(2025, 6, 15, 18, 0))

            val actual =
                assertDoesNotThrow(
                    ThrowingSupplier {
                        accessService.updatePause(DEFAULT_EXTERNAL_USER_ID, null)
                    },
                )

            assertNull(actual.pauseUntil)
            assertEquals(expectedTime, actual.timeOfLastNotification)
        }

        @Test
        @DisplayName("updatePause(): cancel does NOT reset notificationAttempts")
        fun `successful updatePause cancel keeps notificationAttempts`() {
            val before = accessService.findNotificationSettingByExternalUserId(DEFAULT_EXTERNAL_USER_ID)

            val actual =
                assertDoesNotThrow(
                    ThrowingSupplier {
                        accessService.updatePause(DEFAULT_EXTERNAL_USER_ID, null)
                    },
                )

            assertEquals(before.notificationAttempts, actual.notificationAttempts)
        }

        @Test
        @DisplayName("updatePause(): re-pause overwrites previous pauseUntil and timeOfLastNotification")
        fun `successful updatePause re-pause overwrites`() {
            val firstPause = LocalDateTime.of(2025, 6, 15, 14, 0)
            val secondPause = LocalDateTime.of(2025, 6, 15, 18, 0)
            accessService.updatePause(DEFAULT_EXTERNAL_USER_ID, firstPause)

            val actual =
                assertDoesNotThrow(
                    ThrowingSupplier {
                        accessService.updatePause(DEFAULT_EXTERNAL_USER_ID, secondPause)
                    },
                )

            assertEquals(secondPause, actual.pauseUntil)
            assertEquals(
                secondPause.minusMinutes(IntervalNotificationType.TWO_HOURS.minutes),
                actual.timeOfLastNotification,
            )
        }

        @Test
        @DisplayName("updatePause(): persists pauseUntil so subsequent reads return it")
        fun `successful updatePause persists pauseUntil`() {
            val pauseUntil = LocalDateTime.of(2025, 6, 15, 14, 0)

            accessService.updatePause(DEFAULT_EXTERNAL_USER_ID, pauseUntil)

            val reloaded = accessService.findNotificationSettingByExternalUserId(DEFAULT_EXTERNAL_USER_ID)
            assertEquals(pauseUntil, reloaded.pauseUntil)
        }

        @Test
        @DisplayName(
            "updatePause(): throws NotificationSettingsNotFoundException when record not found by externalUserId",
        )
        fun `failure updatePause not found`() {
            assertThrows<NotificationSettingsNotFoundException> {
                accessService.updatePause(NOT_EXISTED_USER_ID, LocalDateTime.of(2025, 6, 15, 14, 0))
            }
        }

        @Test
        @DisplayName(
            "updatePause(): throws NotificationSettingsNotFoundException when record not found by externalUserId on cancel",
        )
        fun `failure updatePause cancel not found`() {
            assertThrows<NotificationSettingsNotFoundException> {
                accessService.updatePause(NOT_EXISTED_USER_ID, null)
            }
        }

        @Test
        @DisplayName(
            "updatePause(): throws NotificationSettingsNotFoundException for disabled user (filtered by @SQLRestriction)",
        )
        fun `failure updatePause disabled user`() {
            assertThrows<NotificationSettingsNotFoundException> {
                accessService.updatePause(DISABLED_EXTERNAL_USER_ID, LocalDateTime.of(2025, 6, 15, 14, 0))
            }
        }

        @Test
        @DisplayName("updateQuietMode(): clears active pauseUntil")
        fun `updateQuietMode clears active pauseUntil`() {
            val pauseUntil = LocalDateTime.of(2025, 6, 15, 14, 0)
            accessService.updatePause(DEFAULT_EXTERNAL_USER_ID, pauseUntil)

            assertDoesNotThrow(
                ThrowingSupplier {
                    accessService.updateQuietMode(DEFAULT_EXTERNAL_USER_ID, LocalTime.of(22, 0), LocalTime.of(8, 0))
                },
            )

            val actual = accessService.findNotificationSettingByExternalUserId(DEFAULT_EXTERNAL_USER_ID)
            assertNull(actual.pauseUntil)
            assertEquals(LocalTime.of(22, 0), actual.quietModeStart)
            assertEquals(LocalTime.of(8, 0), actual.quietModeEnd)
        }

        @Test
        @DisplayName("updateQuietModeDisabled(): clears active pauseUntil")
        fun `updateQuietModeDisabled clears active pauseUntil`() {
            val pauseUntil = LocalDateTime.of(2025, 6, 15, 14, 0)
            accessService.updateQuietMode(DEFAULT_EXTERNAL_USER_ID, LocalTime.of(22, 0), LocalTime.of(8, 0))
            accessService.updatePause(DEFAULT_EXTERNAL_USER_ID, pauseUntil)

            assertDoesNotThrow(
                ThrowingSupplier {
                    accessService.updateQuietModeDisabled(DEFAULT_EXTERNAL_USER_ID)
                },
            )

            val actual = accessService.findNotificationSettingByExternalUserId(DEFAULT_EXTERNAL_USER_ID)
            assertNull(actual.pauseUntil)
            assertNull(actual.quietModeStart)
            assertNull(actual.quietModeEnd)
        }

        @Test
        @DisplayName("updateNotificationsDisabled(): clears active pauseUntil before disabling")
        fun `updateNotificationsDisabled clears active pauseUntil`() {
            val pauseUntil = LocalDateTime.of(2025, 6, 15, 14, 0)
            accessService.updatePause(DEFAULT_EXTERNAL_USER_ID, pauseUntil)

            assertDoesNotThrow(
                ThrowingSupplier {
                    accessService.updateNotificationsDisabled(DEFAULT_EXTERNAL_USER_ID)
                },
            )
            // While disabled the entity is filtered out by @SQLRestriction, so re-enable to read it.
            accessService.updateNotificationsEnabled(DEFAULT_EXTERNAL_USER_ID)

            val actual = accessService.findNotificationSettingByExternalUserId(DEFAULT_EXTERNAL_USER_ID)
            assertNull(actual.pauseUntil)
        }

        @Test
        @DisplayName("updateDailyGoal(): persists new daily goal for existing user")
        fun `successful updateDailyGoal updates value`() {
            val newGoalMl = 3000

            assertDoesNotThrow(
                ThrowingSupplier {
                    accessService.updateDailyGoal(DEFAULT_EXTERNAL_USER_ID, newGoalMl)
                },
            )

            val actual = accessService.findNotificationSettingByExternalUserId(DEFAULT_EXTERNAL_USER_ID)
            assertEquals(newGoalMl, actual.dailyGoalMl)
        }

        @Test
        @DisplayName("updateDailyGoal(): does not throw when user does not exist (no-op update)")
        fun `successful updateDailyGoal noop for missing user`() {
            assertDoesNotThrow(
                ThrowingSupplier {
                    accessService.updateDailyGoal(NOT_EXISTED_USER_ID, 1500)
                },
            )
        }

        @Test
        @DisplayName("updateDailyGoal(): does not affect dailyGoalMl of other users")
        fun `successful updateDailyGoal does not affect other users`() {
            val newGoalForFirst = 2500
            val before = accessService.findNotificationSettingByExternalUserId(DEFAULT_EXTERNAL_USER_ID)
            // DISABLED_EXTERNAL_USER_ID is filtered by @SQLRestriction, so re-enable to read it back.
            accessService.updateNotificationsEnabled(DISABLED_EXTERNAL_USER_ID)
            val secondBefore = accessService.findNotificationSettingByExternalUserId(DISABLED_EXTERNAL_USER_ID)

            accessService.updateDailyGoal(DEFAULT_EXTERNAL_USER_ID, newGoalForFirst)

            val firstAfter = accessService.findNotificationSettingByExternalUserId(DEFAULT_EXTERNAL_USER_ID)
            val secondAfter = accessService.findNotificationSettingByExternalUserId(DISABLED_EXTERNAL_USER_ID)
            assertNotEquals(before.dailyGoalMl, firstAfter.dailyGoalMl)
            assertEquals(newGoalForFirst, firstAfter.dailyGoalMl)
            assertEquals(secondBefore.dailyGoalMl, secondAfter.dailyGoalMl)
        }

        @Test
        @DisplayName(
            "updatePause(null): on already-expired pause, clears pauseUntil but does NOT reset timeOfLastNotification",
        )
        fun `updatePause cancel after pause expired keeps timeOfLastNotification`() {
            getMutableClock().setTime("2025-06-15T10:00:00Z")
            val pauseUntil = LocalDateTime.of(2025, 6, 15, 11, 0)
            accessService.updatePause(DEFAULT_EXTERNAL_USER_ID, pauseUntil)
            val timerWhilePaused = pauseUntil.minusMinutes(IntervalNotificationType.TWO_HOURS.minutes)

            // Advance clock past pauseUntil so the pause is expired.
            getMutableClock().setTime("2025-06-15T15:00:00Z")

            val actual =
                assertDoesNotThrow(
                    ThrowingSupplier {
                        accessService.updatePause(DEFAULT_EXTERNAL_USER_ID, null)
                    },
                )

            assertNull(actual.pauseUntil)
            // Cancel is idempotent for already-expired pause: timer is not bumped.
            assertEquals(timerWhilePaused, actual.timeOfLastNotification)
        }

        companion object {
            private const val DEFAULT_ID = 1L
            private const val NOT_EXISTED_USER_ID = 0L
            private const val DEFAULT_EXTERNAL_USER_ID = 1L
            private const val DISABLED_EXTERNAL_USER_ID = 2L
            private const val WITHOUT_NOTIFICATION_ATTEMPTS = 0
        }
    }

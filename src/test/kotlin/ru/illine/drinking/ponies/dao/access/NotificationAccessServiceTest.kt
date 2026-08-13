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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlConfig
import ru.illine.drinking.ponies.exception.NotificationSettingsNotFoundException
import ru.illine.drinking.ponies.model.base.IntervalNotificationType
import ru.illine.drinking.ponies.model.dto.internal.UserStateChangeDto
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest
import ru.illine.drinking.ponies.test.util.TestClockHelper
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
        private val telegramUserAccessService: TelegramUserAccessService,
        private val clock: Clock,
    ) {
        private fun getMutableClock() = clock as TestClockHelper.MutableClock

        @BeforeEach
        fun resetClock() {
            getMutableClock().setTime(TestClockHelper.DEFAULT_TIME)
        }

        @Test
        @DisplayName("findAllNotificationSettings(): returns a not empty set")
        fun `successful findAllNotificationSettings`() {
            assertFalse(accessService.findAllNotificationSettings().isEmpty())
        }

        @Test
        @DisplayName("findAllNotificationSettings(): survives a soft-deleted user instead of failing the whole batch")
        fun `findAllNotificationSettings survives a soft deleted user`() {
            val settings = assertDoesNotThrow(ThrowingSupplier { accessService.findAllNotificationSettings() })

            assertFalse(settings.isEmpty(), "The users who are still around have to survive the deleted one")
        }

        @Test
        @DisplayName("findAllNotificationSettings(): leaves the soft-deleted user out and keeps the live ones")
        fun `findAllNotificationSettings returns the live users only`() {
            val externalUserIds = accessService.findAllNotificationSettings().map { it.telegramUser.externalUserId }

            assertEquals(listOf(DEFAULT_EXTERNAL_USER_ID), externalUserIds)
            assertFalse(
                externalUserIds.contains(DELETED_EXTERNAL_USER_ID),
                "A deleted user must not be reminded to drink, even with notifications enabled",
            )
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
        @DisplayName("recordMailingResults(): an untouched snapshot goes back in without complaint")
        fun `successful recordMailingResults`() {
            val existed = accessService.findNotificationSettingByExternalUserId(DEFAULT_EXTERNAL_USER_ID)

            assertDoesNotThrow {
                accessService.recordMailingResults(setOf(existed))
            }
        }

        @Test
        @DisplayName("recordMailingResults(): writes back what a mailing round produces")
        fun `recordMailingResults writes the mailing result`() {
            val snapshot =
                accessService.findNotificationSettingByExternalUserId(DEFAULT_EXTERNAL_USER_ID).apply {
                    notificationAttempts = 3
                    timeOfLastNotification = LocalDateTime.of(2025, 1, 1, 9, 0)
                    telegramChat.previousNotificationMessageId = 4242
                }

            accessService.recordMailingResults(setOf(snapshot))

            val stored = accessService.findNotificationSettingByExternalUserId(DEFAULT_EXTERNAL_USER_ID)
            assertEquals(3, stored.notificationAttempts)
            assertEquals(LocalDateTime.of(2025, 1, 1, 9, 0), stored.timeOfLastNotification)
            assertEquals(4242, stored.telegramChat.previousNotificationMessageId)
        }

        @Test
        @DisplayName("recordMailingResults(): a ban applied mid-round is not reverted by the stale snapshot")
        fun `recordMailingResults does not revert a ban`() {
            val snapshot = accessService.findNotificationSettingByExternalUserId(DEFAULT_EXTERNAL_USER_ID)
            telegramUserAccessService.updateState(
                id = DEFAULT_USER_ID,
                actorId = DISABLED_USER_ID,
                change = UserStateChangeDto(banned = true),
            )

            accessService.recordMailingResults(setOf(snapshot))

            assertTrue(telegramUserAccessService.resolveAccessFlags(DEFAULT_EXTERNAL_USER_ID).isBanned)
        }

        @ParameterizedTest(name = "[{index}] externalUserId={0}")
        @ValueSource(longs = [NOT_EXISTED_USER_ID, DELETED_EXTERNAL_USER_ID])
        @DisplayName("recordMailingResults(): a user whose settings are gone is skipped, the rest of the round lands")
        fun `recordMailingResults survives a vanished user`(vanished: Long) {
            val alive =
                accessService.findNotificationSettingByExternalUserId(DEFAULT_EXTERNAL_USER_ID).apply {
                    notificationAttempts = 7
                }
            val gone = DtoGenerator.generateNotificationDto(externalUserId = vanished)

            assertDoesNotThrow { accessService.recordMailingResults(listOf(gone, alive)) }

            assertEquals(
                7,
                accessService.findNotificationSettingByExternalUserId(DEFAULT_EXTERNAL_USER_ID).notificationAttempts,
                "One missing row must not cost the whole round",
            )
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
            assertFalse(actual.timeOfLastNotification.isAfter(LocalDateTime.now(clock)))
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
            assertFalse(actual.timeOfLastNotification.isAfter(LocalDateTime.now(clock)))
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
            accessService.updateNotificationsEnabled(DEFAULT_EXTERNAL_USER_ID)

            val actual = accessService.findNotificationSettingByExternalUserId(DEFAULT_EXTERNAL_USER_ID)
            assertNull(actual.pauseUntil)
        }

        @Test
        @DisplayName("updateNotificationsEnabled(): clears an active pause so the countdown starts over")
        fun `updateNotificationsEnabled clears active pause`() {
            val now = LocalDateTime.now(clock)
            accessService.updatePause(DEFAULT_EXTERNAL_USER_ID, now.plusHours(8))

            accessService.updateNotificationsEnabled(DEFAULT_EXTERNAL_USER_ID)

            val actual = accessService.findNotificationSettingByExternalUserId(DEFAULT_EXTERNAL_USER_ID)
            assertNull(actual.pauseUntil)
            assertFalse(
                actual.timeOfLastNotification.isAfter(now),
                "a pause shifts timeOfLastNotification forward, and the shift outlives the pause mark",
            )
        }

        @Test
        @DisplayName("updateNotificationsEnabled(): a countdown that is not shifted stays where it was")
        fun `updateNotificationsEnabled keeps an unshifted countdown`() {
            val lastNotification = LocalDateTime.now(clock).minusMinutes(30)
            accessService.updateTimeOfLastNotification(DEFAULT_EXTERNAL_USER_ID, lastNotification)

            accessService.updateNotificationsEnabled(DEFAULT_EXTERNAL_USER_ID)

            val actual = accessService.findNotificationSettingByExternalUserId(DEFAULT_EXTERNAL_USER_ID)
            assertEquals(
                lastNotification,
                actual.timeOfLastNotification,
                "Without a pause to undo there is nothing to move, and moving it would delay the reminder",
            )
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

            getMutableClock().setTime("2025-06-15T15:00:00Z")

            val actual =
                assertDoesNotThrow(
                    ThrowingSupplier {
                        accessService.updatePause(DEFAULT_EXTERNAL_USER_ID, null)
                    },
                )

            assertNull(actual.pauseUntil)
            assertEquals(timerWhilePaused, actual.timeOfLastNotification)
        }

        companion object {
            private const val DEFAULT_ID = 1L
            private const val NOT_EXISTED_USER_ID = 0L
            private const val DEFAULT_USER_ID = 1L
            private const val DISABLED_USER_ID = 2L
            private const val DEFAULT_EXTERNAL_USER_ID = 1L
            private const val DISABLED_EXTERNAL_USER_ID = 2L
            private const val DELETED_EXTERNAL_USER_ID = 3L
            private const val WITHOUT_NOTIFICATION_ATTEMPTS = 0
        }
    }

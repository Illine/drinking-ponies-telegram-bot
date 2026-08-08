package ru.illine.drinking.ponies.scheduler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlConfig
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.service.notification.NotificationSenderService
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest
import ru.illine.drinking.ponies.test.util.ClockHelperTest
import java.time.Clock

@SpringIntegrationTest
@DisplayName("NotificationScheduler Spring Integration Test")
@Sql(
    scripts = ["classpath:sql/scheduler/NotificationScheduler.sql"],
    config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
)
@Sql(
    scripts = ["classpath:sql/clear.sql"],
    config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
)
class NotificationSchedulerIntegrationTest
    @Autowired
    constructor(
        private val scheduler: NotificationScheduler,
        private val clock: Clock,
    ) {
        @MockitoBean
        private lateinit var notificationSenderService: NotificationSenderService

        @BeforeEach
        fun resetClock() {
            (clock as ClockHelperTest.MutableClock).setTime(ClockHelperTest.DEFAULT_TIME)
        }

        @Test
        @DisplayName("sendDrinkingReminders(): a soft-deleted account no longer silences the reminders of everyone")
        fun `a deleted user does not stop the mailing`() {
            scheduler.sendDrinkingReminders()

            val captor = argumentCaptor<List<NotificationSettingDto>>()
            verify(notificationSenderService).sendNotifications(captor.capture())
            assertEquals(
                listOf(LIVE_EXTERNAL_USER_ID),
                captor.firstValue.map { it.telegramUser.externalUserId },
                "The live user has to be reminded, and the deleted one left out",
            )
        }

        @Test
        @DisplayName("sendDrinkingReminders(): nothing is suspended when no one ran out of attempts")
        fun `nothing is suspended`() {
            scheduler.sendDrinkingReminders()

            verify(notificationSenderService, never()).suspendNotifications(any())
        }

        companion object {
            private const val LIVE_EXTERNAL_USER_ID = 2L
        }
    }

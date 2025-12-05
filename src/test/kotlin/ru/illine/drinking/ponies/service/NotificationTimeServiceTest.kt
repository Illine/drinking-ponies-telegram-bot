package ru.illine.drinking.ponies.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import ru.illine.drinking.ponies.model.base.DelayNotificationType
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest
import ru.illine.drinking.ponies.test.util.ClockHelperTest
import java.time.Clock
import java.time.LocalTime
import java.util.stream.Stream

@SpringIntegrationTest
@DisplayName("NotificationTimeService Spring Integration Test")
class NotificationTimeServiceTest @Autowired constructor(
    private val notificationTimeService: NotificationTimeService,
    private val clock: Clock
) {

    private fun getMutableClock() = clock as ClockHelperTest.MutableClock

    @BeforeEach
    fun resetClock() {
        getMutableClock().setTime(ClockHelperTest.DEFAULT_TIME)
    }

    @ParameterizedTest(name = "{index}: {5}")
    @MethodSource("quietTimeScenarios")
    fun `isOutsideQuietTime table test`(
        currentTimeIso: String,
        userZone: String,
        quietStartIso: String?,
        quietEndIso: String?,
        expected: Boolean,
        description: String
    ) {
        getMutableClock().setTime(currentTimeIso)

        val start = quietStartIso?.let { LocalTime.parse(it) }
        val end = quietEndIso?.let { LocalTime.parse(it) }

        val dto = DtoGenerator.generateNotificationDto(
            userTimeZone = userZone,
            quietModeStart = start,
            quietModeEnd = end
        )

        val actual = notificationTimeService.isOutsideQuietTime(dto)
        assertEquals(expected, actual, description)
    }

    @ParameterizedTest(name = "{index}: {4}")
    @MethodSource("notificationDueScenarios")
    fun `isNotificationDue table test`(
        currentTimeIso: String,
        lastNotificationTimeIso: String,
        delay: DelayNotificationType,
        expected: Boolean,
        description: String
    ) {
        getMutableClock().setTime(currentTimeIso)

        val lastNotification = java.time.ZonedDateTime.parse(lastNotificationTimeIso).toLocalDateTime()

        val dto = DtoGenerator.generateNotificationDto(
            timeOfLastNotification = lastNotification,
            delayNotification = delay
        )

        val actual = notificationTimeService.isNotificationDue(dto)
        assertEquals(expected, actual, description)
    }

    companion object {

        @JvmStatic
        fun quietTimeScenarios(): Stream<Arguments> {
            return Stream.of(
                // FORMAT: CurrentTime | Zone | Start | End | Expected | Description

                Arguments.of(
                    "2023-01-01T12:00:00Z", "UTC", null, null, true,
                    "No quiet mode set -> Always allow"
                ),

                Arguments.of(
                    "2023-01-01T15:00:00Z", "UTC", "14:00", "16:00", false,
                    "Day interval: 15:00 is inside 14-16 -> Block"
                ),

                Arguments.of(
                    "2023-01-01T13:00:00Z", "UTC", "14:00", "16:00", true,
                    "Day interval: 13:00 is before 14-16 -> Allow"
                ),

                Arguments.of(
                    "2023-01-01T17:00:00Z", "UTC", "14:00", "16:00", true,
                    "Day interval: 17:00 is after 14-16 -> Allow"
                ),

                Arguments.of(
                    "2023-01-01T15:00:00Z", "UTC", "22:00", "08:00", true,
                    "Night interval: 15:00 is outside 22-08 -> Allow"
                ),

                Arguments.of(
                    "2023-01-01T12:00:00Z", "Asia/Tokyo", "20:00", "08:00", false,
                    "Tokyo (UTC+9): 12:00Z is 21:00 local. Inside 20-08 -> Block"
                ),

                Arguments.of(
                    "2023-01-01T14:00:00Z", "UTC", "14:00", "16:00", false,
                    "Exact start time -> Block (Inclusive)"
                ),

                Arguments.of(
                    "2023-01-01T16:00:00Z", "UTC", "14:00", "16:00", false,
                    "Exact end time -> Block (Inclusive)"
                ),

                Arguments.of(
                    "2023-01-01T23:00:00Z", "UTC", "22:00", "08:00", false,
                    "Night interval: 23:00 (Pre-midnight) inside 22-08 -> Block"
                ),

                Arguments.of(
                    "2023-01-01T02:00:00Z", "UTC", "22:00", "08:00", false,
                    "Night interval: 02:00 (Post-midnight) inside 22-08 -> Block"
                ),

                Arguments.of(
                    "2023-01-01T16:00:01Z", "UTC", "14:00", "16:00", true,
                    "1 second after quiet time ends -> Allow"
                ),

                Arguments.of(
                    "2023-01-01T12:00:00Z", "Invalid/Zone", "14:00", "16:00", true,
                    "Invalid timezone -> Fallback to UTC -> Allow"
                ),

                Arguments.of(
                    "2023-01-01T15:00:00Z", "UTC", "14:00", "14:00", true,
                    "Start == End -> Incorrect start/end  ->  Allow"
                ),

                Arguments.of(
                    "2023-01-02T00:00:00Z", "UTC", "22:00", "08:00", false,
                    "Midnight exactly (00:00) inside night interval (22-08) -> Block"
                ),

                Arguments.of(
                    "2023-01-01T21:00:00Z", "Asia/Dubai", "23:00", "07:00", false,
                    "Day change: UTC 21:00 is Local 01:00 (next day). Inside 23-07 -> Block"
                ),

                Arguments.of(
                    "2023-01-01T12:00:00Z", "UTC", "14:00", null, true,
                    "Partial null: Start set, End null -> Allow"
                ),
                Arguments.of(
                    "2023-01-01T12:00:00Z", "UTC", null, "16:00", true,
                    "Partial null: Start null, End set -> Allow"
                ),

                Arguments.of(
                    "2023-01-01T22:00:00Z", "UTC", "22:00", "08:00", false,
                    "Night interval start boundary: 22:00 exactly -> Block"
                ),

                Arguments.of(
                    "2023-01-02T08:00:00Z", "UTC", "22:00", "08:00", false,
                    "Night interval end boundary: 08:00 exactly -> Block"
                ),

                Arguments.of(
                    "2023-01-02T08:00:00.000000001Z", "UTC", "22:00", "08:00", true,
                    "1 nanosecond after night interval ends -> Allow"
                )
            )
        }

        @JvmStatic
        fun notificationDueScenarios(): Stream<Arguments> {
            return Stream.of(
                // FORMAT: CurrentTime | LastNotificationTime | Delay | Expected | Description

                Arguments.of(
                    "2023-01-01T12:00:00Z", "2023-01-01T10:00:00Z", DelayNotificationType.TWO_HOURS, false,
                    "Exact boundary: 10:00 + 120m == 12:00. isBefore is strict -> Not due yet"
                ),

                Arguments.of(
                    "2023-01-01T12:00:01Z", "2023-01-01T10:00:00Z", DelayNotificationType.TWO_HOURS, true,
                    "1 second after due time -> Due"
                ),

                Arguments.of(
                    "2023-01-01T11:59:59Z", "2023-01-01T10:00:00Z", DelayNotificationType.TWO_HOURS, false,
                    "1 second before due time -> Not Due"
                ),

                Arguments.of(
                    "2023-01-01T15:00:00Z", "2023-01-01T10:00:00Z", DelayNotificationType.HOUR, true,
                    "Long overdue: 10:00 + 60m < 15:00 -> Due"
                ),

                Arguments.of(
                    "2023-01-02T01:00:00Z", "2023-01-01T23:00:00Z", DelayNotificationType.HOUR, true,
                    "Day rollover: 23:00 + 60m = 00:00 (next day) < 01:00 -> Due"
                ),

                Arguments.of(
                    "2023-01-01T12:00:00Z", "2023-01-01T11:50:00Z", DelayNotificationType.HALF_HOUR, false,
                    "Short delay not reached: 11:50 + 30m = 12:20 > 12:00 -> Not Due"
                ),

                Arguments.of(
                    "2023-01-01T10:00:00Z", "2023-01-01T12:00:00Z", DelayNotificationType.HOUR, false,
                    "Last notification is in future -> Not Due"
                )
            )
        }
    }
}

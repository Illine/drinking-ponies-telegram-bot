package ru.illine.drinking.ponies.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.TimeHelper.DEFAULT_TIME_FORMATTER
import ru.illine.drinking.ponies.util.TimeHelper.nextNotificationTimeByNow
import ru.illine.drinking.ponies.util.TimeHelper.timeToString
import java.time.*

@UnitTest
@DisplayName("TimeHelper Unit Test")
class TimeHelperTest {

    private val clock = Clock.fixed(Instant.now(), ZoneOffset.UTC)
    private val now = LocalDateTime.now(clock)

    @Test
    @DisplayName("timeToString(LocalDateTime): returns time formatted as HH:mm")
    fun `timeToString from LocalDateTime`() {
        val expected = now.format(DEFAULT_TIME_FORMATTER)

        assertEquals(expected, timeToString(now))
    }

    @Test
    @DisplayName("timeToString(LocalTime): returns time formatted as HH:mm")
    fun `timeToString from LocalTime`() {
        val time = LocalTime.of(14, 5)

        assertEquals("14:05", timeToString(time))
    }

    @Test
    @DisplayName("nextNotificationTimeByNow(): returns now - interval + offset")
    fun `nextNotificationTimeByNow returns correct time`() {
        val intervalMinutes = 60L
        val offsetMinutes = 1L
        val expected = now.minusMinutes(intervalMinutes).plusMinutes(offsetMinutes)

        assertEquals(expected, nextNotificationTimeByNow(clock, intervalMinutes, offsetMinutes))
    }

    @Test
    @DisplayName("nextNotificationTimeByNow(): offset equals interval - returns now")
    fun `nextNotificationTimeByNow with equal interval and offset returns now`() {
        val minutes = 30L

        assertEquals(now, nextNotificationTimeByNow(clock, minutes, minutes))
    }

    @Test
    @DisplayName("nextNotificationTimeByNow(): offset greater than interval - returns future time")
    fun `nextNotificationTimeByNow with offset greater than interval returns future time`() {
        val intervalMinutes = 10L
        val offsetMinutes = 30L
        val expected = now.plusMinutes(offsetMinutes - intervalMinutes)

        assertEquals(expected, nextNotificationTimeByNow(clock, intervalMinutes, offsetMinutes))
    }
}
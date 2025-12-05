package ru.illine.drinking.ponies.test.util

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

object ClockHelperTest {

    val DEFAULT_TIME = "2025-01-01T12:00:00Z"
    val DEFAULT_ZONE = ZoneOffset.UTC

    class MutableClock(
        private var instant: Instant,
        private val zone: ZoneId
    ) : Clock() {
        fun setTime(newInstant: Instant) {
            this.instant = newInstant
        }

        fun setTime(dateTime: String) {
            this.instant = Instant.parse(dateTime)
        }

        override fun getZone(): ZoneId = zone

        override fun withZone(zone: ZoneId): Clock = MutableClock(instant, zone)

        override fun instant(): Instant = instant
    }
}
package ru.illine.drinking.ponies.util.statistics

import java.time.*

fun LocalDateTime.toUtcInstant(): Instant = this.toInstant(ZoneOffset.UTC)

object StatisticsPeriodHelper {

    fun toLocal(eventTimeUtc: LocalDateTime, zone: ZoneId): LocalDateTime =
        eventTimeUtc.atZone(ZoneOffset.UTC).withZoneSameInstant(zone).toLocalDateTime()

    fun localDayBoundsToUtc(
        startLocal: LocalDate,
        endLocalExclusive: LocalDate,
        zone: ZoneId
    ): Pair<LocalDateTime, LocalDateTime> {
        val startUtc = startLocal.atStartOfDay(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
        val endUtc = endLocalExclusive.atStartOfDay(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
        return startUtc to endUtc
    }

}

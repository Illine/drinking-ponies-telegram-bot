package ru.illine.drinking.ponies.model.base

import java.util.*

@Suppress("unused")
enum class DelayNotificationType(
    val displayName: String,
    val minutes: Long,
    val queryData: UUID
) {
    HALF_HOUR("30 минут", 30, UUID.randomUUID()),
    HOUR("1 час", 60, UUID.randomUUID()),
    HOUR_AND_HALF("1 час 30 минут", 90, UUID.randomUUID()),
    TWO_HOURS("2 часа", 120, UUID.randomUUID());

    companion object {

        fun typeOf(queryData: String): DelayNotificationType? {
            return EnumSet.allOf(DelayNotificationType::class.java)
                .find { Objects.equals(queryData, it.queryData.toString()) }
        }

    }

}
package ru.illine.drinking.ponies.model.base

import java.util.*

enum class DelayNotificationType(
    val displayName: String,
    val minutes: Long
) {
    HALF_HOUR("30 минут", 30),
    HOUR("1 час", 60),
    HOUR_AND_HALF("1 час 30 минут", 90),
    TWO_HOURS("2 часа", 120);

    companion object {

        val BUTTON_IDS =
            EnumSet.allOf(DelayNotificationType::class.java)
                .stream()
                .map { it.name }
                .toList()

    }

}
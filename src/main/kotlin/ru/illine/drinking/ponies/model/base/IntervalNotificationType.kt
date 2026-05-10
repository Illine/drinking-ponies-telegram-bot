package ru.illine.drinking.ponies.model.base

import ru.illine.drinking.ponies.util.telegram.TelegramTimeConstants

@Suppress("unused")
enum class IntervalNotificationType(
    val displayName: String,
    val minutes: Long,
) {
    HALF_HOUR(TelegramTimeConstants.HALF_HOUR, 30),
    HOUR(TelegramTimeConstants.HOUR, 60),
    HOUR_AND_HALF(TelegramTimeConstants.HOUR_AND_HALF, 90),
    TWO_HOURS(TelegramTimeConstants.TWO_HOURS, 120),
    THREE_HOURS(TelegramTimeConstants.THREE_HOURS, 180);
}

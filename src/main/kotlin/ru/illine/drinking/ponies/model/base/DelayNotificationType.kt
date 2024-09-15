package ru.illine.drinking.ponies.model.base

import ru.illine.drinking.ponies.util.TelegramConstants
import java.util.*

@Suppress("unused")
enum class DelayNotificationType(
    val displayName: String,
    val minutes: Long,
    val queryData: UUID
) {
    HALF_HOUR(TelegramConstants.HALF_HOUR, 30, UUID.fromString("e86794e6-be89-4423-b2dc-804118565699")),
    HOUR(TelegramConstants.HOUR, 60, UUID.fromString("d7d1a7c5-1b78-4a70-815d-a99d21f6a322")),
    HOUR_AND_HALF(TelegramConstants.HOUR_AND_HALF, 90, UUID.fromString("2052df88-cd05-4816-9ef6-9c68d0c05450")),
    TWO_HOURS(TelegramConstants.TWO_HOURS, 120, UUID.fromString("6c7b8c46-e1e2-4711-be37-d79dad5ad94f")),
    THREE_HOURS(TelegramConstants.THREE_HOURS, 180, UUID.fromString("f22f3cd8-480a-4961-8608-15222d2d3e15"));

    companion object {

        fun typeOf(queryData: String): DelayNotificationType? {
            return DelayNotificationType.values()
                .find { Objects.equals(queryData, it.queryData.toString()) }
        }

    }

}
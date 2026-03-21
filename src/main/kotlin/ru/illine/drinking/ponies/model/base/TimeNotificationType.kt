package ru.illine.drinking.ponies.model.base

import ru.illine.drinking.ponies.util.TelegramTimeConstants
import java.util.*

@Suppress("unused")
enum class TimeNotificationType(
    val displayName: String,
    val minutes: Long,
    val queryData: UUID
) {
    FIVE_MINUTES(TelegramTimeConstants.FIVE_MINUTES, 5, UUID.fromString("a1c2d3e4-5f67-4890-abcd-ef1234567801")),
    TEN_MINUTES(TelegramTimeConstants.TEN_MINUTES, 10, UUID.fromString("b2d3e4f5-6a78-4901-bcde-f12345678902")),
    TWENTY_MINUTES(TelegramTimeConstants.TWENTY_MINUTES, 20, UUID.fromString("c3e4f5a6-7b89-4012-cdef-123456789003")),
    HALF_HOUR(TelegramTimeConstants.HALF_HOUR, 30, UUID.fromString("e86794e6-be89-4423-b2dc-804118565699")),
    HOUR(TelegramTimeConstants.HOUR, 60, UUID.fromString("d7d1a7c5-1b78-4a70-815d-a99d21f6a322")),
    HOUR_AND_HALF(TelegramTimeConstants.HOUR_AND_HALF, 90, UUID.fromString("2052df88-cd05-4816-9ef6-9c68d0c05450")),
    TWO_HOURS(TelegramTimeConstants.TWO_HOURS, 120, UUID.fromString("6c7b8c46-e1e2-4711-be37-d79dad5ad94f")),
    THREE_HOURS(TelegramTimeConstants.THREE_HOURS, 180, UUID.fromString("f22f3cd8-480a-4961-8608-15222d2d3e15"));

    companion object {

        fun typeOf(queryData: String): TimeNotificationType? {
            return entries
                .find { Objects.equals(queryData, it.queryData.toString()) }
        }

    }

}
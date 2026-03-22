package ru.illine.drinking.ponies.model.base

import ru.illine.drinking.ponies.util.telegram.TelegramTimeConstants
import java.util.*

@Suppress("unused")
enum class SnoozeNotificationType(
    val displayName: String,
    val minutes: Long,
    val queryData: UUID
) {
    FIVE_MINS(TelegramTimeConstants.FIVE_MINS, 5, UUID.fromString("c4a7e3f1-2b8d-4a5e-9c6f-1d3e7b8a2c4d")),
    TEN_MINS(TelegramTimeConstants.TEN_MINS, 10, UUID.fromString("f8b2d6e4-3a7c-4f1e-8b5d-2c9f6a3e7d1b")),
    FIFTEEN_MINS(TelegramTimeConstants.FIFTEEN_MINS, 15, UUID.fromString("e3f7a2b8-5c1d-4e9f-b6a3-8d2c7e4f1a5b")),
    TWENTY_MINS(TelegramTimeConstants.TWENTY_MINS, 20, UUID.fromString("b5d1f9c3-7e4a-4b2f-a8e5-3c6f9d2b7e1a"));

    companion object {

        fun typeOf(queryData: String): SnoozeNotificationType? {
            return entries.find { Objects.equals(queryData, it.queryData.toString()) }
        }

    }

}
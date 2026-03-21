package ru.illine.drinking.ponies.model.base

import ru.illine.drinking.ponies.util.TelegramTimeConstants
import java.util.*

@Suppress("unused")
enum class SnoozeTimeNotificationType(
    override val displayName: String,
    override val minutes: Long,
    override val queryData: UUID
) : TimeBasedOption {
    FIVE_MINUTES(TelegramTimeConstants.FIVE_MINUTES, 5, UUID.fromString("a1c2d3e4-5f67-4890-abcd-ef1234567801")),
    TEN_MINUTES(TelegramTimeConstants.TEN_MINUTES, 10, UUID.fromString("b2d3e4f5-6a78-4901-bcde-f12345678902")),
    TWENTY_MINUTES(TelegramTimeConstants.TWENTY_MINUTES, 20, UUID.fromString("c3e4f5a6-7b89-4012-cdef-123456789003")),
    HALF_HOUR(TelegramTimeConstants.HALF_HOUR, 30, UUID.fromString("e86794e6-be89-4423-b2dc-804118565699"));

    companion object {

        fun typeOf(queryData: String): SnoozeTimeNotificationType? {
            return entries
                .find { Objects.equals(queryData, it.queryData.toString()) }
        }

    }

}
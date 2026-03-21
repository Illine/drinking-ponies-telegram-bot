package ru.illine.drinking.ponies.model.base

import ru.illine.drinking.ponies.util.TelegramMessageConstants
import ru.illine.drinking.ponies.util.TelegramTimeConstants
import java.util.*

@Suppress("unused")
enum class TimeNotificationType(
    val displayName: String,
    val minutes: Long,
    val queryData: UUID
) {
    RESET(TelegramMessageConstants.PAUSE_RESET_TO_DEFAULT_MESSAGE, 0, UUID.fromString("806c7780-3c82-445f-835d-b65dd3232688")),

    FIVE_MINUTES(TelegramTimeConstants.FIVE_MINUTES, 5, UUID.fromString("a1c2d3e4-5f67-4890-abcd-ef1234567801")),
    TEN_MINUTES(TelegramTimeConstants.TEN_MINUTES, 10, UUID.fromString("b2d3e4f5-6a78-4901-bcde-f12345678902")),
    TWENTY_MINUTES(TelegramTimeConstants.TWENTY_MINUTES, 20, UUID.fromString("c3e4f5a6-7b89-4012-cdef-123456789003")),
    HALF_HOUR(TelegramTimeConstants.HALF_HOUR, 30, UUID.fromString("e86794e6-be89-4423-b2dc-804118565699")),

    HOUR(TelegramTimeConstants.HOUR, 60, UUID.fromString("568df560-f242-4dd0-bb44-a0773b27d75d")),
    HOUR_AND_HALF(TelegramTimeConstants.HOUR_AND_HALF, 90, UUID.fromString("757f013a-d9fd-463c-974d-27e8f52f8d36")),
    TWO_HOURS(TelegramTimeConstants.TWO_HOURS, 120, UUID.fromString("3bcf42ec-e44c-4f29-b410-e65d5293c786")),
    THREE_HOURS(TelegramTimeConstants.THREE_HOURS, 180, UUID.fromString("a32279e9-c489-4a74-bd2f-bce9a2ca81fa")),
    FOUR_HOURS(TelegramTimeConstants.FOUR_HOURS, 240, UUID.fromString("cb9f45b0-0016-45d1-a436-6c109c548423")),
    FIVE_HOURS(TelegramTimeConstants.FIVE_HOURS, 300, UUID.fromString("508c756f-7987-4b51-bd94-bad6f1a671a3"));

    companion object {

        fun typeOf(queryData: String): TimeNotificationType? {
            return entries
                .find { Objects.equals(queryData, it.queryData.toString()) }
        }

        fun delayTimes(): List<TimeNotificationType> =
            listOf(FIVE_MINUTES, TEN_MINUTES, TWENTY_MINUTES, HALF_HOUR)

        fun settingsTimes(): List<TimeNotificationType> =
            listOf(HOUR, HOUR_AND_HALF, TWO_HOURS, THREE_HOURS)

        fun pauseTimes(): List<TimeNotificationType> =
            listOf(RESET, HOUR, TWO_HOURS, THREE_HOURS, FOUR_HOURS, FIVE_HOURS)

    }

}
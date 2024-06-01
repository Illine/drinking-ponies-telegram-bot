package ru.illine.drinking.ponies.model.base

import ru.illine.drinking.ponies.util.MessageHelper
import java.util.*

@Suppress("unused")
enum class SnoozeNotificationType(
    val displayName: String,
    val minutes: Long,
    val queryData: UUID
) {
    HOUR(MessageHelper.HOUR, 60, UUID.fromString("568df560-f242-4dd0-bb44-a0773b27d75d")),
    HOUR_AND_HALF(MessageHelper.HOUR_AND_HALF, 90, UUID.fromString("757f013a-d9fd-463c-974d-27e8f52f8d36")),
    TWO_HOURS(MessageHelper.TWO_HOURS, 120, UUID.fromString("3bcf42ec-e44c-4f29-b410-e65d5293c786")),
    THREE_HOURS(MessageHelper.THREE_HOURS, 180, UUID.fromString("a32279e9-c489-4a74-bd2f-bce9a2ca81fa")),
    FOUR_HOURS(MessageHelper.FOUR_HOURS, 240, UUID.fromString("cb9f45b0-0016-45d1-a436-6c109c548423")),
    FIVE_HOURS(MessageHelper.FIVE_HOURS, 300, UUID.fromString("508c756f-7987-4b51-bd94-bad6f1a671a3"));

    companion object {

        fun typeOf(queryData: String): SnoozeNotificationType? {
            return EnumSet.allOf(SnoozeNotificationType::class.java)
                .find { Objects.equals(queryData, it.queryData.toString()) }
        }

    }

}
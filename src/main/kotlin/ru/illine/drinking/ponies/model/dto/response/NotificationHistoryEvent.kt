package ru.illine.drinking.ponies.model.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import ru.illine.drinking.ponies.model.base.NotificationHistoryStatus
import ru.illine.drinking.ponies.model.base.WaterEntrySourceType
import java.time.Instant

@Schema(description = "Single notification journal entry")
data class NotificationHistoryEvent(
    @Schema(description = "Entry identifier, used by the edit request", example = "1042")
    val id: Long,
    @Schema(description = "Event time in ISO 8601 UTC format", example = "2026-07-25T07:00:00Z")
    val eventTime: Instant,
    @Schema(description = "Entry status as shown in the journal", example = "CONFIRMED")
    val status: NotificationHistoryStatus,
    @Schema(description = "Water amount in milliliters; 0 means confirmed without an amount", example = "300")
    val amountMl: Int,
    @Schema(
        description = "Entry origin. Only NOTIFICATION entries are returned for now; clients must tolerate new values",
        example = "NOTIFICATION",
    )
    val source: WaterEntrySourceType,
    @Schema(description = "Whether the entry is still within the edit window", example = "true")
    val editable: Boolean,
)

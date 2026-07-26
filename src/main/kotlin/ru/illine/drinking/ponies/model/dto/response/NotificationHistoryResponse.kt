package ru.illine.drinking.ponies.model.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Notification journal grouped by day in the user's timezone")
data class NotificationHistoryResponse(
    @Schema(description = "Days holding at least one entry, ascending; empty days are omitted")
    val days: List<NotificationHistoryDay>,
)

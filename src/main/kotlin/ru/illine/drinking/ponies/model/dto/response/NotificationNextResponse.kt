package ru.illine.drinking.ponies.model.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Next notification time")
data class NotificationNextResponse(
    @Schema(description = "Next notification time in ISO 8601 UTC format", example = "2025-01-01T14:00:00Z")
    val nextNotificationAt: Instant,
)

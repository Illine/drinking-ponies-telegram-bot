package ru.illine.drinking.ponies.model.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Notification pause state")
data class PauseStateResponse(
    @Schema(description = "Whether notifications are currently paused", example = "true")
    val paused: Boolean,
    @Schema(description = "Pause expiration time in ISO 8601 UTC format, null if not paused", example = "2025-01-01T18:30:00Z")
    val pauseUntil: Instant? = null,
)

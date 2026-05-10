package ru.illine.drinking.ponies.model.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Single water intake event")
data class WaterEntry(
    @Schema(description = "Event time in ISO 8601 UTC format", example = "2025-01-01T08:15:00Z")
    val eventTime: Instant,
    @Schema(description = "Amount of water consumed in milliliters", example = "250")
    val amountMl: Int,
)

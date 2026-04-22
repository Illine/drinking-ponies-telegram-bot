package ru.illine.drinking.ponies.model.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class IntervalResponse(
    @Schema(description = "Interval enum name", example = "HOUR")
    val interval: String,
    @Schema(description = "Human-readable interval name, e.g. '1 час', '30 минут'")
    val displayName: String,
    @Schema(description = "Interval duration in minutes", example = "60")
    val minutes: Long,
)
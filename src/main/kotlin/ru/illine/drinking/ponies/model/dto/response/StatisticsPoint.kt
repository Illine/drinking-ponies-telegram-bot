package ru.illine.drinking.ponies.model.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Single point on the statistics chart")
data class StatisticsPoint(
    @Schema(
        description = "Point label: \"HH:mm\" for DAY or \"YYYY-MM-DD\" for WEEK/MONTH",
        example = "2026-05-06"
    )
    val label: String,
    @Schema(description = "Aggregated water amount for this point in milliliters", example = "1800")
    val valueMl: Int,
)

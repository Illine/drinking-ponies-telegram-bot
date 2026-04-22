package ru.illine.drinking.ponies.model.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class QuietModeResponse(
    @Schema(description = "Quiet mode start time", example = "23:00", pattern = "HH:mm")
    val start: String,
    @Schema(description = "Quiet mode end time", example = "08:00", pattern = "HH:mm")
    val end: String,
)

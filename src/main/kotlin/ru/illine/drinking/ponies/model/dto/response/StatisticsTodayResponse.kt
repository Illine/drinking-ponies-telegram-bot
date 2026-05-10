package ru.illine.drinking.ponies.model.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Today's water intake events in user's timezone")
data class StatisticsTodayResponse(
    @Schema(description = "Individual water intake events sorted by event time")
    val entries: List<WaterEntry>
)

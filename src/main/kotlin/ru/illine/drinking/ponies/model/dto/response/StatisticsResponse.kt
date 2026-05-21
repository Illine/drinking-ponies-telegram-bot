package ru.illine.drinking.ponies.model.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Aggregated water consumption statistics for the requested period")
data class StatisticsResponse(
    @Schema(description = "Chart points ordered by time")
    val points: List<StatisticsPoint>,
    @Schema(description = "Current daily water goal in milliliters", example = "2000")
    val dailyGoalMl: Int,
    @Schema(description = "Average consumption per day in the period, milliliters", example = "1620")
    val averageMlPerDay: Int,
    @Schema(description = "Best day in the period; null for DAY and for empty periods")
    val bestDay: BestDayInfo?,
    @Schema(description = "Current consecutive-days streak meeting the daily goal", example = "3")
    val currentStreakDays: Int,
    @Schema(description = "Cute insight phrase")
    val insight: InsightInfo,
    @Schema(
        description = "Timestamp of the user's earliest water entry; used as the left bound of the calendar",
        example = "2026-04-15T10:30:00Z",
        nullable = true
    )
    val firstEntryAt: Instant?,
)

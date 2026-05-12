package ru.illine.drinking.ponies.model.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import ru.illine.drinking.ponies.model.base.StatisticsPeriodType

@Schema(description = "Aggregated water consumption statistics for the requested period")
data class StatisticsResponse(
    @Schema(description = "Requested period", example = "WEEK")
    val period: StatisticsPeriodType,
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
    @Schema(
        description = "Share of successful days [0.0..1.0]; null for DAY",
        example = "0.43"
    )
    val goalProgress: Double?,
    @Schema(description = "Cute insight phrase")
    val insight: InsightInfo,
)

package ru.illine.drinking.ponies.model.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "All user notification settings")
data class SettingResponse(
    @Schema(description = "Interval enum name", example = "HOUR")
    val interval: String? = null,
    @Schema(description = "Human-readable interval name, e.g. '1 час', '30 минут'")
    val intervalDisplayName: String? = null,
    @Schema(description = "Interval duration in minutes", example = "60")
    val intervalMinutes: Long? = null,
    @Schema(description = "Quiet mode start time in HH:mm format", example = "23:00", pattern = "HH:mm")
    val quietModeStart: String? = null,
    @Schema(description = "Quiet mode end time in HH:mm format", example = "08:00", pattern = "HH:mm")
    val quietModeEnd: String? = null,
    @Schema(description = "User's timezone in IANA format", example = "Europe/Moscow")
    val timezone: String? = null,
    @Schema(description = "Whether notifications are enabled", example = "true")
    val notificationActive: Boolean,
)
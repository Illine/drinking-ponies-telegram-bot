package ru.illine.drinking.ponies.model.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class NotificationStatusResponse(
    @Schema(description = "Whether notifications are enabled", example = "true")
    val active: Boolean,
)
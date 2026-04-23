package ru.illine.drinking.ponies.model.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class TimezoneResponse(
    @Schema(description = "User's timezone in IANA format", example = "Europe/Moscow")
    val timezone: String,
)
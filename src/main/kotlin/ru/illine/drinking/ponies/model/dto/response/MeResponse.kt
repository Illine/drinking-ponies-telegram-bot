package ru.illine.drinking.ponies.model.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Current user identity")
data class MeResponse(
    @Schema(description = "Telegram user id", example = "123456789")
    val telegramUserId: Long,
    @Schema(description = "Whether the user has admin privileges", example = "false")
    val isAdmin: Boolean,
)

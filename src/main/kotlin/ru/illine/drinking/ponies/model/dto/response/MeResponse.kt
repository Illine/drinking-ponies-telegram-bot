package ru.illine.drinking.ponies.model.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Current user identity")
data class MeResponse(
    // Kotlin field unified to externalUserId across the codebase (DPTB-140).
    // The JSON key is intentionally kept as "telegramUserId" - it is the public /users/me contract consumed by the MiniApp.
    @JsonProperty("telegramUserId")
    @Schema(description = "Telegram user id", example = "123456789")
    val externalUserId: Long,
    @Schema(description = "Whether the user has admin privileges", example = "false")
    val isAdmin: Boolean,
)

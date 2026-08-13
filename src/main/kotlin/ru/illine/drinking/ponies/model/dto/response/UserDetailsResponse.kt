package ru.illine.drinking.ponies.model.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Full user card for the admin page")
data class UserDetailsResponse(
    @Schema(description = "Internal identifier, the one admin endpoints operate on", example = "1042")
    val id: Long,
    @JsonProperty("telegramUserId")
    @Schema(description = "Telegram user id", example = "482719301")
    val externalUserId: Long,
    @Schema(description = "Telegram first name", example = "Алиса")
    val firstName: String?,
    @Schema(description = "Telegram last name, null if not set", example = "Петрова")
    val lastName: String?,
    @Schema(description = "Telegram username without the leading @, null if not set", example = "alice")
    val username: String?,
    @Schema(description = "Whether the user has admin privileges", example = "false")
    val isAdmin: Boolean,
    @Schema(description = "Whether the user has been banned", example = "false")
    val isBanned: Boolean,
    @Schema(description = "Whether the user has not been deleted", example = "true")
    val isActive: Boolean,
    @Schema(
        description = "Time of the last water intake or snooze, null if the user has never answered",
        example = "2026-08-07T21:14:03Z",
    )
    val lastActivity: Instant?,
    @Schema(description = "Timezone the user receives notifications in", example = "Europe/Moscow")
    val timeZone: String,
    @Schema(description = "Time the user was registered", example = "2026-05-10T01:22:00Z")
    val registeredAt: Instant,
)

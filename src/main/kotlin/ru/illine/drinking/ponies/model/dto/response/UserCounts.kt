package ru.illine.drinking.ponies.model.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description =
        "How many users fall into each status, counted over the whole search result and " +
            "regardless of the requested status - the admin page shows all counters at once",
)
data class UserCounts(
    @Schema(description = "Every user, deleted and banned included", example = "11")
    val all: Long,
    @Schema(description = "Neither deleted nor banned", example = "8")
    val active: Long,
    @Schema(description = "Deleted", example = "1")
    val inactive: Long,
    @Schema(description = "Banned", example = "2")
    val banned: Long,
)

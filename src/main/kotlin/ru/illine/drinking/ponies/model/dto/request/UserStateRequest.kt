package ru.illine.drinking.ponies.model.dto.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "User state update payload, one toggle of the admin card per field")
data class UserStateRequest(
    // Null means "leave as is" - ban and admin toggles will join as sibling fields.
    @Schema(
        description = "False soft deletes the user, true restores them",
        example = "false",
        nullable = true,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    )
    val isActive: Boolean?,
)

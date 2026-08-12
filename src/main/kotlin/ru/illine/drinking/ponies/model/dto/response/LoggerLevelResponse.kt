package ru.illine.drinking.ponies.model.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import ru.illine.drinking.ponies.model.base.LogLevelType

@Schema(description = "Log level of a single logger")
data class LoggerLevelResponse(
    @Schema(description = "Logger name", example = "SERVICE")
    val name: String,
    @Schema(
        description = "Level set explicitly on this logger; null when the logger inherits it",
        example = "DEBUG",
        nullable = true,
    )
    val configuredLevel: LogLevelType?,
    @Schema(
        description = "Level in effect, inherited from the closest configured parent; null for an unknown logger",
        example = "INFO",
        nullable = true,
    )
    val effectiveLevel: LogLevelType?,
    @Schema(
        description = "True for a logger the application declares itself, false for one coming from a library",
        example = "true",
    )
    val application: Boolean,
)

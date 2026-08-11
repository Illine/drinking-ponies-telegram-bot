package ru.illine.drinking.ponies.model.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.boot.logging.LogLevel

@Schema(description = "Log level of a single logger")
data class LoggerLevelResponse(
    @Schema(description = "Logger name", example = "SERVICE")
    val name: String,
    @Schema(
        description = "Level set explicitly on this logger; null when the logger inherits it",
        example = "DEBUG",
        nullable = true,
    )
    val configuredLevel: LogLevel?,
    @Schema(
        description = "Level in effect, inherited from the closest configured parent; null for an unknown logger",
        example = "INFO",
        nullable = true,
    )
    val effectiveLevel: LogLevel?,
)

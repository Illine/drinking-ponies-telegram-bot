package ru.illine.drinking.ponies.model.dto.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.boot.logging.LogLevel

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Log level to apply to a logger")
data class LoggerLevelRequest(
    @Schema(
        description = "Level the logger is set to; OFF silences it completely, FATAL is applied as ERROR",
        example = "DEBUG",
    )
    val level: LogLevel,
)

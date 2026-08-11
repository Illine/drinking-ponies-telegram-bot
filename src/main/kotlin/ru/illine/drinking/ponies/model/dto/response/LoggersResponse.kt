package ru.illine.drinking.ponies.model.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Loggers of the application, followed by any other logger with an explicit level")
data class LoggersResponse(
    @Schema(description = "Our own loggers in declaration order, then the explicitly configured ones")
    val loggers: List<LoggerLevelResponse>,
)

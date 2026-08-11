package ru.illine.drinking.ponies.model.dto.internal

import org.springframework.boot.logging.LogLevel

data class LoggerLevelDto(
    val name: String,
    val configuredLevel: LogLevel?,
    val effectiveLevel: LogLevel?,
)

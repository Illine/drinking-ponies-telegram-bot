package ru.illine.drinking.ponies.model.dto.internal

import ru.illine.drinking.ponies.model.base.LogLevelType

data class LoggerLevelDto(
    val name: String,
    val configuredLevel: LogLevelType?,
    val effectiveLevel: LogLevelType?,
    val application: Boolean,
)

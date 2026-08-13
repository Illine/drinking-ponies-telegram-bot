package ru.illine.drinking.ponies.model.base

import org.springframework.boot.logging.LogLevel

enum class LogLevelType(
    val level: LogLevel,
) {
    TRACE(LogLevel.TRACE),
    DEBUG(LogLevel.DEBUG),
    INFO(LogLevel.INFO),
    WARN(LogLevel.WARN),
    ERROR(LogLevel.ERROR),
    OFF(LogLevel.OFF),
    ;

    companion object {
        fun of(level: LogLevel?): LogLevelType? = entries.find { it.level == level }
    }
}

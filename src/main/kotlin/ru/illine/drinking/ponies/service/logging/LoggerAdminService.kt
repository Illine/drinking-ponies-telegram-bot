package ru.illine.drinking.ponies.service.logging

import org.springframework.boot.logging.LogLevel
import ru.illine.drinking.ponies.model.dto.internal.LoggerLevelDto

interface LoggerAdminService {
    fun getKnownLevels(): List<LoggerLevelDto>

    fun getLevel(name: String): LoggerLevelDto

    fun setLevel(
        name: String,
        level: LogLevel,
        actorId: Long,
    ): LoggerLevelDto

    fun resetLevels(actorId: Long): List<LoggerLevelDto>
}

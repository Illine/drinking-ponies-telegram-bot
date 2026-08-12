package ru.illine.drinking.ponies.service.logging

import ru.illine.drinking.ponies.model.base.LogLevelType
import ru.illine.drinking.ponies.model.dto.internal.LoggerLevelDto

interface LoggerAdminService {
    fun getKnownLevels(): List<LoggerLevelDto>

    fun getLevel(name: String): LoggerLevelDto

    fun setLevel(
        name: String,
        level: LogLevelType,
        actorId: Long,
    ): LoggerLevelDto

    fun resetLevels(actorId: Long): List<LoggerLevelDto>
}

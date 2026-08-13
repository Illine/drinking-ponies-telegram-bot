package ru.illine.drinking.ponies.mapper

import io.mcarle.konvert.api.Konverter
import ru.illine.drinking.ponies.model.dto.internal.LoggerLevelDto
import ru.illine.drinking.ponies.model.dto.response.LoggerLevelResponse

@Konverter
interface LoggerLevelResponseMapper {
    fun toResponse(dto: LoggerLevelDto): LoggerLevelResponse

    companion object : LoggerLevelResponseMapper by Konverter.get<LoggerLevelResponseMapper>()
}

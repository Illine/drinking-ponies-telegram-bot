package ru.illine.drinking.ponies.mapper

import io.mcarle.konvert.api.Konverter
import ru.illine.drinking.ponies.model.dto.internal.PauseStateDto
import ru.illine.drinking.ponies.model.dto.response.PauseStateResponse

@Konverter
interface PauseStateResponseMapper {
    fun toResponse(dto: PauseStateDto): PauseStateResponse

    companion object : PauseStateResponseMapper by Konverter.get<PauseStateResponseMapper>()
}

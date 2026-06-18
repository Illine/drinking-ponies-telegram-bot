package ru.illine.drinking.ponies.builder

import io.mcarle.konvert.api.Konverter
import ru.illine.drinking.ponies.model.dto.SettingDto
import ru.illine.drinking.ponies.model.dto.response.SettingResponse

@Konverter
interface SettingResponseMapper {
    fun toResponse(dto: SettingDto): SettingResponse
}

object SettingResponseBuilder {
    private val mapper: SettingResponseMapper = Konverter.get<SettingResponseMapper>()

    fun toResponse(dto: SettingDto): SettingResponse = mapper.toResponse(dto)
}

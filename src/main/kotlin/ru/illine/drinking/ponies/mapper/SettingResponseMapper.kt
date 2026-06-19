package ru.illine.drinking.ponies.mapper

import io.mcarle.konvert.api.Konverter
import ru.illine.drinking.ponies.model.dto.SettingDto
import ru.illine.drinking.ponies.model.dto.response.SettingResponse

@Konverter
interface SettingResponseMapper {
    fun toResponse(dto: SettingDto): SettingResponse

    companion object : SettingResponseMapper by Konverter.get<SettingResponseMapper>()
}

package ru.illine.drinking.ponies.mapper

import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Mapping
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserDto
import ru.illine.drinking.ponies.model.entity.TelegramUserEntity

@Konverter
interface TelegramUserMapper {
    fun toDto(entity: TelegramUserEntity): TelegramUserDto

    @Konvert(mappings = [Mapping(target = "id", ignore = true)])
    fun toNewEntity(dto: TelegramUserDto): TelegramUserEntity

    companion object : TelegramUserMapper by Konverter.get<TelegramUserMapper>()
}

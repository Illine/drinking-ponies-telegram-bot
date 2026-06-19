package ru.illine.drinking.ponies.mapper

import io.mcarle.konvert.api.Konverter
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserDto
import ru.illine.drinking.ponies.model.entity.TelegramUserEntity

@Konverter
interface TelegramUserMapper {
    fun toDto(entity: TelegramUserEntity): TelegramUserDto
    fun toEntity(dto: TelegramUserDto): TelegramUserEntity

    companion object : TelegramUserMapper by Konverter.get<TelegramUserMapper>()
}

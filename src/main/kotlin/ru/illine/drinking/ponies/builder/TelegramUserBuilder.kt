package ru.illine.drinking.ponies.builder

import io.mcarle.konvert.api.Konverter
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserDto
import ru.illine.drinking.ponies.model.entity.TelegramUserEntity

@Konverter
interface TelegramUserMapper {
    fun toDto(entity: TelegramUserEntity): TelegramUserDto
    fun toEntity(dto: TelegramUserDto): TelegramUserEntity
}

object TelegramUserBuilder {
    private val mapper: TelegramUserMapper = Konverter.get<TelegramUserMapper>()

    fun toDto(entity: TelegramUserEntity): TelegramUserDto = mapper.toDto(entity)

    fun toEntity(dto: TelegramUserDto): TelegramUserEntity = mapper.toEntity(dto)
}

package ru.illine.drinking.ponies.mapper

import io.mcarle.konvert.api.Konverter
import ru.illine.drinking.ponies.model.dto.internal.TelegramAuthUserDto
import ru.illine.drinking.ponies.util.telegram.TelegramInitDataUser

@Konverter
interface TelegramAuthUserMapper {
    fun toDto(initDataUser: TelegramInitDataUser): TelegramAuthUserDto

    companion object : TelegramAuthUserMapper by Konverter.get<TelegramAuthUserMapper>()
}

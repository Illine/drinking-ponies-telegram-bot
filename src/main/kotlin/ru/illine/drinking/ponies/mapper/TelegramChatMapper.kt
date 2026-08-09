package ru.illine.drinking.ponies.mapper

import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Mapping
import ru.illine.drinking.ponies.model.dto.internal.TelegramChatDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserDto
import ru.illine.drinking.ponies.model.entity.TelegramChatEntity
import ru.illine.drinking.ponies.model.entity.TelegramUserEntity

@Konverter
interface TelegramChatMapper {
    fun toDto(
        @Konverter.Source chat: TelegramChatEntity,
        telegramUser: TelegramUserDto,
    ): TelegramChatDto

    @Konvert(mappings = [Mapping(target = "id", ignore = true)])
    fun toNewEntity(
        @Konverter.Source chat: TelegramChatDto,
        telegramUser: TelegramUserEntity,
    ): TelegramChatEntity

    companion object : TelegramChatMapper by Konverter.get<TelegramChatMapper>()
}

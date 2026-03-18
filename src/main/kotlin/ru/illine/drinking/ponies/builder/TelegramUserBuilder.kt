package ru.illine.drinking.ponies.builder

import ru.illine.drinking.ponies.model.dto.internal.TelegramUserDto
import ru.illine.drinking.ponies.model.entity.TelegramUserEntity

object TelegramUserBuilder {
    fun toDto(entity: TelegramUserEntity): TelegramUserDto =
        TelegramUserDto(
            id = entity.id,
            externalUserId = entity.externalUserId,
            userTimeZone = entity.userTimeZone,
            created = entity.created,
            deleted = entity.deleted,
        )

    fun toEntity(dto: TelegramUserDto): TelegramUserEntity =
        TelegramUserEntity(
            id = dto.id,
            externalUserId = dto.externalUserId,
            userTimeZone = dto.userTimeZone,
            created = dto.created,
            deleted = dto.deleted,
        )
}

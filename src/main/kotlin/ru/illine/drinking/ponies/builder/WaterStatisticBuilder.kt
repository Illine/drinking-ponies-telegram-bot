package ru.illine.drinking.ponies.builder

import ru.illine.drinking.ponies.model.dto.internal.TelegramUserDto
import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto
import ru.illine.drinking.ponies.model.entity.TelegramUserEntity
import ru.illine.drinking.ponies.model.entity.WaterStatisticEntity

object WaterStatisticBuilder {

    fun toDto(entity: WaterStatisticEntity, user: TelegramUserDto): WaterStatisticDto =
        WaterStatisticDto(
            id = entity.id,
            telegramUser = user,
            eventTime = entity.eventTime,
            eventType = entity.eventType,
            waterAmountMl = entity.waterAmountMl
        )

    fun toEntity(dto: WaterStatisticDto, user: TelegramUserEntity): WaterStatisticEntity =
        WaterStatisticEntity(
            id = dto.id,
            telegramUser = user,
            eventTime = dto.eventTime,
            eventType = dto.eventType,
            waterAmountMl = dto.waterAmountMl
        )

}
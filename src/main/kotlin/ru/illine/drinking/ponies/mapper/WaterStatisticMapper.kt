package ru.illine.drinking.ponies.mapper

import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Mapping
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserDto
import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto
import ru.illine.drinking.ponies.model.dto.response.WaterEntry
import ru.illine.drinking.ponies.model.entity.TelegramUserEntity
import ru.illine.drinking.ponies.model.entity.WaterStatisticEntity

@Konverter
interface WaterStatisticMapper {
    fun toDto(
        @Konverter.Source entity: WaterStatisticEntity,
        telegramUser: TelegramUserDto,
    ): WaterStatisticDto

    fun toEntity(
        @Konverter.Source dto: WaterStatisticDto,
        telegramUser: TelegramUserEntity,
    ): WaterStatisticEntity

    @Konvert(
        mappings = [
            Mapping(source = "waterAmountMl", target = "amountMl"),
            Mapping(target = "eventTime", expression = "it.eventTime.toInstant(java.time.ZoneOffset.UTC)"),
        ]
    )
    fun toWaterEntry(dto: WaterStatisticDto): WaterEntry

    companion object : WaterStatisticMapper by Konverter.get<WaterStatisticMapper>()
}

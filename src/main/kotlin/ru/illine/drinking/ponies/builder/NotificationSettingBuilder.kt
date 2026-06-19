package ru.illine.drinking.ponies.builder

import io.mcarle.konvert.api.Konverter
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramChatDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserDto
import ru.illine.drinking.ponies.model.entity.NotificationSettingEntity
import ru.illine.drinking.ponies.model.entity.TelegramChatEntity
import ru.illine.drinking.ponies.model.entity.TelegramUserEntity

@Konverter
interface NotificationSettingMapper {
    fun toDto(
        @Konverter.Source setting: NotificationSettingEntity,
        telegramUser: TelegramUserDto,
        telegramChat: TelegramChatDto,
    ): NotificationSettingDto

    fun toEntity(
        @Konverter.Source setting: NotificationSettingDto,
        telegramUser: TelegramUserEntity,
        telegramChat: TelegramChatEntity,
    ): NotificationSettingEntity
}

object NotificationSettingBuilder {
    private val mapper: NotificationSettingMapper = Konverter.get<NotificationSettingMapper>()

    fun toDto(
        setting: NotificationSettingEntity,
        user: TelegramUserDto,
        chat: TelegramChatDto
    ): NotificationSettingDto =
        mapper.toDto(setting, user, chat)

    fun toEntity(
        setting: NotificationSettingDto,
        user: TelegramUserEntity,
        chat: TelegramChatEntity
    ): NotificationSettingEntity =
        mapper.toEntity(setting, user, chat)
}

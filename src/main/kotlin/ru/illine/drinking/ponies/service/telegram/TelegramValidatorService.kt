package ru.illine.drinking.ponies.service.telegram

import ru.illine.drinking.ponies.model.dto.TelegramUserDto

interface TelegramValidatorService {

    fun verifySignature(initData: String): Boolean

    fun map(initData: String): TelegramUserDto

}

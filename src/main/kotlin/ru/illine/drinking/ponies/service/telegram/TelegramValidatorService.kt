package ru.illine.drinking.ponies.service.telegram

import ru.illine.drinking.ponies.model.dto.internal.TelegramAuthUserDto

interface TelegramValidatorService {
    fun verifySignature(initData: String): Boolean

    fun map(initData: String): TelegramAuthUserDto
}

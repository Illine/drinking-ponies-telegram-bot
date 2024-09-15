package ru.illine.drinking.ponies.service

import ru.illine.drinking.ponies.model.dto.TelegramUserDto
import java.time.Duration

interface TelegramValidatorService {

    fun verifySignature(
        initData: String,
        expirationTime: Duration = Duration.ofMinutes(10),
    ): Boolean

    fun map(initData: String): TelegramUserDto

}
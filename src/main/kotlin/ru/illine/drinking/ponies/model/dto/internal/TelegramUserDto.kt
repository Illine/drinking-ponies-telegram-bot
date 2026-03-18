package ru.illine.drinking.ponies.model.dto.internal

import java.time.LocalDateTime

data class TelegramUserDto(
    var id: Long? = null,

    var externalUserId: Long,

    var userTimeZone: String,

    var created: LocalDateTime = LocalDateTime.now(),

    var deleted: Boolean = false
) {
    companion object {
        fun create(externalUserId: Long): TelegramUserDto =
            TelegramUserDto(
                externalUserId = externalUserId,
                userTimeZone = "Europe/Moscow",
            )
    }
}
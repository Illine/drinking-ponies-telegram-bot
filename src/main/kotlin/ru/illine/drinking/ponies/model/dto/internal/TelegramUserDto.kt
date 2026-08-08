package ru.illine.drinking.ponies.model.dto.internal

import java.time.LocalDateTime
import java.time.ZoneOffset

data class TelegramUserDto(
    var id: Long? = null,
    var externalUserId: Long,
    var userTimeZone: String,
    var isAdmin: Boolean = false,
    var isBanned: Boolean = false,
    var firstName: String? = null,
    var lastName: String? = null,
    var username: String? = null,
    var created: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
    var deleted: Boolean = false,
) {
    companion object {
        fun create(
            externalUserId: Long,
            profile: TelegramUserProfileDto = TelegramUserProfileDto(),
        ): TelegramUserDto =
            TelegramUserDto(
                externalUserId = externalUserId,
                userTimeZone = "Europe/Moscow",
                firstName = profile.firstName,
                lastName = profile.lastName,
                username = profile.username,
            )
    }
}

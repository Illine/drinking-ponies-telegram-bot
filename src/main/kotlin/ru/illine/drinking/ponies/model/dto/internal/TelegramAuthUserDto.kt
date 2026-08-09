package ru.illine.drinking.ponies.model.dto.internal

data class TelegramAuthUserDto(
    val id: Long? = null,
    val externalUserId: Long,
    val firstName: String? = null,
    val lastName: String? = null,
    val username: String? = null,
    val isAdmin: Boolean = false,
)

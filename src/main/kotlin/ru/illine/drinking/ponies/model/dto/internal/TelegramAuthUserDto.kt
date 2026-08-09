package ru.illine.drinking.ponies.model.dto.internal

data class TelegramAuthUserDto(
    val externalUserId: Long,
    val firstName: String? = null,
    val lastName: String? = null,
    val username: String? = null,
    val isAdmin: Boolean = false,
    val isBanned: Boolean = false,
    val isActive: Boolean = false,
)

package ru.illine.drinking.ponies.model.dto.internal

data class UserAccessDto(
    val id: Long? = null,
    val externalUserId: Long,
    val isAdmin: Boolean = false,
    val isBanned: Boolean = false,
    val isDeleted: Boolean = false,
)

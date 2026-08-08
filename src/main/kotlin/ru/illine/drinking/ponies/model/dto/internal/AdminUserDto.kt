package ru.illine.drinking.ponies.model.dto.internal

import java.time.LocalDateTime

data class AdminUserDto(
    val id: Long,
    val externalUserId: Long,
    val firstName: String?,
    val lastName: String?,
    val username: String?,
    val isAdmin: Boolean,
    val isBanned: Boolean,
    val deleted: Boolean,
    val timeZone: String,
    val created: LocalDateTime,
    val lastActivity: LocalDateTime?,
)

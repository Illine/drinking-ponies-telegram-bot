package ru.illine.drinking.ponies.dao.repository.projection

import java.time.LocalDateTime

interface AdminUserProjection {
    val id: Long
    val externalUserId: Long
    val firstName: String?
    val lastName: String?
    val username: String?
    val admin: Boolean
    val banned: Boolean
    val deleted: Boolean
    val timeZone: String
    val created: LocalDateTime
    val lastActivity: LocalDateTime?
}

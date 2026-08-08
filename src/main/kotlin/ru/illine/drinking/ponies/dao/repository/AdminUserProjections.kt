package ru.illine.drinking.ponies.dao.repository

import java.time.LocalDateTime

// Property names must match the aliases of the native query - hence admin/banned
// instead of isAdmin/isBanned, an "is" prefix would rename the getter.
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

interface UserCountsProjection {
    val all: Long
    val active: Long
    val inactive: Long
    val banned: Long
}

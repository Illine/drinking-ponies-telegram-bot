package ru.illine.drinking.ponies.model.base

enum class AdminUserStatusFilter(
    val deleted: Boolean?,
    val banned: Boolean?,
) {
    ALL(null, null),
    ACTIVE(false, false),
    INACTIVE(true, false),
    BANNED(null, true),
}

package ru.illine.drinking.ponies.model.base

// Categories are mutually exclusive, so the counters add up to ALL: a ban wins over everything
// else, so a user who is both banned and deleted counts as BANNED only.
// Null means the column is not constrained.
enum class AdminUserStatusFilter(
    val deleted: Boolean?,
    val banned: Boolean?,
) {
    ALL(null, null),
    ACTIVE(false, false),
    INACTIVE(true, false),
    BANNED(null, true),
}

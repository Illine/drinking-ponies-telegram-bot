package ru.illine.drinking.ponies.model.dto.internal

// isDeleted is admin-facing state, not a gate: nothing denies access by it. A soft deleted user
// keeps working and is brought back by /start; blocking belongs to the ban feature.
data class UserAccessDto(
    val isAdmin: Boolean = false,
    val isBanned: Boolean = false,
    val isDeleted: Boolean = false,
)

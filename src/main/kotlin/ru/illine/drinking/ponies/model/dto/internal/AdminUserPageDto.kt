package ru.illine.drinking.ponies.model.dto.internal

data class AdminUserPageDto(
    val users: List<AdminUserDto>,
    val page: Int,
    val size: Int,
    val total: Long,
    val counts: UserCountsDto,
)

package ru.illine.drinking.ponies.model.dto.internal

data class UserCountsDto(
    val all: Long,
    val active: Long,
    val inactive: Long,
    val banned: Long,
)

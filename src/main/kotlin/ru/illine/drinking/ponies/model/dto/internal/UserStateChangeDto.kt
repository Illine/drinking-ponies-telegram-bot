package ru.illine.drinking.ponies.model.dto.internal

data class UserStateChangeDto(
    val deleted: Boolean? = null,
    val banned: Boolean? = null,
)

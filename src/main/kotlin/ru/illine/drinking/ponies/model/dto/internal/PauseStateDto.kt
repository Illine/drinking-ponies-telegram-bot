package ru.illine.drinking.ponies.model.dto.internal

import java.time.Instant

data class PauseStateDto(
    val paused: Boolean,
    val pauseUntil: Instant? = null,
)

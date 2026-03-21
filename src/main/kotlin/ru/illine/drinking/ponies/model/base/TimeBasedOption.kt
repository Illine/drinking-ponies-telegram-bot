package ru.illine.drinking.ponies.model.base

import java.util.*

interface TimeBasedOption {
    val displayName: String
    val minutes: Long
    val queryData: UUID
}

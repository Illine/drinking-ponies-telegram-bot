package ru.illine.drinking.ponies.model.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SettingDto(
    val interval: String? = null,
    val intervalDisplayName: String? = null,
    val intervalMinutes: Long? = null,
    val quietModeStart: String? = null,
    val quietModeEnd: String? = null,
    val timezone: String? = null,
    val notificationActive: Boolean,
)
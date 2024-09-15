package ru.illine.drinking.ponies.model.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class TelegramUserDto(
    @JsonProperty("id")
    val telegramId: Long,
    @JsonProperty("first_name")
    val firstName: String?,
    @JsonProperty("last_name")
    val lastName: String?,
    @JsonProperty("username")
    val username: String?,
    @JsonProperty("language_code")
    val languageCode: String = "en",
    @JsonProperty("is_premium")
    val isPremium: Boolean = false,
    @JsonProperty("allows_write_to_pm")
    val allowsWriteToPm: Boolean = false
)
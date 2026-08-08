package ru.illine.drinking.ponies.model.dto.request

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class TelegramInitDataUser(
    @JsonProperty("id")
    val externalUserId: Long,
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
    val allowsWriteToPm: Boolean = false,
    @JsonIgnore
    val isAdmin: Boolean = false,
    @JsonIgnore
    val isBanned: Boolean = false,
    @JsonIgnore
    val isActive: Boolean = false,
)

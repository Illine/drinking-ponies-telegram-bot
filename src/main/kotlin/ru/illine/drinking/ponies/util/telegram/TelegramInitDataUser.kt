package ru.illine.drinking.ponies.util.telegram

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
)

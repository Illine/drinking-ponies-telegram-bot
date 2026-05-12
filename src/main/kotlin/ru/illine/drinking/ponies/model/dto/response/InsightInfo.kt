package ru.illine.drinking.ponies.model.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Cute pre-rendered insight phrase for the period")
data class InsightInfo(
    @Schema(
        description = "Localized text with placeholders already substituted",
        example = "Котик, ты пьёшь водицу 3 дней подряд - так держать!"
    )
    val text: String,
)

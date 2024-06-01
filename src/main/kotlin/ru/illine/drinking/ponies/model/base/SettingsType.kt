package ru.illine.drinking.ponies.model.base

import java.util.UUID

@Suppress("unused")
enum class SettingsType(
    val displayName: String,
    val queryData: UUID,
    val visible: Boolean
) {
    DELAY_NOTIFICATION("Периодичность уведомлений", UUID.fromString("fd789961-0706-47fa-869d-a17a5ecc871b"), true),
    SILENCE_TIME("Расписание тихого режима", UUID.fromString("2486d63e-8893-40c5-bb4f-e45caf75b5c9"), false),
    TIMEZONE("Часовой пояс", UUID.fromString("f99bf271-8e39-4a62-87d7-13fbcbc85355"), false)
}

package ru.illine.drinking.ponies.model.base

import java.util.UUID

@Suppress("unused")
enum class SettingsType(
    val displayName: String,
    val queryData: UUID
) {
    DELAY_NOTIFICATION("Периодичность уведомлений", UUID.randomUUID()),
    SILENCE_TIME("Расписание тихого режима", UUID.randomUUID()),
    TIMEZONE("Часовой пояс", UUID.randomUUID())
}

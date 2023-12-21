package ru.illine.drinking.ponies.model.base

import java.util.UUID

@Suppress("unused")
enum class SettingsType(
    val displayName: String,
    val queryData: UUID,
    val visible: Boolean
) {
    DELAY_NOTIFICATION("Периодичность уведомлений", UUID.randomUUID(), true),
    SILENCE_TIME("Расписание тихого режима", UUID.randomUUID(), false),
    TIMEZONE("Часовой пояс", UUID.randomUUID(), false)
}

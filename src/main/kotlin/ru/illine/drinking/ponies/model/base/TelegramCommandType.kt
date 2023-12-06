package ru.illine.drinking.ponies.model.base

enum class TelegramCommandType(
    val command: String,
    val info: String,
    val descriptions: String,
    val visible: Boolean
) {

    START("start", "Start command", "Начало взаимодействия с ботом", false),
    SETTINGS("settings", "User's settings command", "Изменение настроек уведомлений", true),
    RESUME("resume", "Resuming of notifications for a user", "Возобновить отправку уведомления", true),
    STOP("stop", "Stopping of notifications for a user", "Остановить отправку уведомлений", true)
}
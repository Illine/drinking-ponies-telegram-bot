package ru.illine.drinking.ponies.model.base

enum class TelegramCommandType(
    val order: Int,
    val command: String,
    val info: String,
    val descriptions: String,
    val visible: Boolean
) {

    START(1, "start", "Start command", "Начало взаимодействия с ботом", false),
    RESUME(2, "resume", "Resuming of notifications for a user", "Возобновить отправку уведомления", true),
    STOP(3, "stop", "Stopping of notifications for a user", "Остановить отправку уведомлений", true),
    SETTINGS(4, "settings", "User's settings command", "Изменение настроек уведомлений", true);

    override fun toString(): String = command

}
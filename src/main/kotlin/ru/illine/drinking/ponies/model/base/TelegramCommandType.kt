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
    SETTINGS(4, "settings", "User's settings command", "Изменение настроек уведомлений", true),
    VERSION(5, "version", "Shows a current version of the bot", "Показать текущую версию бота", false),
    SNOOZE(6, "snooze", "Allows to delay the sending of notifications", "Позволяет отложить отправку уведомлений", true);

    override fun toString(): String = command

}
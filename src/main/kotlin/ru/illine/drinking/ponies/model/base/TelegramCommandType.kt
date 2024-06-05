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
    PAUSE(4, "pause", "Allows to delay the sending of notifications", "Позволяет отложить отправку уведомлений", true),
    SETTINGS(5, "settings", "User's settings command", "Изменение настроек уведомлений", true),
    VERSION(99, "version", "Shows a current version of the bot", "Показать текущую версию бота", false);

    override fun toString(): String = command

}
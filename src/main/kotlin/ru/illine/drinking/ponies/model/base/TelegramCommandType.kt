package ru.illine.drinking.ponies.model.base

enum class TelegramCommandType(
    val order: Int,
    val command: String,
    val info: String,
    val descriptions: String,
    val visible: Boolean
) {

    START(1, "start", "Start command", "Начало взаимодействия с ботом", false),
    PAUSE(2, "pause", "Allows to delay the sending of notifications", "Позволяет отложить отправку уведомлений", true),
    VERSION(99, "version", "Shows a current version of the bot", "Показать текущую версию бота", false);

    override fun toString(): String = command

}
package ru.illine.drinking.ponies.model.base

enum class TelegramCommandType(
    val command: String,
    val info: String,
    val descriptions: String,
    val visible: Boolean
) {

    START("start", "Start command", "Команда для начала взаимодействия с ботом", false),
    SETTINGS("settings", "User's settings command", "Команда для изменения настроек напоминания", true)
}
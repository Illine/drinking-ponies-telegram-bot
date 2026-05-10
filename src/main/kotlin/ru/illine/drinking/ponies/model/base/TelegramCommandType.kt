package ru.illine.drinking.ponies.model.base

enum class TelegramCommandType(
    val command: String,
    val info: String,
) {

    START("start", "Start command");

    override fun toString(): String = command

}

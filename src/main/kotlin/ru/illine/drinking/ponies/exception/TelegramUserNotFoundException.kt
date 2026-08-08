package ru.illine.drinking.ponies.exception

class TelegramUserNotFoundException(
    message: String,
) : NotFoundException("user not found", message)

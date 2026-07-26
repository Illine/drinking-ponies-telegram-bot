package ru.illine.drinking.ponies.exception

class NotificationSettingsNotFoundException(
    message: String,
) : NotFoundException("notification settings not found", message)

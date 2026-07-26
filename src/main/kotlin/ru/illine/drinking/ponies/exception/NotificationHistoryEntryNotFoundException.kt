package ru.illine.drinking.ponies.exception

class NotificationHistoryEntryNotFoundException(
    message: String,
) : NotFoundException("notification history entry not found", message)

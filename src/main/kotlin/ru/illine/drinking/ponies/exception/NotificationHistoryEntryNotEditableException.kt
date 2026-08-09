package ru.illine.drinking.ponies.exception

class NotificationHistoryEntryNotEditableException(
    message: String,
) : ConflictException("notification history entry is not editable", message)

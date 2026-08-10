package ru.illine.drinking.ponies.exception

class LastAdminException(
    message: String,
) : ConflictException("you cannot revoke the privileges of the last admin", message)

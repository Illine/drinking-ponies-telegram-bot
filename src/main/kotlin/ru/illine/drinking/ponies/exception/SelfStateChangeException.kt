package ru.illine.drinking.ponies.exception

class SelfStateChangeException(
    message: String,
) : ConflictException("you cannot change the state of your own account", message)

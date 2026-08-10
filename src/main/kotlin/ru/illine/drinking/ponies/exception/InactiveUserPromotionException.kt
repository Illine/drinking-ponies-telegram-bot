package ru.illine.drinking.ponies.exception

class InactiveUserPromotionException(
    message: String,
) : ConflictException("you cannot grant admin privileges to a banned or deleted user", message)

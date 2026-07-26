package ru.illine.drinking.ponies.exception

abstract class NotFoundException(
    val clientMessage: String,
    message: String,
) : RuntimeException(message)

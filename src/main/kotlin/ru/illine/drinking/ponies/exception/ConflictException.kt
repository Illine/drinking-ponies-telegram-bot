package ru.illine.drinking.ponies.exception

abstract class ConflictException(
    val clientMessage: String,
    message: String,
) : RuntimeException(message)

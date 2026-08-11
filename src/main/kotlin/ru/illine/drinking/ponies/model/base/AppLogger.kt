package ru.illine.drinking.ponies.model.base

import org.slf4j.Logger
import org.slf4j.LoggerFactory

enum class AppLogger(
    val value: String,
) {
    ROOT(Logger.ROOT_LOGGER_NAME),
    AUDIT("AUDIT"),
    API("API"),
    BOT("BOT"),
    SCHEDULER("SCHEDULER"),
    SERVICE("SERVICE"),
    ACCESS_SERVICE("ACCESS-SERVICE"),
    STRATEGY("STRATEGY"),
    INTERCEPTOR("INTERCEPTOR"),
    EXCEPTION_HANDLER("EXCEPTION-HANDLER"),
    HELPER("HELPER"),
    SQL("SQL"),
    ;

    val logger: Logger = LoggerFactory.getLogger(value)
}

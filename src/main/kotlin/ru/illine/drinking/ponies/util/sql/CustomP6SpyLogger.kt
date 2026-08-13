package ru.illine.drinking.ponies.util.sql

import com.p6spy.engine.spy.appender.Slf4JLogger
import org.springframework.util.ReflectionUtils
import ru.illine.drinking.ponies.model.base.AppLogger

class CustomP6SpyLogger : Slf4JLogger() {
    companion object {
        private const val SLF4J_LOGGER_NAME = "logger"
        private val LOGGER = AppLogger.SQL.logger
    }

    init {
        overrideDefaultLoggerViaReflection()
    }

    private fun overrideDefaultLoggerViaReflection() {
        ReflectionUtils
            .findField(this.javaClass, SLF4J_LOGGER_NAME)
            ?.let {
                ReflectionUtils.makeAccessible(it)
                ReflectionUtils.setField(it, this, LOGGER)
            }
    }
}

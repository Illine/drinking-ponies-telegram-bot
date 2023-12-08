package ru.illine.drinking.ponies.util.sql

import com.p6spy.engine.spy.appender.Slf4JLogger
import org.slf4j.LoggerFactory
import org.springframework.util.ReflectionUtils

class CustomP6SpyLogger : Slf4JLogger() {

    companion object {
        private const val SLF4J_LOGGER_NAME = "log"
        private val LOGGER = LoggerFactory.getLogger("SQL")
    }

    init {
        overrideDefaultLoggerViaReflection()
    }

    private fun overrideDefaultLoggerViaReflection() {
        ReflectionUtils.findField(this.javaClass, SLF4J_LOGGER_NAME)
            ?.let {
                ReflectionUtils.makeAccessible(it)
                ReflectionUtils.setField(it, this, LOGGER)
            }
    }
}

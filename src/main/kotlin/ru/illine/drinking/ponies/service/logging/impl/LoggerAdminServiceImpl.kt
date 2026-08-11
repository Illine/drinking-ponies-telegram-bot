package ru.illine.drinking.ponies.service.logging.impl

import org.springframework.boot.logging.LogLevel
import org.springframework.boot.logging.LoggingSystem
import org.springframework.stereotype.Service
import ru.illine.drinking.ponies.model.base.AppLogger
import ru.illine.drinking.ponies.model.dto.internal.LoggerLevelDto
import ru.illine.drinking.ponies.service.logging.LoggerAdminService

@Service
class LoggerAdminServiceImpl(
    private val loggingSystem: LoggingSystem,
) : LoggerAdminService {
    private val logger = AppLogger.AUDIT.logger

    private val startupLevels: Map<String, LogLevel?>

    init {
        loggingSystem.setLogLevel(logger.name, LogLevel.INFO)
        startupLevels = loggingSystem.loggerConfigurations.associate { it.name to it.configuredLevel }
    }

    override fun getKnownLevels(): List<LoggerLevelDto> =
        (AppLogger.entries.map { it.value } + configuredLoggerNames())
            .distinct()
            .filterNot { it == logger.name }
            .map { readLevel(it) }

    override fun getLevel(name: String): LoggerLevelDto = readLevel(name)

    override fun setLevel(
        name: String,
        level: LogLevel,
        actorId: Long,
    ): LoggerLevelDto {
        require(name != logger.name) { "The audit logger cannot be reconfigured through the API" }

        logger.warn(
            "Admin [{}] sets logger [{}] to [{}], effective level was [{}]",
            actorId,
            name,
            level,
            loggingSystem.getLoggerConfiguration(name)?.effectiveLevel,
        )
        loggingSystem.setLogLevel(name, level)

        return readLevel(name)
    }

    override fun resetLevels(actorId: Long): List<LoggerLevelDto> {
        logger.warn("Admin [{}] returns every logger to the level it had at startup", actorId)
        loggingSystem
            .loggerConfigurations
            .filter { it.configuredLevel != startupLevels[it.name] }
            .forEach { loggingSystem.setLogLevel(it.name, startupLevels[it.name]) }

        return getKnownLevels()
    }

    private fun configuredLoggerNames(): List<String> =
        loggingSystem
            .loggerConfigurations
            .filter { it.configuredLevel != null }
            .map { it.name }

    private fun readLevel(name: String): LoggerLevelDto =
        loggingSystem.getLoggerConfiguration(name).let {
            LoggerLevelDto(
                name = name,
                configuredLevel = it?.configuredLevel,
                effectiveLevel = it?.effectiveLevel,
                application = AppLogger.entries.any { appLogger -> name == appLogger.value },
            )
        }
}

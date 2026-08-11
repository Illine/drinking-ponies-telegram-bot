package ru.illine.drinking.ponies.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.illine.drinking.ponies.config.web.security.AdminOnly
import ru.illine.drinking.ponies.model.dto.internal.TelegramAuthUserDto
import ru.illine.drinking.ponies.model.dto.internal.requireStoredId
import ru.illine.drinking.ponies.model.dto.request.LoggerLevelRequest
import ru.illine.drinking.ponies.model.dto.response.LoggerLevelResponse
import ru.illine.drinking.ponies.model.dto.response.LoggersResponse
import ru.illine.drinking.ponies.service.logging.LoggerAdminService
import ru.illine.drinking.ponies.util.logging.LoggingConstants
import ru.illine.drinking.ponies.util.telegram.TelegramGeneralConstants

@RestController
@RequestMapping("/systems/loggers")
@Validated
@AdminOnly
@Tag(name = "Logger administration", description = "Runtime log levels, applied without a restart")
class LoggerAdminController(
    private val loggerAdminService: LoggerAdminService,
) {
    @GetMapping
    @Operation(summary = "List our own loggers plus every logger that carries an explicit level")
    fun getLoggers(): LoggersResponse =
        LoggersResponse(
            loggerAdminService.getKnownLevels().map {
                LoggerLevelResponse(it.name, it.configuredLevel, it.effectiveLevel)
            },
        )

    @GetMapping("/{name}")
    @Operation(summary = "Get the level of any logger, including one of a third-party library")
    fun getLogger(
        @Parameter(description = "Logger name", example = "org.hibernate.SQL")
        @PathVariable(name = "name")
        @Pattern(regexp = LoggingConstants.LOGGER_NAME_PATTERN) name: String,
    ): LoggerLevelResponse =
        loggerAdminService.getLevel(name).let {
            LoggerLevelResponse(it.name, it.configuredLevel, it.effectiveLevel)
        }

    @PutMapping("/{name}")
    @Operation(summary = "Set the level of a logger until the application restarts")
    fun setLoggerLevel(
        @Parameter(description = "Logger name", example = "org.hibernate.SQL")
        @PathVariable(name = "name")
        @Pattern(regexp = LoggingConstants.LOGGER_NAME_PATTERN) name: String,
        @Parameter(hidden = true)
        @RequestAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE) actor: TelegramAuthUserDto,
        @Valid @RequestBody request: LoggerLevelRequest,
    ): LoggerLevelResponse =
        loggerAdminService.setLevel(name, request.level, actor.requireStoredId()).let {
            LoggerLevelResponse(it.name, it.configuredLevel, it.effectiveLevel)
        }

    @PostMapping("/reset")
    @Operation(summary = "Return every logger to the level it had at startup")
    fun resetLoggers(
        @Parameter(hidden = true)
        @RequestAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE) actor: TelegramAuthUserDto,
    ): LoggersResponse =
        LoggersResponse(
            loggerAdminService.resetLevels(actor.requireStoredId()).map {
                LoggerLevelResponse(it.name, it.configuredLevel, it.effectiveLevel)
            },
        )
}

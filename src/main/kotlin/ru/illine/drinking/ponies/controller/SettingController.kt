package ru.illine.drinking.ponies.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import ru.illine.drinking.ponies.model.base.IntervalNotificationType
import ru.illine.drinking.ponies.model.dto.TelegramUserDto
import ru.illine.drinking.ponies.model.dto.response.SettingResponse
import ru.illine.drinking.ponies.service.notification.NotificationSettingsService
import ru.illine.drinking.ponies.util.telegram.TelegramGeneralConstants
import java.time.LocalTime

@RestController
@RequestMapping("/settings")
@Tag(name = "Settings", description = "Notification settings management")
class SettingController(
    private val notificationSettingsService: NotificationSettingsService
) {

    @GetMapping
    @Operation(summary = "Get all notification settings")
    fun getSettings(
        @Parameter(hidden = true)
        @RequestAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE) telegramUser: TelegramUserDto,
    ): SettingResponse {
        val settings = notificationSettingsService.getAllSettings(telegramUser.telegramId)
        return SettingResponse(
            interval = settings.interval,
            intervalDisplayName = settings.intervalDisplayName,
            intervalMinutes = settings.intervalMinutes,
            quietModeStart = settings.quietModeStart,
            quietModeEnd = settings.quietModeEnd,
            timezone = settings.timezone,
            notificationActive = settings.notificationActive,
        )
    }

    @PutMapping("/quiet-mode")
    @Operation(summary = "Change quiet mode schedule")
    fun changeQuietMode(
        @Parameter(hidden = true)
        @RequestAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE) telegramUser: TelegramUserDto,
        @Parameter(description = "Start time in HH:mm format", example = "23:00", schema = Schema(type = "string", pattern = "HH:mm"))
        @RequestParam(name = "start", required = true) @DateTimeFormat(pattern = "HH:mm") start: LocalTime,
        @Parameter(description = "End time in HH:mm format", example = "08:00", schema = Schema(type = "string", pattern = "HH:mm"))
        @RequestParam(name = "end", required = true) @DateTimeFormat(pattern = "HH:mm") end: LocalTime
    ) {
        notificationSettingsService.changeQuietMode(telegramUser.telegramId, start, end)
    }

    @PutMapping("/timezone")
    @Operation(summary = "Change user timezone")
    fun changeTimezone(
        @Parameter(hidden = true)
        @RequestAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE) telegramUser: TelegramUserDto,
        @Parameter(description = "IANA timezone identifier", example = "Europe/Moscow")
        @RequestParam(name = "timezone", required = true) timezone: String
    ) {
        notificationSettingsService.changeTimezone(telegramUser.telegramId, timezone)
    }

    @PutMapping("/interval")
    @Operation(summary = "Change notification interval")
    fun changeInterval(
        @Parameter(hidden = true)
        @RequestAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE) telegramUser: TelegramUserDto,
        @Parameter(description = "Notification interval enum value", example = "HOUR")
        @RequestParam(name = "interval", required = true) interval: IntervalNotificationType
    ) {
        notificationSettingsService.changeInterval(telegramUser.telegramId, interval)
    }

    @PutMapping("/notification-status")
    @Operation(summary = "Change notification enabled status")
    fun changeNotificationStatus(
        @Parameter(hidden = true)
        @RequestAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE) telegramUser: TelegramUserDto,
        @Parameter(description = "Enable or disable notifications", example = "true")
        @RequestParam(name = "active", required = true) active: Boolean
    ) {
        notificationSettingsService.changeNotificationStatus(telegramUser.telegramId, active)
    }
}

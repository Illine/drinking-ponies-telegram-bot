package ru.illine.drinking.ponies.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import ru.illine.drinking.ponies.model.base.IntervalNotificationType
import ru.illine.drinking.ponies.model.dto.TelegramUserDto
import ru.illine.drinking.ponies.model.dto.response.IntervalResponse
import ru.illine.drinking.ponies.model.dto.response.NotificationStatusResponse
import ru.illine.drinking.ponies.model.dto.response.QuietModeResponse
import ru.illine.drinking.ponies.model.dto.response.TimezoneResponse
import ru.illine.drinking.ponies.service.notification.NotificationSettingsService
import ru.illine.drinking.ponies.util.TimeHelper
import ru.illine.drinking.ponies.util.telegram.TelegramGeneralConstants
import java.time.LocalTime

@RestController
@RequestMapping("/settings")
@Tag(name = "Settings", description = "Notification settings management")
class SettingController(
    private val notificationSettingsService: NotificationSettingsService
) {

    @GetMapping("/quiet-mode")
    @Operation(summary = "Get current quiet mode schedule")
    fun getQuietMode(
        @Parameter(hidden = true)
        @RequestAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE) telegramUser: TelegramUserDto,
    ): QuietModeResponse {
        val (start, end) = notificationSettingsService.getQuietMode(telegramUser.telegramId)
        return QuietModeResponse(
            start = TimeHelper.timeToString(start),
            end = TimeHelper.timeToString(end),
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

    @GetMapping("/timezone")
    @Operation(summary = "Get current user timezone")
    fun getTimezone(
        @Parameter(hidden = true)
        @RequestAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE) telegramUser: TelegramUserDto,
    ): TimezoneResponse {
        val settings = notificationSettingsService.getNotificationSettings(telegramUser.telegramId)
        return TimezoneResponse(timezone = settings.telegramUser.userTimeZone)
    }

    @GetMapping("/interval")
    @Operation(summary = "Get current notification interval")
    fun getInterval(
        @Parameter(hidden = true)
        @RequestAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE) telegramUser: TelegramUserDto,
    ): IntervalResponse {
        val settings = notificationSettingsService.getNotificationSettings(telegramUser.telegramId)
        return IntervalResponse(
            interval = settings.notificationInterval.name,
            displayName = settings.notificationInterval.displayName,
            minutes = settings.notificationInterval.minutes,
        )
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

    @GetMapping("/notification-status")
    @Operation(summary = "Get notification enabled status")
    fun getNotificationStatus(
        @Parameter(hidden = true)
        @RequestAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE) telegramUser: TelegramUserDto,
    ): NotificationStatusResponse {
        val enabled = notificationSettingsService.isEnabledNotifications(telegramUser.telegramId)
        return NotificationStatusResponse(active = enabled)
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

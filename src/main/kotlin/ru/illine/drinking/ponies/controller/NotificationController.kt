package ru.illine.drinking.ponies.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import ru.illine.drinking.ponies.model.dto.TelegramUserDto
import ru.illine.drinking.ponies.model.dto.response.NotificationNextResponse
import ru.illine.drinking.ponies.model.dto.response.PauseStateResponse
import ru.illine.drinking.ponies.service.notification.NotificationSettingsService
import ru.illine.drinking.ponies.util.telegram.TelegramGeneralConstants

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "Notification timing")
@Validated
class NotificationController(
    private val notificationSettingsService: NotificationSettingsService
) {

    @GetMapping("/next")
    @Operation(summary = "Get next notification time")
    fun getNextNotification(
        @Parameter(hidden = true)
        @RequestAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE) telegramUser: TelegramUserDto,
    ): NotificationNextResponse {
        val nextAt = notificationSettingsService.getNextNotificationAt(telegramUser.telegramId)
        return NotificationNextResponse(nextNotificationAt = nextAt)
    }

    @GetMapping("/pause")
    @Operation(summary = "Get notification pause state")
    fun getPauseState(
        @Parameter(hidden = true)
        @RequestAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE) telegramUser: TelegramUserDto,
    ): PauseStateResponse = notificationSettingsService.getPauseState(telegramUser.telegramId)

    @PutMapping("/pause")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Pause or cancel pause for notifications")
    fun changePause(
        @Parameter(hidden = true)
        @RequestAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE) telegramUser: TelegramUserDto,
        @Parameter(description = "Pause duration in minutes (0 = cancel pause, max 300 = 5 hours)", example = "60")
        @RequestParam(name = "minutes", required = true) @Min(0) @Max(300) minutes: Long
    ) {
        if (minutes == 0L) {
            notificationSettingsService.cancelPause(telegramUser.telegramId)
        } else {
            notificationSettingsService.pauseNotifications(telegramUser.telegramId, minutes)
        }
    }
}

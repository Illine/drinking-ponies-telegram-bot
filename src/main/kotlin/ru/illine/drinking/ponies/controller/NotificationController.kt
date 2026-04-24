package ru.illine.drinking.ponies.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.illine.drinking.ponies.model.dto.TelegramUserDto
import ru.illine.drinking.ponies.model.dto.response.NotificationNextResponse
import ru.illine.drinking.ponies.service.notification.NotificationSettingsService
import ru.illine.drinking.ponies.util.telegram.TelegramGeneralConstants

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "Notification timing")
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
        return NotificationNextResponse(nextNotificationAt = nextAt.toString())
    }
}
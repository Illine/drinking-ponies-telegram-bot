package ru.illine.drinking.ponies.controller

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import ru.illine.drinking.ponies.model.dto.TelegramUserDto
import ru.illine.drinking.ponies.service.notification.NotificationSettingsService
import ru.illine.drinking.ponies.util.telegram.TelegramGeneralConstants
import java.time.LocalTime

@RestController
@RequestMapping("/settings")
class SettingController(
    private val notificationSettingsService: NotificationSettingsService
) {

    @PutMapping("/modes/silent")
    fun changeSilentMode(
        @RequestAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE) telegramUser: TelegramUserDto,
        @RequestParam("messageId") messageId: Int,
        @RequestParam("start") @DateTimeFormat(pattern = "HH:mm") start: LocalTime,
        @RequestParam("end") @DateTimeFormat(pattern = "HH:mm") end: LocalTime
    ) {
        notificationSettingsService.changeQuietMode(telegramUser.telegramId, messageId, start, end)
    }
}

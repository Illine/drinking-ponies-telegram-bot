package ru.illine.drinking.ponies.controller

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import ru.illine.drinking.ponies.model.dto.TelegramUserDto
import ru.illine.drinking.ponies.service.app.NotificationSettingsService
import ru.illine.drinking.ponies.util.TelegramConstants
import java.time.LocalTime

@RestController
@RequestMapping("/settings")
class SettingController(
    private val notificationSettingsService: NotificationSettingsService
) {

    @PutMapping("/modes/silent")
    fun changeSilentMode(
        @RequestAttribute(TelegramConstants.TELEGRAM_USER_ATTRIBUTE) telegramUser: TelegramUserDto,
        @RequestParam("messageId") messageId: Int,
        @RequestParam("start") @DateTimeFormat(pattern = "HH:mm") start: LocalTime,
        @RequestParam("end") @DateTimeFormat(pattern = "HH:mm") end: LocalTime
    ) {
        notificationSettingsService.changeQuiteMode(telegramUser.telegramId, messageId, start, end)
    }
}

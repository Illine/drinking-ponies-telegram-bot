package ru.illine.drinking.ponies.builder

import ru.illine.drinking.ponies.model.dto.SettingDto
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.util.TimeHelper

object SettingBuilder {
    fun toDto(settings: NotificationSettingDto): SettingDto = SettingDto(
        interval = settings.notificationInterval.name,
        intervalDisplayName = settings.notificationInterval.displayName,
        intervalMinutes = settings.notificationInterval.minutes,
        quietModeStart = TimeHelper.timeToString(settings.quietModeStart!!),
        quietModeEnd = TimeHelper.timeToString(settings.quietModeEnd!!),
        timezone = settings.telegramUser.userTimeZone,
        dailyGoalMl = settings.dailyGoalMl,
        notificationActive = settings.enabled,
    )
}

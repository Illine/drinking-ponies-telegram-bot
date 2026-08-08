package ru.illine.drinking.ponies.mapper

import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.model.dto.internal.SettingDto
import ru.illine.drinking.ponies.util.TimeHelper

object SettingMapper {
    fun toDto(settings: NotificationSettingDto): SettingDto =
        SettingDto(
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

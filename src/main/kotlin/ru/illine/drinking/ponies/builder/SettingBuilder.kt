package ru.illine.drinking.ponies.builder

import ru.illine.drinking.ponies.model.dto.SettingDto
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.util.TimeHelper

// Intentionally hand-written, not migrated to Konvert (DPTB-136): this is a presentation
// transformation, not a structural mapping - the interval enum is expanded into three fields,
// LocalTime is formatted to String, and timezone is read from a nested object. Konvert would
// express all of this as @Mapping(expression=...), which it does not verify, giving no real
// safety over this named-constructor form. Correctness is guarded by
// NotificationSettingsServiceTest."getAllSettings returns full dto when enabled", which asserts every field.
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

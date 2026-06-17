package ru.illine.drinking.ponies.service.notification

import ru.illine.drinking.ponies.model.base.IntervalNotificationType
import ru.illine.drinking.ponies.model.dto.SettingDto
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.model.dto.response.PauseStateResponse
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime

interface NotificationSettingsService {

    fun getAllSettings(externalUserId: Long): SettingDto

    fun getNextNotificationAt(externalUserId: Long): Instant

    fun getNotificationSettings(externalUserId: Long): NotificationSettingDto

    fun getAllNotificationSettings(): Collection<NotificationSettingDto>

    fun getQuietMode(externalUserId: Long): Pair<LocalTime, LocalTime>

    fun resetNotificationTimer(externalUserId: Long, time: LocalDateTime): NotificationSettingDto

    fun changeInterval(externalUserId: Long, notificationInterval: IntervalNotificationType): NotificationSettingDto

    fun changeQuietMode(externalUserId: Long, start: LocalTime, end: LocalTime)

    fun disableQuietMode(externalUserId: Long)

    fun isEnabledNotifications(externalUserId: Long): Boolean

    fun changeNotificationStatus(externalUserId: Long, active: Boolean)

    fun changeTimezone(externalUserId: Long, timezone: String)

    fun pauseNotifications(externalUserId: Long, minutes: Long)

    fun cancelPause(externalUserId: Long)

    fun getPauseState(externalUserId: Long): PauseStateResponse

    fun changeDailyGoal(externalUserId: Long, goalMl: Int)

}

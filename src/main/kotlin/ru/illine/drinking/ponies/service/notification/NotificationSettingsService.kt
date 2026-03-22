package ru.illine.drinking.ponies.service.notification

import java.time.LocalTime

interface NotificationSettingsService {

    fun changeQuietMode(userId: Long, messageId: Int, start: LocalTime, end: LocalTime)

    fun disableQuietMode(userId: Long)

}

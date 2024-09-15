package ru.illine.drinking.ponies.service.app

import java.time.LocalTime

interface NotificationSettingsService {

    fun changeQuiteMode(userId: Long, messageId: Int, start: LocalTime, end: LocalTime)

    fun disableQuiteMode(userId: Long)

}
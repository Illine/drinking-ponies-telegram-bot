package ru.illine.drinking.ponies.service

import ru.illine.drinking.ponies.model.dto.NotificationDto

interface NotificationTimeService {

    fun isOutsideQuietTime(dto: NotificationDto): Boolean

    fun isNotificationDue(dto: NotificationDto): Boolean

}
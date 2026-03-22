package ru.illine.drinking.ponies.service.notification

import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto

interface NotificationTimeService {

    fun isOutsideQuietTime(dto: NotificationSettingDto): Boolean

    fun isNotificationDue(dto: NotificationSettingDto): Boolean

}

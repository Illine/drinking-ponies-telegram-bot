package ru.illine.drinking.ponies.service.notification

import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto

interface NotificationSenderService {

    fun sendNotifications(notifications: Collection<NotificationSettingDto>)

    fun suspendNotifications(notifications: Collection<NotificationSettingDto>)

}

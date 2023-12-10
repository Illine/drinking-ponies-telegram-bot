package ru.illine.drinking.ponies.model.dto

import ru.illine.drinking.ponies.model.base.DelayNotificationType
import ru.illine.drinking.ponies.model.entity.NotificationEntity
import java.time.OffsetDateTime
import java.time.ZoneId

data class NotificationDto(
    var id: Long? = null,

    var userId: Long,

    val chatId: Long,

    val delayNotification: DelayNotificationType,

    var timeOfLastNotification: OffsetDateTime = OffsetDateTime.now(),

    var notificationAttempts: Int = 0,

    var userTimeZone: String = ZoneId.systemDefault().toString(),

    var previousNotificationMessageId: Int? = null,

    var created: OffsetDateTime? = null,

    var updated: OffsetDateTime? = null,

    var deleted: Boolean = false
) {

    fun toEntity(): NotificationEntity {
        return NotificationEntity(
            id = id,
            userId = userId,
            chatId = chatId,
            delayNotification = delayNotification,
            timeOfLastNotification = timeOfLastNotification,
            notificationAttempts = notificationAttempts,
            userTimeZone = userTimeZone,
            previousNotificationMessageId = previousNotificationMessageId,
            deleted = deleted
        )
    }

    companion object {
        fun create(
            userId: Long,
            chatId: Long,
            delayNotification: DelayNotificationType = DelayNotificationType.TWO_HOURS
        ): NotificationDto {
            return NotificationDto(
                userId = userId,
                chatId = chatId,
                delayNotification = delayNotification
            )
        }
    }
}

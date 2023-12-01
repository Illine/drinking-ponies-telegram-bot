package ru.illine.drinking.ponies.model.dto

import ru.illine.drinking.ponies.model.base.DelayNotificationType
import ru.illine.drinking.ponies.model.entity.NotificationEntity
import java.time.OffsetDateTime

data class NotificationDto(
    var id: Long? = null,

    var userId: Long,

    val chatId: Long,

    val delayNotification: DelayNotificationType,

    var timeOfLastNotification: OffsetDateTime,

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
            deleted = deleted
        )
    }

    companion object {
        fun create(
            userId: Long,
            chatId: Long,
            delayNotification: DelayNotificationType
        ): NotificationDto {
            return NotificationDto(
                userId = userId,
                chatId = chatId,
                delayNotification = delayNotification,
                timeOfLastNotification = OffsetDateTime.now()
            )
        }
    }
}

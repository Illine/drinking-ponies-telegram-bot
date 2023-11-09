package ru.illine.drinking.ponies.model.dto

import org.telegram.telegrambots.meta.api.objects.Update
import ru.illine.drinking.ponies.model.base.DelayNotificationType
import ru.illine.drinking.ponies.model.entity.UserNotificationEntity
import java.time.OffsetDateTime

data class UserNotificationDto(
    var id: Long? = null,

    var userId: Long,

    var username: String,

    var firstName: String? = null,

    var lastName: String? = null,

    var languageCode: String? = null,

    var premium: Boolean = false,

    val chatId: Long,

    val delayNotification: DelayNotificationType,

    var timeOfLastNotification: OffsetDateTime,

    var created: OffsetDateTime? = null,

    var updated: OffsetDateTime? = null,

    var deleted: Boolean = false
) {
    fun toEntity(): UserNotificationEntity {
        return UserNotificationEntity(
            id = id,
            userId = userId,
            username = username,
            firstName = firstName,
            lastName = lastName,
            languageCode = languageCode,
            premium = premium,
            chatId = chatId,
            delayNotification = delayNotification,
            timeOfLastNotification = timeOfLastNotification,
            deleted = deleted
        )
    }
    companion object {
        fun create(update: Update, delayNotification: DelayNotificationType): UserNotificationDto {
            val message = update.message
            val from = message.from
            return UserNotificationDto(
                userId = from.id,
                username = from.userName,
                firstName = from.firstName,
                lastName = from.lastName,
                languageCode = from.languageCode,
                premium = from.isPremium,
                chatId = message.chatId,
                delayNotification = delayNotification,
                timeOfLastNotification = OffsetDateTime.now()
            )
        }
    }
}

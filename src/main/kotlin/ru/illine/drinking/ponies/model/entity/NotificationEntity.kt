package ru.illine.drinking.ponies.model.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import org.hibernate.annotations.SQLDelete
import ru.illine.drinking.ponies.model.base.DelayNotificationType
import ru.illine.drinking.ponies.model.dto.NotificationDto
import java.time.LocalDateTime

@Entity
@Table(
    name = "notifications",
    schema = "drinking_ponies",
    indexes = [Index(name = "notifications_user_id_unique_index", columnList = "user_id", unique = true)]
)
@SQLDelete(sql = "update notifications set deleted = true where id = ?")
class NotificationEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notificationSeqGenerator")
    @SequenceGenerator(
        name = "notificationSeqGenerator",
        sequenceName = "notification_seq",
        allocationSize = 1
    )
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(name = "chat_id", nullable = false, updatable = false)
    val chatId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "delay_notification", nullable = false)
    var delayNotification: DelayNotificationType,

    @Column(name = "time_of_last_notification", nullable = false)
    var timeOfLastNotification: LocalDateTime,

    @Column(name = "user_time_zone", nullable = false)
    var userTimeZone: String,

    @Column(name = "notification_attempts", nullable = false)
    var notificationAttempts: Int = 0,

    @Column(name = "previous_notification_message_id")
    var previousNotificationMessageId: Int? = null,

    @Column(name = "deleted", nullable = false)
    var deleted: Boolean = false

) {

    @Column(name = "created", nullable = false, updatable = false)
    @JsonIgnore
    var created: LocalDateTime? = null

    @Column(name = "updated", nullable = false)
    @JsonIgnore
    var updated: LocalDateTime? = null

    @PrePersist
    private fun onCreate() {
        val now = LocalDateTime.now()
        created = now
        updated = now
    }

    @PreUpdate
    private fun onUpdate() {
        updated = LocalDateTime.now()
    }

    @PreRemove
    private fun onDelete() {
        updated = LocalDateTime.now()
        deleted = false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NotificationEntity

        if (id != other.id) return false
        if (userId != other.userId) return false
        if (chatId != other.chatId) return false
        if (deleted != other.deleted) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + userId.hashCode()
        result = 31 * result + chatId.hashCode()
        result = 31 * result + deleted.hashCode()
        return result
    }

    fun toDto(): NotificationDto {
        return NotificationDto(
            id = id,
            userId = userId,
            chatId = chatId,
            delayNotification = delayNotification,
            timeOfLastNotification = timeOfLastNotification,
            notificationAttempts = notificationAttempts,
            userTimeZone = userTimeZone,
            previousNotificationMessageId = previousNotificationMessageId,
            created = created,
            updated = updated,
            deleted = deleted
        )
    }
}
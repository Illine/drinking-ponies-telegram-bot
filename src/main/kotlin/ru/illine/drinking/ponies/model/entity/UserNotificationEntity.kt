package ru.illine.drinking.ponies.model.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import org.hibernate.annotations.Where
import ru.illine.drinking.ponies.model.base.DelayNotificationType
import ru.illine.drinking.ponies.model.dto.UserNotificationDto
import java.time.OffsetDateTime

@Entity
@Table(
    name = "user_notifications",
    indexes = [Index(name = "user_notifications_user_id_unique_index", columnList = "user_id", unique = true)]
)
@Where(clause = "deleted = false")
class UserNotificationEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "userNotificationSeqGenerator")
    @SequenceGenerator(
        name = "userNotificationSeqGenerator",
        sequenceName = "user_notification_seq",
        allocationSize = 1
    )
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(name = "username", nullable = false)
    var username: String,

    @Column(name = "first_name")
    var firstName: String? = null,

    @Column(name = "last_name")
    var lastName: String? = null,

    @Column(name = "language_code", nullable = false)
    var languageCode: String? = null,

    @Column(name = "premium", nullable = false)
    var premium: Boolean = false,

    @Column(name = "chat_id", nullable = false, updatable = false)
    val chatId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "delay_notification", nullable = false)
    var delayNotification: DelayNotificationType,

    @Column(name = "time_of_last_notification", nullable = false)
    var timeOfLastNotification: OffsetDateTime,

    @Column(name = "deleted", nullable = false)
    var deleted: Boolean = false

) {

    @Column(name = "created", nullable = false, updatable = false)
    @JsonIgnore
    lateinit var created: OffsetDateTime

    @Column(name = "updated", nullable = false)
    @JsonIgnore
    lateinit var updated: OffsetDateTime

    @PrePersist
    private fun onCreate() {
        val now = OffsetDateTime.now()
        created = now
        updated = now
    }

    @PreUpdate
    private fun onUpdate() {
        updated = OffsetDateTime.now()
    }

    @PreRemove
    private fun onDelete() {
        updated = OffsetDateTime.now()
        deleted = false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserNotificationEntity

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

    fun toDto(): UserNotificationDto {
        return UserNotificationDto(
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
            created = created,
            updated = updated,
            deleted = deleted
        )
    }
}
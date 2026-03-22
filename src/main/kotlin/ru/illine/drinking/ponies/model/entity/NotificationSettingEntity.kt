package ru.illine.drinking.ponies.model.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLRestriction
import ru.illine.drinking.ponies.model.base.IntervalNotificationType
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "notification_settings")
@SQLDelete(sql = "update notification_settings set enabled = false where id = ?")
@SQLRestriction(value = "enabled = true")
class NotificationSettingEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notificationSettingSeqGenerator")
    @SequenceGenerator(
        name = "notificationSettingSeqGenerator",
        sequenceName = "notification_setting_seq",
        allocationSize = 1
    )
    var id: Long? = null,

    @ManyToOne(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.MERGE, CascadeType.REFRESH],
    )
    @JoinColumn(name = "telegram_user_id", nullable = false)
    var telegramUser: TelegramUserEntity,

    @ManyToOne(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.MERGE, CascadeType.REFRESH],
    )
    @JoinColumn(name = "telegram_chat_id", nullable = false)
    var telegramChat: TelegramChatEntity,

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_interval", nullable = false)
    var notificationInterval: IntervalNotificationType,

    @Column(name = "time_of_last_notification", nullable = false)
    var timeOfLastNotification: LocalDateTime,

    @Column(name = "notification_attempts", nullable = false)
    var notificationAttempts: Int = 0,

    @Column(name = "quiet_mode_start", nullable = false)
    @JsonIgnore
    var quietModeStart: LocalTime? = null,

    @Column(name = "quiet_mode_end", nullable = false)
    @JsonIgnore
    var quietModeEnd: LocalTime? = null,

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true
) {

    @PreRemove
    private fun onDelete() {
        enabled = false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NotificationSettingEntity

        if (id != other.id) return false
        if (notificationAttempts != other.notificationAttempts) return false
        if (enabled != other.enabled) return false
        if (notificationInterval != other.notificationInterval) return false
        if (timeOfLastNotification != other.timeOfLastNotification) return false
        if (quietModeStart != other.quietModeStart) return false
        if (quietModeEnd != other.quietModeEnd) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + notificationAttempts
        result = 31 * result + enabled.hashCode()
        result = 31 * result + notificationInterval.hashCode()
        result = 31 * result + timeOfLastNotification.hashCode()
        result = 31 * result + (quietModeStart?.hashCode() ?: 0)
        result = 31 * result + (quietModeEnd?.hashCode() ?: 0)
        return result
    }
}
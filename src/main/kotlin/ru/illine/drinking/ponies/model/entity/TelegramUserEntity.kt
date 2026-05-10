package ru.illine.drinking.ponies.model.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLRestriction
import java.time.LocalDateTime

@Entity
@Table(
    name = "telegram_users",
    indexes = [Index(name = "telegram_users_external_user_id_unique_index", columnList = "external_user_id", unique = true)]
)
@SQLDelete(sql = "update telegram_users set deleted = true where id = ?")
@SQLRestriction(value = "deleted = false")
class TelegramUserEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "telegramUserSeqGenerator")
    @SequenceGenerator(
        name = "telegramUserSeqGenerator",
        sequenceName = "telegram_user_seq",
        allocationSize = 1
    )
    var id: Long? = null,

    @Column(name = "external_user_id", nullable = false)
    var externalUserId: Long,

    @Column(name = "user_time_zone", nullable = false)
    var userTimeZone: String,

    @Column(name = "is_admin", nullable = false)
    var isAdmin: Boolean = false,

    @Column(name = "created", nullable = false, updatable = false)
    @JsonIgnore
    var created: LocalDateTime = LocalDateTime.now(),

    @Column(name = "deleted", nullable = false)
    var deleted: Boolean = false,

    @OneToMany(
        fetch = FetchType.LAZY,
        mappedBy = "telegramUser",
        orphanRemoval = true,
        cascade = [CascadeType.ALL],
    )
    val telegramChats: MutableSet<TelegramChatEntity> = mutableSetOf(),

    @OneToOne(
        fetch = FetchType.LAZY,
        mappedBy = "telegramUser",
        orphanRemoval = true,
        cascade = [CascadeType.ALL],
    )
    var notificationSettings: NotificationSettingEntity? = null
) {
    @PreRemove
    private fun onDelete() {
        deleted = true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TelegramUserEntity

        if (id != other.id) return false
        if (externalUserId != other.externalUserId) return false
        if (userTimeZone != other.userTimeZone) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + created.hashCode()
        result = 31 * result + deleted.hashCode()
        result = 31 * result + userTimeZone.hashCode()
        result = 31 * result + externalUserId.hashCode()

        return result
    }

    fun addTelegramChat(chat: TelegramChatEntity) {
        telegramChats.add(chat)
        chat.telegramUser = this
    }
}

package ru.illine.drinking.ponies.model.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "telegram_chats",
    indexes = [Index(
        name = "telegram_chats_external_chat_id_unique_index",
        columnList = "external_chat_id",
        unique = true
    )]
)
class TelegramChatEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "telegramChatSeqGenerator")
    @SequenceGenerator(
        name = "telegramChatSeqGenerator",
        sequenceName = "telegram_chat_seq",
        allocationSize = 1
    )
    var id: Long? = null,

    @Column(name = "external_chat_id", nullable = false)
    var externalChatId: Long,

    @Column(name = "previous_notification_external_message_id")
    var previousNotificationMessageId: Int? = null,

    @ManyToOne(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.MERGE, CascadeType.REFRESH],
    )
    @JoinColumn(name = "telegram_user_id", nullable = false)
    var telegramUser: TelegramUserEntity,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TelegramChatEntity

        if (id != other.id) return false
        if (externalChatId != other.externalChatId) return false
        if (previousNotificationMessageId != other.previousNotificationMessageId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + externalChatId.hashCode()
        result = 31 * result + previousNotificationMessageId.hashCode()
        return result
    }
}
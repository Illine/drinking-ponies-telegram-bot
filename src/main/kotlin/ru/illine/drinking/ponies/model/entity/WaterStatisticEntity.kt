package ru.illine.drinking.ponies.model.entity

import jakarta.persistence.*
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import java.time.LocalDateTime

@Entity
@Table(name = "water_statistics")
class WaterStatisticEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "waterStatisticsSeqGenerator")
    @SequenceGenerator(
        name = "waterStatisticsSeqGenerator",
        sequenceName = "water_statistics_seq",
        allocationSize = 1
    )
    var id: Long? = null,

    @ManyToOne(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.MERGE, CascadeType.REFRESH],
    )
    @JoinColumn(name = "user_id", nullable = false)
    var telegramUser: TelegramUserEntity,

    @Column(name = "event_time", nullable = false)
    var eventTime: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    var eventType: AnswerNotificationType,

    @Column(name = "water_amount_ml", nullable = false)
    var waterAmountMl: Int = 0

) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WaterStatisticEntity

        if (id != other.id) return false
        if (eventTime != other.eventTime) return false
        if (eventType != other.eventType) return false
        if (waterAmountMl != other.waterAmountMl) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + eventTime.hashCode()
        result = 31 * result + eventType.hashCode()
        result = 31 * result + waterAmountMl
        return result
    }

}

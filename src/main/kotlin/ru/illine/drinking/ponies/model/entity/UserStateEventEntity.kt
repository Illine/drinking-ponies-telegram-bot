package ru.illine.drinking.ponies.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import ru.illine.drinking.ponies.model.base.UserStateEventType
import java.time.LocalDateTime

// Users are referenced by raw ids: @SQLRestriction hides a deleted user, and every event is about state changes
// that a deleted or banned user goes through.
@Entity
@Table(name = "user_state_events")
class UserStateEventEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "userStateEventSeqGenerator")
    @SequenceGenerator(
        name = "userStateEventSeqGenerator",
        sequenceName = "user_state_event_seq",
        allocationSize = 1,
    )
    var id: Long? = null,
    @Column(name = "user_id", nullable = false)
    var userId: Long,
    @Column(name = "actor_user_id", nullable = false)
    var actorUserId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    var eventType: UserStateEventType,
    @Column(name = "event_time", nullable = false)
    var eventTime: LocalDateTime,
)

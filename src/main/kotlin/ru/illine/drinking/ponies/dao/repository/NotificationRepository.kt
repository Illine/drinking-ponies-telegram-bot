package ru.illine.drinking.ponies.dao.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.illine.drinking.ponies.model.entity.NotificationEntity

interface NotificationRepository : JpaRepository<NotificationEntity, Long> {

    fun findByUserId(userId: Long): NotificationEntity?

    fun existsByUserId(userId: Long): Boolean

    @Modifying
    @Query(
        value = "update drinking_ponies.notifications set deleted = :deleted where user_id = :userId",
        nativeQuery = true
    )
    fun switchDeleted(@Param("userId") userId: Long, @Param("deleted") deleted: Boolean)
}
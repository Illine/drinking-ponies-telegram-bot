package ru.illine.drinking.ponies.dao.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.illine.drinking.ponies.model.entity.NotificationEntity

interface NotificationRepository : JpaRepository<NotificationEntity, Long> {

    @Query(
        value = "select n.* from notifications n where n.user_id = :userId",
        nativeQuery = true
    )
    fun findAnyByUserId(@Param("userId") userId: Long): NotificationEntity?

    fun findByUserId(userId: Long): NotificationEntity?

    fun existsByUserId(userId: Long): Boolean

    fun deleteByUserId(userId: Long): Long

    @Modifying
    @Query(
        value = "update notifications set deleted = false where user_id = :userId",
        nativeQuery = true
    )
    fun enableByUserId(@Param("userId") userId: Long)
}
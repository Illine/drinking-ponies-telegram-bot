package ru.illine.drinking.ponies.dao.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.illine.drinking.ponies.model.entity.NotificationEntity
import java.time.LocalTime

interface NotificationRepository : JpaRepository<NotificationEntity, Long> {

    @Query(
        value = "select n.* from drinking_ponies.notifications n where n.user_id = :userId",
        nativeQuery = true
    )
    fun findByUserId(@Param("userId") userId: Long): NotificationEntity?

    @Query(
        value = "select count(n.id) > 0 from drinking_ponies.notifications n where n.user_id = :userId",
        nativeQuery = true
    )
    fun existsByUserId(@Param("userId") userId: Long): Boolean

    @Query(
        value = "select n.deleted from drinking_ponies.notifications n where n.user_id = :userId",
        nativeQuery = true
    )
    fun isDeletedByUserId(@Param("userId") userId: Long): Boolean

    @Modifying
    @Query(
        value = "update drinking_ponies.notifications set deleted = :deleted where user_id = :userId",
        nativeQuery = true
    )
    fun switchDeleted(@Param("userId") userId: Long, @Param("deleted") deleted: Boolean)

    @Modifying
    @Query(
        value = "update drinking_ponies.notifications " +
                "set quiet_mode_start = :start, quiet_mode_end = :end " +
                "where user_id = :userId",
        nativeQuery = true
    )
    fun updateQuietMode(
        @Param("userId") userId: Long,
        @Param("start") start: LocalTime? = null,
        @Param("end") end: LocalTime? = null
    )
}
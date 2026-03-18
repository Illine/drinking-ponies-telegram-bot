package ru.illine.drinking.ponies.dao.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.illine.drinking.ponies.model.entity.NotificationSettingEntity
import java.time.LocalTime

interface NotificationSettingRepository : JpaRepository<NotificationSettingEntity, Long> {

    fun findByTelegramUser_ExternalUserId(externalUserId: Long): NotificationSettingEntity?

    @Query(
        value = """
            select ns.enabled 
            from notification_settings ns
            inner join telegram_users u on ns.telegram_user_id = u.id
            where u.external_user_id = :externalUserId
        """,
        nativeQuery = true
    )
    fun isEnabledByTelegramUserId(@Param("externalUserId") externalUserId: Long): Boolean

    @Modifying
    @Query(
        value = """
        update notification_settings ns
        set enabled = :enabled
        from telegram_users u
        where ns.telegram_user_id = u.id
          and u.external_user_id = :externalUserId
        """,
        nativeQuery = true
    )
    fun switchEnabled(
        @Param("externalUserId") externalUserId: Long,
        @Param("enabled") enabled: Boolean
    )

    @Modifying
    @Query(
        value = """
        update notification_settings ns
        set quiet_mode_start = :start, quiet_mode_end   = :end
        from telegram_users u
        where ns.telegram_user_id = u.id
          and u.external_user_id = :externalUserId
        """,
        nativeQuery = true
    )
    fun updateQuietMode(
        @Param("externalUserId") externalUserId: Long,
        @Param("start") start: LocalTime? = null,
        @Param("end") end: LocalTime? = null
    )

}
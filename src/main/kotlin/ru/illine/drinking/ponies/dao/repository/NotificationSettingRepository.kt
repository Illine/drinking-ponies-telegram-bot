package ru.illine.drinking.ponies.dao.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.illine.drinking.ponies.model.entity.NotificationSettingEntity
import java.time.LocalDateTime
import java.time.LocalTime

interface NotificationSettingRepository : JpaRepository<NotificationSettingEntity, Long> {
    // Spring Data resolves the nested property by the underscore, so the rule cannot apply here.
    @Suppress("ktlint:standard:function-naming")
    fun findByTelegramUser_ExternalUserId(externalUserId: Long): NotificationSettingEntity?

    @Query(
        value = """
            select ns from NotificationSettingEntity ns
            join fetch ns.telegramUser u
            join fetch ns.telegramChat
            where u.isBanned = false
        """,
    )
    fun findAllNotBannedWithUserAndChat(): List<NotificationSettingEntity>

    @Query(
        value = """
            select ns from NotificationSettingEntity ns
            join fetch ns.telegramUser u
            join fetch ns.telegramChat
            where u.externalUserId in :externalUserIds
        """,
    )
    fun findAllWithUserAndChatByExternalUserIdIn(
        @Param("externalUserIds") externalUserIds: Collection<Long>,
    ): List<NotificationSettingEntity>

    @Query(
        value = """
            select ns.enabled
            from notification_settings ns
            inner join telegram_users u on ns.telegram_user_id = u.id
            where u.external_user_id = :externalUserId
        """,
        nativeQuery = true,
    )
    fun isEnabledByExternalUserId(
        @Param("externalUserId") externalUserId: Long,
    ): Boolean

    @Modifying
    @Query(
        value = """
        update notification_settings ns
        set enabled = :enabled
        from telegram_users u
        where ns.telegram_user_id = u.id
          and u.external_user_id = :externalUserId
        """,
        nativeQuery = true,
    )
    fun switchEnabled(
        @Param("externalUserId") externalUserId: Long,
        @Param("enabled") enabled: Boolean,
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
        nativeQuery = true,
    )
    fun updateQuietMode(
        @Param("externalUserId") externalUserId: Long,
        @Param("start") start: LocalTime? = null,
        @Param("end") end: LocalTime? = null,
    )

    // A pause is a forward shift of time_of_last_notification, so dropping the mark alone would leave
    // the user silent until the shift runs out.
    @Modifying
    @Query(
        value = """
        update notification_settings ns
        set pause_until = null,
            time_of_last_notification = least(ns.time_of_last_notification, :now)
        from telegram_users u
        where ns.telegram_user_id = u.id
          and u.external_user_id = :externalUserId
        """,
        nativeQuery = true,
    )
    fun clearPause(
        @Param("externalUserId") externalUserId: Long,
        @Param("now") now: LocalDateTime,
    )

    @Modifying
    @Query(
        value = """
        update notification_settings ns
        set daily_goal_ml = :goalMl
        from telegram_users u
        where ns.telegram_user_id = u.id
          and u.external_user_id = :externalUserId
        """,
        nativeQuery = true,
    )
    fun updateDailyGoal(
        @Param("externalUserId") externalUserId: Long,
        @Param("goalMl") goalMl: Int,
    )
}

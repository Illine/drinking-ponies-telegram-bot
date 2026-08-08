package ru.illine.drinking.ponies.dao.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.illine.drinking.ponies.model.entity.TelegramUserEntity

// The admin queries below are native on purpose: @SQLRestriction(deleted = false) hides soft-deleted
// users from every JPQL query, and the admin list is the one place that has to see them.
private const val ADMIN_USER_SELECT = """
    select u.id               as "id",
           u.external_user_id as "externalUserId",
           u.first_name       as "firstName",
           u.last_name        as "lastName",
           u.username         as "username",
           u.is_admin         as "admin",
           u.is_banned        as "banned",
           u.deleted          as "deleted",
           u.user_time_zone   as "timeZone",
           u.created          as "created",
           (
               select max(ws.event_time)
               from water_statistics ws
               where ws.user_id = u.id
                 and ws.event_type in ('YES', 'SNOOZE')
           )                  as "lastActivity"
    from telegram_users u
"""

// The caller passes the pattern already wrapped in wildcards and escaped, or null for "no search".
// Casts keep Postgres from complaining that it cannot infer the type of a null parameter.
private const val SEARCH_FILTER = """
    (
        cast(:search as text) is null
        or concat_ws(' ', u.first_name, u.last_name, u.username, u.external_user_id, u.id)
               ilike cast(:search as text) escape '\'
    )
"""

// Nulls come from AdminUserStatusFilter and mean "this column is not constrained".
private const val STATUS_FILTER = """
    (
        (cast(:deleted as boolean) is null or u.deleted = cast(:deleted as boolean))
        and (cast(:banned as boolean) is null or u.is_banned = cast(:banned as boolean))
    )
"""

interface TelegramUserRepository : JpaRepository<TelegramUserEntity, Long> {
    fun findByExternalUserId(externalUserId: Long): TelegramUserEntity?

    fun findAllByExternalUserIdIn(externalUserId: Collection<Long>): Set<TelegramUserEntity>

    fun existsByExternalUserId(externalUserId: Long): Boolean

    // Native, so that @SQLRestriction does not hide a soft-deleted user from the auth path:
    // the entity still comes back managed, so profile refresh keeps working through dirty checking.
    @Query(value = "select * from telegram_users u where u.external_user_id = :externalUserId", nativeQuery = true)
    fun findByExternalUserIdIncludingDeleted(
        @Param("externalUserId") externalUserId: Long,
    ): TelegramUserEntity?

    // No countQuery: the total for any status is already one of the columns of countForAdmin.
    @Query(
        value = """
            $ADMIN_USER_SELECT
            where $SEARCH_FILTER
              and $STATUS_FILTER
            order by "lastActivity" desc nulls last, u.id
        """,
        nativeQuery = true,
    )
    fun findAllForAdmin(
        @Param("search") search: String?,
        @Param("deleted") deleted: Boolean?,
        @Param("banned") banned: Boolean?,
        pageable: Pageable,
    ): List<AdminUserProjection>

    @Query(value = """$ADMIN_USER_SELECT where u.id = :id""", nativeQuery = true)
    fun findByIdForAdmin(
        @Param("id") id: Long,
    ): AdminUserProjection?

    @Query(
        value = """
            select count(*)                                                          as "all",
                   count(*) filter (where u.deleted = false and u.is_banned = false) as "active",
                   count(*) filter (where u.deleted = true and u.is_banned = false)  as "inactive",
                   count(*) filter (where u.is_banned = true)                        as "banned"
            from telegram_users u
            where $SEARCH_FILTER
        """,
        nativeQuery = true,
    )
    fun countForAdmin(
        @Param("search") search: String?,
    ): UserCountsProjection

    @Modifying(clearAutomatically = true)
    @Query(value = "update telegram_users set deleted = :deleted where id = :id", nativeQuery = true)
    fun updateDeleted(
        @Param("id") id: Long,
        @Param("deleted") deleted: Boolean,
    )

    // Native for the same reason: a soft-deleted user is invisible to derived queries, so /start
    // would take them for a newcomer and hit the unique index on external_user_id.
    // Returns 0 when there was nothing to restore.
    @Modifying(clearAutomatically = true)
    @Query(
        value = """
            update telegram_users
            set deleted = false
            where external_user_id = :externalUserId
              and deleted = true
        """,
        nativeQuery = true,
    )
    fun restoreByExternalUserId(
        @Param("externalUserId") externalUserId: Long,
    ): Int
}

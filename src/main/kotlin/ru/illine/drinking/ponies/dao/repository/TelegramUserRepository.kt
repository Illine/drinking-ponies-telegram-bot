package ru.illine.drinking.ponies.dao.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.illine.drinking.ponies.dao.repository.projection.AdminUserProjection
import ru.illine.drinking.ponies.dao.repository.projection.UserCountsProjection
import ru.illine.drinking.ponies.dao.repository.query.TelegramUserQuery.ADMIN_USER_SELECT
import ru.illine.drinking.ponies.dao.repository.query.TelegramUserQuery.SEARCH_FILTER
import ru.illine.drinking.ponies.model.entity.TelegramUserEntity

interface TelegramUserRepository : JpaRepository<TelegramUserEntity, Long> {
    fun findByExternalUserId(externalUserId: Long): TelegramUserEntity?

    fun findAllByExternalUserIdIn(externalUserId: Collection<Long>): Set<TelegramUserEntity>

    fun existsByExternalUserId(externalUserId: Long): Boolean

    @Query(value = "select * from telegram_users u where u.external_user_id = :externalUserId", nativeQuery = true)
    fun findByExternalUserIdIncludingDeleted(
        @Param("externalUserId") externalUserId: Long,
    ): TelegramUserEntity?

    @Query(value = "select * from telegram_users u where u.id = :id", nativeQuery = true)
    fun findByIdIncludingDeleted(
        @Param("id") id: Long,
    ): TelegramUserEntity?

    @Query(
        value = """
            $ADMIN_USER_SELECT
            where $SEARCH_FILTER
              and (
                  (cast(:deleted as boolean) is null or u.deleted = cast(:deleted as boolean))
                  and (cast(:banned as boolean) is null or u.is_banned = cast(:banned as boolean))
              )
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
}

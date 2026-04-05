package ru.illine.drinking.ponies.dao.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.illine.drinking.ponies.model.entity.TelegramUserEntity

interface TelegramUserRepository : JpaRepository<TelegramUserEntity, Long> {

    fun findByExternalUserId(telegramUserId: Long): TelegramUserEntity?

    fun findAllByExternalUserIdIn(telegramUserId: Collection<Long>): Set<TelegramUserEntity>

    fun existsByExternalUserId(telegramUserId: Long): Boolean

    @Query(
        value = "select u.deleted from telegram_users u where u.external_user_id = :externalUserId",
        nativeQuery = true
    )
    fun isDeletedByExternalUserId(@Param("externalUserId") externalUserId: Long): Boolean
}
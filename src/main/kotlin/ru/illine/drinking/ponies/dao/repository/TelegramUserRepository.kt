package ru.illine.drinking.ponies.dao.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.illine.drinking.ponies.model.entity.TelegramUserEntity

interface TelegramUserRepository : JpaRepository<TelegramUserEntity, Long> {
    fun findByExternalUserId(externalUserId: Long): TelegramUserEntity?

    fun findAllByExternalUserIdIn(externalUserId: Collection<Long>): Set<TelegramUserEntity>

    fun existsByExternalUserId(externalUserId: Long): Boolean
}

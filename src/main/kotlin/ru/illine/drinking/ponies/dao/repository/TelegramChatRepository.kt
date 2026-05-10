package ru.illine.drinking.ponies.dao.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.illine.drinking.ponies.model.entity.TelegramChatEntity

interface TelegramChatRepository : JpaRepository<TelegramChatEntity, Long> {

    fun findByExternalChatId(externalChatId: Long): TelegramChatEntity?

}

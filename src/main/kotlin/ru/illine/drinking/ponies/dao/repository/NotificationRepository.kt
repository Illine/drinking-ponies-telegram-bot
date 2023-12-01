package ru.illine.drinking.ponies.dao.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.illine.drinking.ponies.model.entity.NotificationEntity

interface NotificationRepository : JpaRepository<NotificationEntity, Long> {

    fun findByUserId(userId: Long): NotificationEntity?

    fun existsByUserId(userId: Long): Boolean
}
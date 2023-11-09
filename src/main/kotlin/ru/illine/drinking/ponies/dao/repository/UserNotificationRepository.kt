package ru.illine.drinking.ponies.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository
import ru.illine.drinking.ponies.model.entity.UserNotificationEntity

interface UserNotificationRepository : JpaRepository<UserNotificationEntity, Long> {

    fun findByUserId(userId: Long): UserNotificationEntity?
}
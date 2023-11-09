package ru.illine.drinking.ponies.dao.access

import ru.illine.drinking.ponies.model.dto.UserNotificationDto
import java.time.OffsetDateTime

interface UserNotificationAccessService {

    fun findAll(): Set<UserNotificationDto>

    fun findByUserId(userId: Long): UserNotificationDto

    fun save(dto: UserNotificationDto): UserNotificationDto

    fun updateTimeOfLastNotification(userId: Long, time: OffsetDateTime): UserNotificationDto

}
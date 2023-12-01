package ru.illine.drinking.ponies.dao.access

import ru.illine.drinking.ponies.model.dto.NotificationDto
import java.time.OffsetDateTime

interface NotificationAccessService {

    fun findAll(): Set<NotificationDto>

    fun findByUserId(userId: Long): NotificationDto

    fun save(dto: NotificationDto): NotificationDto

    fun updateTimeOfLastNotification(userId: Long, time: OffsetDateTime): NotificationDto

}
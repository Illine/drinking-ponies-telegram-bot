package ru.illine.drinking.ponies.dao.access

import ru.illine.drinking.ponies.model.dto.NotificationDto
import java.time.LocalDateTime
import java.time.LocalTime

interface NotificationAccessService {

    fun findAll(): Set<NotificationDto>

    fun findByUserId(userId: Long): NotificationDto

    fun existsByUserId(userId: Long): Boolean

    fun save(dto: NotificationDto): NotificationDto

    fun updateTimeOfLastNotification(userId: Long, time: LocalDateTime): NotificationDto

    fun updateNotifications(notifications: Collection<NotificationDto>): Set<NotificationDto>

    fun enableByUserId(userId: Long)

    fun disableByUserId(userId: Long)

    fun isActiveNotification(userId: Long): Boolean

    fun changeQuiteMode(userId: Long, start: LocalTime, end: LocalTime)

    fun disableQuietMode(userId: Long)

}
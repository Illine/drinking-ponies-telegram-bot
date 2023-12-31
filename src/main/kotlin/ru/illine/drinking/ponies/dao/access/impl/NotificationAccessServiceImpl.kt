package ru.illine.drinking.ponies.dao.access.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.dao.repository.NotificationRepository
import ru.illine.drinking.ponies.model.dto.NotificationDto
import java.time.LocalDateTime
import java.util.stream.Collectors

@Service
class NotificationAccessServiceImpl(
    private val repository: NotificationRepository
) : NotificationAccessService {

    private val log = LoggerFactory.getLogger("ACCESS-SERVICE")

    @Transactional
    override fun findAll(): Set<NotificationDto> {
        log.info("Finding all Notification records")

        return repository.findAll()
            .map { it.toDto() }
            .toSet()
    }

    @Transactional
    override fun findByUserId(userId: Long): NotificationDto {
        log.info("Finding a Notification by userId [$userId]")

        return requireNotNull(
            repository.findByUserId(userId),
            { "Not found a Notification by userId [$userId]" }
        ).toDto()
    }

    @Transactional
    override fun existsByUserId(userId: Long): Boolean {
        log.info("Does a notification exist for the userId: [$userId]?")
        return repository.existsByUserId(userId)
    }

    @Transactional
    override fun save(dto: NotificationDto): NotificationDto {
        log.info("Saving a Notification by userId [${dto.userId}]")

        val foundEntity = repository.findByUserId(dto.userId)
        if (foundEntity == null) {
            log.info("The Notification will be saved as a new record")
            val entity = dto.toEntity()
            return repository.save(entity).toDto()
        }

        log.info("The Notification will be updated for an existed entity by id: [${foundEntity.id}]")
        val entity = dto.toEntity()
        entity.id = foundEntity.id
        return repository.save(entity).toDto()
    }

    @Transactional
    override fun updateTimeOfLastNotification(userId: Long, time: LocalDateTime): NotificationDto {
        log.info("Updating a time of last notification for a Notification by userId [$userId]")

        val foundEntity = requireNotNull(
            repository.findByUserId(userId),
            { "Not found a Notification by userId [$userId]" }
        )

        return foundEntity
            .apply {
                timeOfLastNotification = time
                notificationAttempts = 0
            }
            .let { repository.save(it) }.toDto()
    }

    @Transactional
    override fun updateNotifications(notifications: Collection<NotificationDto>): Set<NotificationDto> {
        log.info("Updating of notifications...")

        val updatedEntities =
            notifications
                .stream()
                .map { it.toEntity() }
                .toList()

        return repository.saveAll(updatedEntities)
            .stream()
            .map { it.toDto() }
            .collect(Collectors.toSet())
    }

    @Transactional
    override fun enableByUserId(userId: Long) {
        log.info("A notification will be enabled (deleted = false) by userId [$userId]")
        repository.switchDeleted(userId, false)
    }

    @Transactional
    override fun disableByUserId(userId: Long) {
        log.info("A notification will be disabled (deleted = true) by userId [$userId]")
        repository.switchDeleted(userId, true)
    }
}
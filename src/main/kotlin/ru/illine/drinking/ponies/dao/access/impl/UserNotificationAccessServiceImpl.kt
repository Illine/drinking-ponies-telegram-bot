package ru.illine.drinking.ponies.dao.access.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.illine.drinking.ponies.dao.access.UserNotificationAccessService
import ru.illine.drinking.ponies.dao.repository.UserNotificationRepository
import ru.illine.drinking.ponies.model.dto.UserNotificationDto
import java.time.OffsetDateTime

@Service
class UserNotificationAccessServiceImpl(
    private val repository: UserNotificationRepository
) : UserNotificationAccessService {

    private val log = LoggerFactory.getLogger("ACCESS-SERVICE")

    @Transactional
    override fun findAll(): Set<UserNotificationDto> {
        log.info("Finding all UserNotification records")

        return repository.findAll()
            .map { it.toDto() }
            .toSet()
    }

    @Transactional
    override fun findByUserId(userId: Long): UserNotificationDto {
        log.info("Finding a UserNotification by userId [$userId]")

        return requireNotNull(
            repository.findByUserId(userId),
            { "Not found a UserNotification by userId [$userId]" }
        ).toDto()
    }

    @Transactional
    override fun save(dto: UserNotificationDto): UserNotificationDto {
        log.info("Saving a UserNotification by userId [${dto.userId}]")

        val entity = dto.toEntity()
        return repository.save(entity).toDto()
    }

    @Transactional
    override fun updateTimeOfLastNotification(userId: Long, time: OffsetDateTime): UserNotificationDto {
        log.info("Updating a time of last notification for a UserNotification by userId [$userId]")

        val foundEntity = requireNotNull(
            repository.findByUserId(userId),
            { "Not found a UserNotification by userId [$userId]" }
        )
        foundEntity.timeOfLastNotification = time
        return repository.save(foundEntity).toDto()
    }
}
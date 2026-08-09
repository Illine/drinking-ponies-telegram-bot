package ru.illine.drinking.ponies.dao.access.impl

import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.illine.drinking.ponies.config.cache.CacheConfig
import ru.illine.drinking.ponies.dao.access.TelegramUserAccessService
import ru.illine.drinking.ponies.dao.repository.TelegramUserRepository
import ru.illine.drinking.ponies.dao.repository.UserStateEventRepository
import ru.illine.drinking.ponies.exception.TelegramUserNotFoundException
import ru.illine.drinking.ponies.mapper.AdminUserMapper
import ru.illine.drinking.ponies.model.base.AdminUserStatusFilter
import ru.illine.drinking.ponies.model.base.UserStateEventType
import ru.illine.drinking.ponies.model.dto.internal.AdminUserDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserProfileDto
import ru.illine.drinking.ponies.model.dto.internal.UserAccessDto
import ru.illine.drinking.ponies.model.dto.internal.UserCountsDto
import ru.illine.drinking.ponies.model.dto.internal.UserStateChangeDto
import ru.illine.drinking.ponies.model.entity.TelegramUserEntity
import ru.illine.drinking.ponies.model.entity.UserStateEventEntity
import java.time.Clock
import java.time.LocalDateTime

@Service
class TelegramUserAccessServiceImpl(
    private val telegramUserRepository: TelegramUserRepository,
    private val userStateEventRepository: UserStateEventRepository,
    private val clock: Clock,
) : TelegramUserAccessService {
    private val logger = LoggerFactory.getLogger("ACCESS-SERVICE")

    @Cacheable(CacheConfig.USER_ACCESS_FLAGS, key = "#externalUserId")
    override fun resolveAccessFlags(externalUserId: Long): UserAccessDto {
        logger.debug("Resolving access flags for externalUserId={}", externalUserId)

        val user =
            telegramUserRepository.findByExternalUserIdIncludingDeleted(externalUserId)
                ?: return UserAccessDto(externalUserId = externalUserId)

        return user.toAccessDto()
    }

    // The cache is a throttle rather than a store: a miss means the profile is due for a re-read, once per its TTL.
    // A call without a profile returns null and is not cached, so it does not eat the window of a real rename.
    @Transactional
    @Cacheable(CacheConfig.USER_PROFILE_SYNC, key = "#externalUserId", unless = "#result == null")
    override fun syncProfile(
        externalUserId: Long,
        profile: TelegramUserProfileDto,
    ): Boolean? {
        // Telegram guarantees a first name and nothing else, so its absence means no profile was handed over at all.
        if (profile.firstName == null) {
            logger.info("Nothing to sync for externalUserId={}: the caller carries no profile", externalUserId)
            return null
        }

        val user =
            telegramUserRepository
                .findByExternalUserIdIncludingDeleted(externalUserId)
                ?.takeUnless { it.matches(profile) }

        user?.apply {
            logger.debug("Refreshing profile for externalUserId={}", externalUserId)
            firstName = profile.firstName
            lastName = profile.lastName
            username = profile.username
        }

        return user != null
    }

    @Transactional(readOnly = true)
    override fun findAllForAdmin(
        search: String?,
        status: AdminUserStatusFilter,
        pageable: Pageable,
    ): List<AdminUserDto> {
        logger.debug("Listing users for admin: search={}, status={}, page={}", search, status, pageable.pageNumber)

        return telegramUserRepository
            .findAllForAdmin(search, status.deleted, status.banned, pageable)
            .map { AdminUserMapper.toDto(it) }
    }

    @Transactional(readOnly = true)
    override fun findByIdForAdmin(id: Long): AdminUserDto? {
        logger.debug("Loading user [{}] for admin", id)

        return telegramUserRepository.findByIdForAdmin(id)?.let { AdminUserMapper.toDto(it) }
    }

    @Transactional(readOnly = true)
    override fun countForAdmin(search: String?): UserCountsDto {
        logger.debug("Counting users for admin: search={}", search)

        return AdminUserMapper.toCounts(telegramUserRepository.countForAdmin(search))
    }

    @Transactional
    @CacheEvict(CacheConfig.USER_ACCESS_FLAGS, key = "#result.externalUserId")
    override fun updateState(
        id: Long,
        actorId: Long,
        change: UserStateChangeDto,
    ): UserAccessDto {
        val user =
            telegramUserRepository.findByIdIncludingDeleted(id)
                ?: throw TelegramUserNotFoundException("No user with id: [$id]")

        val events =
            buildList {
                change.deleted?.takeIf { it != user.deleted }?.let {
                    user.deleted = it
                    add(if (it) UserStateEventType.DEACTIVATED else UserStateEventType.RESTORED)
                }
                change.banned?.takeIf { it != user.isBanned }?.let {
                    user.isBanned = it
                    add(if (it) UserStateEventType.BANNED else UserStateEventType.UNBANNED)
                }
            }

        if (events.isNotEmpty()) {
            logger.info("Applying {} to user [{}]", events, id)
            recordEvents(id, actorId, events)
        }

        return user.toAccessDto()
    }

    @Transactional
    @CacheEvict(CacheConfig.USER_ACCESS_FLAGS, key = "#externalUserId", condition = "#result")
    override fun restoreIfDeleted(externalUserId: Long): Boolean {
        val user =
            telegramUserRepository
                .findByExternalUserIdIncludingDeleted(externalUserId)
                ?.takeIf { it.deleted }
                ?: return false

        val id = requireNotNull(user.id)
        logger.info("Restoring soft deleted user [{}]", externalUserId)
        user.deleted = false
        recordEvents(id, id, listOf(UserStateEventType.RESTORED))

        return true
    }

    private fun recordEvents(
        userId: Long,
        actorId: Long,
        types: List<UserStateEventType>,
    ) {
        val eventTime = LocalDateTime.now(clock)

        userStateEventRepository.saveAll(
            types.map {
                UserStateEventEntity(
                    userId = userId,
                    actorUserId = actorId,
                    eventType = it,
                    eventTime = eventTime,
                )
            },
        )
    }

    private fun TelegramUserEntity.toAccessDto(): UserAccessDto =
        UserAccessDto(
            id = id,
            externalUserId = externalUserId,
            isAdmin = isAdmin,
            isBanned = isBanned,
            isDeleted = deleted,
        )

    private fun TelegramUserEntity.matches(profile: TelegramUserProfileDto): Boolean =
        firstName == profile.firstName &&
            lastName == profile.lastName &&
            username == profile.username
}

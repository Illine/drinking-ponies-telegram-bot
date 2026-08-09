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
import ru.illine.drinking.ponies.mapper.AdminUserMapper
import ru.illine.drinking.ponies.model.base.AdminUserStatusFilter
import ru.illine.drinking.ponies.model.dto.internal.AdminUserDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserProfileDto
import ru.illine.drinking.ponies.model.dto.internal.UserAccessDto
import ru.illine.drinking.ponies.model.dto.internal.UserCountsDto
import ru.illine.drinking.ponies.model.entity.TelegramUserEntity

@Service
class TelegramUserAccessServiceImpl(
    private val telegramUserRepository: TelegramUserRepository,
) : TelegramUserAccessService {
    private val logger = LoggerFactory.getLogger("ACCESS-SERVICE")

    @Transactional
    @Cacheable(CacheConfig.USER_ACCESS_FLAGS, key = "#externalUserId")
    override fun resolveAccessFlags(
        externalUserId: Long,
        profile: TelegramUserProfileDto,
    ): UserAccessDto {
        logger.debug("Resolving access flags for externalUserId={}", externalUserId)

        val user =
            telegramUserRepository.findByExternalUserIdIncludingDeleted(externalUserId) ?: return UserAccessDto()
        refreshProfile(user, profile)

        return UserAccessDto(isAdmin = user.isAdmin, isBanned = user.isBanned, isDeleted = user.deleted)
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
    @CacheEvict(CacheConfig.USER_ACCESS_FLAGS, key = "#externalUserId")
    override fun updateState(
        id: Long,
        externalUserId: Long,
        deleted: Boolean?,
    ) {
        logger.debug("Setting deleted={} for user [{}]", deleted, id)

        telegramUserRepository.updateState(id, deleted)
    }

    @Transactional
    @CacheEvict(CacheConfig.USER_ACCESS_FLAGS, key = "#externalUserId")
    override fun restoreIfDeleted(externalUserId: Long): Boolean {
        val restored = telegramUserRepository.restoreByExternalUserId(externalUserId) > 0
        if (restored) {
            logger.info("Restoring soft deleted user [{}]", externalUserId)
        }

        return restored
    }

    private fun refreshProfile(
        user: TelegramUserEntity,
        profile: TelegramUserProfileDto,
    ) {
        if (profile.firstName == null) return

        if (user.matches(profile)) return

        logger.debug("Refreshing profile for externalUserId={}", user.externalUserId)
        user.firstName = profile.firstName
        user.lastName = profile.lastName
        user.username = profile.username
    }

    private fun TelegramUserEntity.matches(profile: TelegramUserProfileDto): Boolean =
        firstName == profile.firstName &&
            lastName == profile.lastName &&
            username == profile.username
}

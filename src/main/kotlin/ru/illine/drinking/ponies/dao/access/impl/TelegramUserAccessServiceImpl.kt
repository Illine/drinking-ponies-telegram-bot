package ru.illine.drinking.ponies.dao.access.impl

import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.illine.drinking.ponies.config.cache.CacheConfig
import ru.illine.drinking.ponies.dao.access.TelegramUserAccessService
import ru.illine.drinking.ponies.dao.repository.TelegramUserRepository

@Service
class TelegramUserAccessServiceImpl(
    private val telegramUserRepository: TelegramUserRepository,
) : TelegramUserAccessService {

    private val logger = LoggerFactory.getLogger("ACCESS-SERVICE")

    // When admin promote/demote endpoints are added,
    // annotate them with @CacheEvict(CacheConfig.USER_IS_ADMIN, key = "#externalUserId")
    @Transactional(readOnly = true)
    @Cacheable(CacheConfig.USER_IS_ADMIN, key = "#externalUserId")
    override fun findIsAdminByExternalUserId(externalUserId: Long): Boolean {
        logger.debug("Resolving isAdmin for externalUserId={}", externalUserId)

        return telegramUserRepository.findByExternalUserId(externalUserId)?.isAdmin ?: false
    }
}

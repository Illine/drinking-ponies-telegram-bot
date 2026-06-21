package ru.illine.drinking.ponies.config.cache

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.illine.drinking.ponies.config.property.CacheProperties

@Configuration
@EnableCaching
class CacheConfig(
    private val cacheProperties: CacheProperties,
) {
    companion object {
        const val USER_IS_ADMIN = "user-is-admin"
        const val WATER_FIRST_ENTRY = "water-first-entry"

        private val ALL_CACHES = listOf(USER_IS_ADMIN, WATER_FIRST_ENTRY)
    }

    // Wraps the underlying Caffeine manager so cache mutations (@CacheEvict, @CachePut) inside a
    // @Transactional method are deferred until the transaction commits. Without this, an evict
    // can happen before commit and a concurrent reader may re-cache the stale value.
    @Bean
    fun cacheManager(): CacheManager {
        val target = CaffeineCacheManager()
        ALL_CACHES.forEach { name ->
            val override = cacheProperties.overrides[name]
            val ttl = override?.ttl ?: cacheProperties.default.ttl
            val maximumSize = override?.maximumSize ?: cacheProperties.default.maximumSize
            target.registerCustomCache(
                name,
                Caffeine
                    .newBuilder()
                    .expireAfterWrite(ttl)
                    .maximumSize(maximumSize)
                    .build(),
            )
        }
        return TransactionAwareCacheManagerProxy(target)
    }
}

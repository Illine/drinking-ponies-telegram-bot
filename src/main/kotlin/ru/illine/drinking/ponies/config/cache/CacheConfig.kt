package ru.illine.drinking.ponies.config.cache

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.illine.drinking.ponies.config.property.CacheProperties

@Configuration
@EnableCaching
class CacheConfig(
    private val cacheProperties: CacheProperties
) {

    companion object {
        const val USER_IS_ADMIN = "user-is-admin"

        private val ALL_CACHES = listOf(USER_IS_ADMIN)
    }

    @Bean
    fun cacheManager(): CacheManager {
        val manager = CaffeineCacheManager()
        ALL_CACHES.forEach { name ->
            val override = cacheProperties.overrides[name]
            val ttl = override?.ttl ?: cacheProperties.default.ttl
            val maximumSize = override?.maximumSize ?: cacheProperties.default.maximumSize
            manager.registerCustomCache(
                name,
                Caffeine.newBuilder()
                    .expireAfterWrite(ttl)
                    .maximumSize(maximumSize)
                    .build()
            )
        }
        return manager
    }
}

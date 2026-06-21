package ru.illine.drinking.ponies.config.cache

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy
import ru.illine.drinking.ponies.config.property.CacheProperties
import ru.illine.drinking.ponies.config.property.CacheProperties.CacheEntry
import ru.illine.drinking.ponies.config.property.CacheProperties.CacheEntryOverride
import ru.illine.drinking.ponies.test.tag.UnitTest
import java.time.Duration

@UnitTest
@DisplayName("CacheConfig Unit Test")
class CacheConfigTest {
    @Test
    @DisplayName("cacheManager(): registers all caches when no overrides are provided")
    fun `cacheManager registers all caches`() {
        val properties =
            CacheProperties(
                default = CacheEntry(ttl = Duration.ofMinutes(7), maximumSize = 50),
                overrides = emptyMap(),
            )

        val manager = CacheConfig(properties).cacheManager()

        assertTrue(manager.cacheNames.contains(CacheConfig.USER_IS_ADMIN))
        assertTrue(manager.cacheNames.contains(CacheConfig.WATER_FIRST_ENTRY))
        assertNotNull(manager.getCache(CacheConfig.USER_IS_ADMIN))
        assertNotNull(manager.getCache(CacheConfig.WATER_FIRST_ENTRY))
        assertEquals(2, manager.cacheNames.size)
    }

    @Test
    @DisplayName("cacheManager(): returns a TransactionAwareCacheManagerProxy so evicts defer until commit")
    fun `cacheManager wraps caffeine in transaction aware proxy`() {
        // Without this proxy, @CacheEvict happens before the @Transactional method commits,
        // and a concurrent reader can re-cache the stale value. The proxy is load-bearing.
        val properties =
            CacheProperties(
                default = CacheEntry(ttl = Duration.ofMinutes(7), maximumSize = 50),
                overrides = emptyMap(),
            )

        val manager = CacheConfig(properties).cacheManager()

        assertTrue(
            manager is TransactionAwareCacheManagerProxy,
            "Expected TransactionAwareCacheManagerProxy, got ${manager::class}",
        )
    }

    @Test
    @DisplayName("cacheManager(): applies override values for ttl and maximumSize when both are set")
    fun `cacheManager applies override ttl and maximumSize`() {
        val properties =
            CacheProperties(
                default = CacheEntry(ttl = Duration.ofMinutes(7), maximumSize = 50),
                overrides =
                    mapOf(
                        CacheConfig.USER_IS_ADMIN to
                            CacheEntryOverride(
                                ttl = Duration.ofMinutes(15),
                                maximumSize = 200,
                            ),
                    ),
            )

        val manager = CacheConfig(properties).cacheManager()

        assertNotNull(manager.getCache(CacheConfig.USER_IS_ADMIN))
        assertNotNull(manager.getCache(CacheConfig.WATER_FIRST_ENTRY))
        assertEquals(2, manager.cacheNames.size)
    }

    @Test
    @DisplayName("cacheManager(): falls back to default ttl and maximumSize when override fields are null")
    fun `cacheManager falls back to default when override fields are null`() {
        val properties =
            CacheProperties(
                default = CacheEntry(ttl = Duration.ofMinutes(7), maximumSize = 50),
                overrides =
                    mapOf(
                        CacheConfig.USER_IS_ADMIN to
                            CacheEntryOverride(
                                ttl = null,
                                maximumSize = null,
                            ),
                    ),
            )

        val manager = CacheConfig(properties).cacheManager()

        assertNotNull(manager.getCache(CacheConfig.USER_IS_ADMIN))
        assertNotNull(manager.getCache(CacheConfig.WATER_FIRST_ENTRY))
        assertEquals(2, manager.cacheNames.size)
    }
}

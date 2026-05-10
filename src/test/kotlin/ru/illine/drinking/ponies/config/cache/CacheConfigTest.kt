package ru.illine.drinking.ponies.config.cache

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.illine.drinking.ponies.config.property.CacheProperties
import ru.illine.drinking.ponies.config.property.CacheProperties.CacheEntry
import ru.illine.drinking.ponies.config.property.CacheProperties.CacheEntryOverride
import ru.illine.drinking.ponies.test.tag.UnitTest
import java.time.Duration

@UnitTest
@DisplayName("CacheConfig Unit Test")
class CacheConfigTest {

    @Test
    @DisplayName("cacheManager(): registers USER_IS_ADMIN cache when no overrides are provided")
    fun `cacheManager registers USER_IS_ADMIN cache`() {
        val properties = CacheProperties(
            default = CacheEntry(ttl = Duration.ofMinutes(7), maximumSize = 50),
            overrides = emptyMap()
        )

        val manager = CacheConfig(properties).cacheManager()

        assertTrue(manager.cacheNames.contains(CacheConfig.USER_IS_ADMIN))
        assertNotNull(manager.getCache(CacheConfig.USER_IS_ADMIN))
        assertEquals(1, manager.cacheNames.size)
    }

    @Test
    @DisplayName("cacheManager(): applies override values for ttl and maximumSize when both are set")
    fun `cacheManager applies override ttl and maximumSize`() {
        val properties = CacheProperties(
            default = CacheEntry(ttl = Duration.ofMinutes(7), maximumSize = 50),
            overrides = mapOf(
                CacheConfig.USER_IS_ADMIN to CacheEntryOverride(
                    ttl = Duration.ofMinutes(15),
                    maximumSize = 200
                )
            )
        )

        val manager = CacheConfig(properties).cacheManager()

        assertNotNull(manager.getCache(CacheConfig.USER_IS_ADMIN))
        assertEquals(1, manager.cacheNames.size)
    }

    @Test
    @DisplayName("cacheManager(): falls back to default ttl and maximumSize when override fields are null")
    fun `cacheManager falls back to default when override fields are null`() {
        val properties = CacheProperties(
            default = CacheEntry(ttl = Duration.ofMinutes(7), maximumSize = 50),
            overrides = mapOf(
                CacheConfig.USER_IS_ADMIN to CacheEntryOverride(
                    ttl = null,
                    maximumSize = null
                )
            )
        )

        val manager = CacheConfig(properties).cacheManager()

        assertNotNull(manager.getCache(CacheConfig.USER_IS_ADMIN))
        assertEquals(1, manager.cacheNames.size)
    }
}

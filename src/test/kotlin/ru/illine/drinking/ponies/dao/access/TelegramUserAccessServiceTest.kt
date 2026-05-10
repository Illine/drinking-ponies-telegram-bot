package ru.illine.drinking.ponies.dao.access

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlConfig
import ru.illine.drinking.ponies.config.cache.CacheConfig
import ru.illine.drinking.ponies.dao.repository.TelegramUserRepository
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest

@SpringIntegrationTest
@DisplayName("TelegramUserAccessService Spring Integration Test")
@Sql(
    scripts = ["classpath:sql/access/TelegramUserAccessService.sql"],
    config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
    scripts = ["classpath:sql/clear.sql"],
    config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class TelegramUserAccessServiceTest @Autowired constructor(
    private val accessService: TelegramUserAccessService,
    private val telegramUserRepository: TelegramUserRepository,
    private val cacheManager: CacheManager,
) {

    private val ADMIN_USER_ID = 1L
    private val NON_ADMIN_USER_ID = 2L
    private val MISSING_USER_ID = 0L

    @BeforeEach
    fun clearCache() {
        cacheManager.getCache(CacheConfig.USER_IS_ADMIN)?.clear()
    }

    @Test
    @DisplayName("findIsAdminByExternalUserId(): admin user - returns true")
    fun `returns true for admin`() {
        assertTrue(accessService.findIsAdminByExternalUserId(ADMIN_USER_ID))
    }

    @Test
    @DisplayName("findIsAdminByExternalUserId(): non-admin user - returns false")
    fun `returns false for non-admin`() {
        assertFalse(accessService.findIsAdminByExternalUserId(NON_ADMIN_USER_ID))
    }

    @Test
    @DisplayName("findIsAdminByExternalUserId(): missing user - returns false")
    fun `returns false for missing user`() {
        assertFalse(accessService.findIsAdminByExternalUserId(MISSING_USER_ID))
    }

    @Test
    @DisplayName("findIsAdminByExternalUserId(): caches result after first call")
    fun `caches true result after first call`() {
        val cache = cacheManager.getCache(CacheConfig.USER_IS_ADMIN)
        assertNotNull(cache)
        assertNull(cache!!.get(ADMIN_USER_ID))

        accessService.findIsAdminByExternalUserId(ADMIN_USER_ID)

        assertEquals(true, cache.get(ADMIN_USER_ID)?.get())
    }

    @Test
    @DisplayName("findIsAdminByExternalUserId(): caches false for missing user")
    fun `caches false result for missing user`() {
        val cache = cacheManager.getCache(CacheConfig.USER_IS_ADMIN)!!

        accessService.findIsAdminByExternalUserId(MISSING_USER_ID)

        assertEquals(false, cache.get(MISSING_USER_ID)?.get())
    }

    @Test
    @DisplayName("findIsAdminByExternalUserId(): returns stale value from cache after DB change until eviction")
    fun `returns stale value from cache after db change`() {
        assertTrue(accessService.findIsAdminByExternalUserId(ADMIN_USER_ID))

        val entity = telegramUserRepository.findByExternalUserId(ADMIN_USER_ID)!!
        entity.isAdmin = false
        telegramUserRepository.saveAndFlush(entity)

        assertTrue(
            accessService.findIsAdminByExternalUserId(ADMIN_USER_ID),
            "Expected stale cached value (true) before manual cache eviction"
        )

        cacheManager.getCache(CacheConfig.USER_IS_ADMIN)?.clear()

        assertFalse(accessService.findIsAdminByExternalUserId(ADMIN_USER_ID))
    }
}

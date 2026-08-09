package ru.illine.drinking.ponies.dao.access

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlConfig
import ru.illine.drinking.ponies.config.cache.CacheConfig
import ru.illine.drinking.ponies.dao.repository.TelegramUserRepository
import ru.illine.drinking.ponies.dao.repository.UserStateEventRepository
import ru.illine.drinking.ponies.exception.TelegramUserNotFoundException
import ru.illine.drinking.ponies.model.base.AdminUserStatusFilter
import ru.illine.drinking.ponies.model.base.UserStateEventType
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserProfileDto
import ru.illine.drinking.ponies.model.dto.internal.UserAccessDto
import ru.illine.drinking.ponies.model.dto.internal.UserStateChangeDto
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest
import java.time.Clock
import java.time.LocalDateTime

@SpringIntegrationTest
@DisplayName("TelegramUserAccessService Spring Integration Test")
@Sql(
    scripts = ["classpath:sql/access/TelegramUserAccessService.sql"],
    config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
)
@Sql(
    scripts = ["classpath:sql/clear.sql"],
    config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
)
class TelegramUserAccessServiceTest
    @Autowired
    constructor(
        private val accessService: TelegramUserAccessService,
        private val telegramUserRepository: TelegramUserRepository,
        private val userStateEventRepository: UserStateEventRepository,
        private val cacheManager: CacheManager,
        private val clock: Clock,
    ) {
        @BeforeEach
        fun clearCache() {
            cacheManager.getCache(CacheConfig.USER_ACCESS_FLAGS)?.clear()
            cacheManager.getCache(CacheConfig.USER_PROFILE_SYNC)?.clear()
        }

        @Nested
        @DisplayName("resolveAccessFlags()")
        inner class ResolveAccessFlags {
            @ParameterizedTest(name = "[{index}] externalUserId={1} - admin={2}, banned={3}, deleted={4}")
            @CsvSource(
                "1, 1,      true,  false, false",
                "2, 2,      false, false, false",
                "3, 777001, false, false, true",
                "4, 777002, false, true,  false",
                "6, 777004, false, true,  true",
            )
            @DisplayName("returns the stored flags with the internal id, a soft-deleted user resolves as well")
            fun `returns stored flags`(
                id: Long,
                externalUserId: Long,
                isAdmin: Boolean,
                isBanned: Boolean,
                isDeleted: Boolean,
            ) {
                val access = accessService.resolveAccessFlags(externalUserId)

                assertEquals(UserAccessDto(id, externalUserId, isAdmin, isBanned, isDeleted), access)
            }

            @Test
            @DisplayName("an unknown caller resolves to the default flags without an internal id")
            fun `returns default flags for an unknown caller`() {
                val access = accessService.resolveAccessFlags(MISSING_EXTERNAL_ID)

                assertEquals(UserAccessDto(externalUserId = MISSING_EXTERNAL_ID), access)
            }

            @Test
            @DisplayName("caches the resolved flags after the first call")
            fun `caches resolved flags`() {
                val cache = cacheManager.getCache(CacheConfig.USER_ACCESS_FLAGS)!!
                assertNull(cache.get(ADMIN_EXTERNAL_ID))

                accessService.resolveAccessFlags(ADMIN_EXTERNAL_ID)

                assertEquals(
                    UserAccessDto(ADMIN_USER_ID, ADMIN_EXTERNAL_ID, isAdmin = true),
                    cache.get(ADMIN_EXTERNAL_ID)?.get(),
                )
            }

            @Test
            @DisplayName("caches the default flags for a user that is not stored at all")
            fun `caches default flags for missing user`() {
                val cache = cacheManager.getCache(CacheConfig.USER_ACCESS_FLAGS)!!

                accessService.resolveAccessFlags(MISSING_EXTERNAL_ID)

                assertEquals(
                    UserAccessDto(externalUserId = MISSING_EXTERNAL_ID),
                    cache.get(MISSING_EXTERNAL_ID)?.get(),
                )
            }

            @Test
            @DisplayName("keeps serving the cached flags after a DB change until the entry is evicted")
            fun `returns stale value from cache after db change`() {
                assertTrue(accessService.resolveAccessFlags(ADMIN_EXTERNAL_ID).isAdmin)

                val entity = telegramUserRepository.findByExternalUserId(ADMIN_EXTERNAL_ID)!!
                entity.isAdmin = false
                telegramUserRepository.saveAndFlush(entity)

                assertTrue(
                    accessService.resolveAccessFlags(ADMIN_EXTERNAL_ID).isAdmin,
                    "Expected stale cached value (true) before eviction",
                )

                cacheManager.getCache(CacheConfig.USER_ACCESS_FLAGS)?.clear()

                assertFalse(accessService.resolveAccessFlags(ADMIN_EXTERNAL_ID).isAdmin)
            }

            @Test
            @DisplayName("resolving the flags leaves the stored profile alone, that is what syncProfile is for")
            fun `resolving the flags never touches the profile`() {
                accessService.resolveAccessFlags(ADMIN_EXTERNAL_ID)

                val stored = telegramUserRepository.findByExternalUserIdIncludingDeleted(ADMIN_EXTERNAL_ID)!!
                assertEquals("Alisa", stored.firstName)
                assertEquals("Petrova", stored.lastName)
                assertEquals("alisaadmin", stored.username)
            }
        }

        @Nested
        @DisplayName("syncProfile()")
        inner class SyncProfile {
            @Test
            @DisplayName("stores the profile from initData when it differs from the stored one")
            fun `refreshes profile on mismatch`() {
                val profile =
                    DtoGenerator.generateTelegramUserProfileDto(
                        firstName = "Alisa",
                        lastName = "Sidorova",
                        username = "alisamoved",
                    )

                assertEquals(true, accessService.syncProfile(ADMIN_EXTERNAL_ID, profile))

                val stored = telegramUserRepository.findByExternalUserIdIncludingDeleted(ADMIN_EXTERNAL_ID)!!
                assertEquals("Alisa", stored.firstName)
                assertEquals("Sidorova", stored.lastName)
                assertEquals("alisamoved", stored.username)
            }

            @Test
            @DisplayName("leaves the stored profile alone when the caller has no first name to offer")
            fun `keeps profile when caller sends nothing`() {
                assertNull(accessService.syncProfile(ADMIN_EXTERNAL_ID, TelegramUserProfileDto()))

                val stored = telegramUserRepository.findByExternalUserIdIncludingDeleted(ADMIN_EXTERNAL_ID)!!
                assertEquals("Alisa", stored.firstName)
                assertEquals("Petrova", stored.lastName)
                assertEquals("alisaadmin", stored.username)
            }

            @Test
            @DisplayName("drops the optional fields Telegram stopped sending, first name being present")
            fun `drops the fields telegram no longer sends`() {
                val profile =
                    DtoGenerator.generateTelegramUserProfileDto(
                        firstName = "Alisa",
                        lastName = null,
                        username = null,
                    )

                accessService.syncProfile(ADMIN_EXTERNAL_ID, profile)

                val stored = telegramUserRepository.findByExternalUserIdIncludingDeleted(ADMIN_EXTERNAL_ID)!!
                assertEquals("Alisa", stored.firstName)
                assertNull(stored.lastName, "A name Telegram no longer sends has to be dropped")
                assertNull(stored.username, "A username Telegram no longer sends has to be dropped")
            }

            @Test
            @DisplayName("refreshes the profile of a soft-deleted user as well")
            fun `refreshes profile of soft deleted user`() {
                val profile =
                    DtoGenerator.generateTelegramUserProfileDto(
                        firstName = "Carol",
                        lastName = "Renamed",
                        username = "carolback",
                    )

                accessService.syncProfile(DELETED_EXTERNAL_ID, profile)

                val stored = telegramUserRepository.findByExternalUserIdIncludingDeleted(DELETED_EXTERNAL_ID)!!
                assertEquals("Carol", stored.firstName)
                assertEquals("Renamed", stored.lastName)
                assertEquals("carolback", stored.username)
            }

            @Test
            @DisplayName("reports no refresh when the stored profile already matches the one from initData")
            fun `reports no refresh when the profile already matches`() {
                val profile =
                    DtoGenerator.generateTelegramUserProfileDto(
                        firstName = "Alisa",
                        lastName = "Petrova",
                        username = "alisaadmin",
                    )

                assertEquals(false, accessService.syncProfile(ADMIN_EXTERNAL_ID, profile))

                val cache = cacheManager.getCache(CacheConfig.USER_PROFILE_SYNC)!!
                assertEquals(false, cache.get(ADMIN_EXTERNAL_ID)?.get(), "An answer of its own fills the window")
            }

            @Test
            @DisplayName("a call without a profile leaves the throttle window free for the next real one")
            fun `a call without a profile does not take the throttle window`() {
                assertNull(accessService.syncProfile(ADMIN_EXTERNAL_ID, TelegramUserProfileDto()))

                val cache = cacheManager.getCache(CacheConfig.USER_PROFILE_SYNC)!!
                assertNull(cache.get(ADMIN_EXTERNAL_ID), "Nothing was answered, so nothing may be throttled")

                val renamed = DtoGenerator.generateTelegramUserProfileDto(firstName = "Renamed")
                assertEquals(true, accessService.syncProfile(ADMIN_EXTERNAL_ID, renamed))
                assertEquals(
                    "Renamed",
                    telegramUserRepository.findByExternalUserIdIncludingDeleted(ADMIN_EXTERNAL_ID)!!.firstName,
                )
            }

            @Test
            @DisplayName("throttles by its own cache: a second call within the TTL does not read the profile again")
            fun `throttles the refresh by its own cache`() {
                val profile = DtoGenerator.generateTelegramUserProfileDto(firstName = "Alisa", username = "alisamoved")
                accessService.syncProfile(ADMIN_EXTERNAL_ID, profile)

                val renamedAgain = DtoGenerator.generateTelegramUserProfileDto(firstName = "Renamed")
                accessService.syncProfile(ADMIN_EXTERNAL_ID, renamedAgain)

                val stored = telegramUserRepository.findByExternalUserIdIncludingDeleted(ADMIN_EXTERNAL_ID)!!
                assertEquals("Alisa", stored.firstName, "The throttled call must not have reached the database")
            }
        }

        @Nested
        @DisplayName("findAllForAdmin()")
        inner class FindAllForAdmin {
            @Test
            @DisplayName("lists soft-deleted users too, most recently active first and never-active last")
            fun `lists every user ordered by last activity`() {
                val users = accessService.findAllForAdmin(null, AdminUserStatusFilter.ALL, PageRequest.of(0, PAGE_SIZE))

                assertEquals(listOf(3L, 2L, 1L, 4L, 5L, 6L, 7L, 8L), users.map { it.id })
            }

            @ParameterizedTest(name = "[{index}] status={0} - ids {1}")
            @CsvSource(
                "ALL,      '3,2,1,4,5,6,7,8'",
                "ACTIVE,   '2,1,5,7,8'",
                "INACTIVE, '3'",
                "BANNED,   '4,6'",
            )
            @DisplayName("filters by status, the categories being mutually exclusive")
            fun `filters by status`(
                status: AdminUserStatusFilter,
                expectedIds: String,
            ) {
                val users = accessService.findAllForAdmin(null, status, PageRequest.of(0, PAGE_SIZE))

                assertEquals(expectedIds.toIds(), users.map { it.id })
            }

            @Test
            @DisplayName("a user who is both banned and deleted belongs to BANNED, never to INACTIVE")
            fun `a ban wins over a deletion`() {
                val banned = accessService.findAllForAdmin(null, AdminUserStatusFilter.BANNED, PAGE)
                val inactive = accessService.findAllForAdmin(null, AdminUserStatusFilter.INACTIVE, PAGE)
                val all = accessService.findAllForAdmin(null, AdminUserStatusFilter.ALL, PAGE)

                assertTrue(banned.map { it.id }.contains(BANNED_DELETED_USER_ID))
                assertFalse(inactive.map { it.id }.contains(BANNED_DELETED_USER_ID))
                assertTrue(all.map { it.id }.contains(BANNED_DELETED_USER_ID), "Still one of the users, though")
            }

            @ParameterizedTest(name = "[{index}] search={0} - ids {1}")
            @CsvSource(
                "%bob%,        '2'",
                "%BOB%,        '2'",
                "%petrova%,    '1'",
                "%evefresh%,   '5'",
                "%777002%,     '4'",
                "%5%,          '5'",
                "%777%,        '3,4,5,6'",
                "'%bob smith%', '2'",
            )
            @DisplayName("searches over first name, last name, username and both ids, across field borders")
            fun `searches over name username and both ids`(
                search: String,
                expectedIds: String,
            ) {
                val users =
                    accessService.findAllForAdmin(search, AdminUserStatusFilter.ALL, PageRequest.of(0, PAGE_SIZE))

                assertEquals(expectedIds.toIds(), users.map { it.id })
            }

            @Test
            @DisplayName("returns nothing when the search matches no one")
            fun `returns empty result for a search matching nothing`() {
                val users =
                    accessService.findAllForAdmin("%zzz%", AdminUserStatusFilter.ALL, PageRequest.of(0, PAGE_SIZE))

                assertTrue(users.isEmpty())
            }

            @Test
            @DisplayName("an exact pattern without wildcards matches nothing on its own")
            fun `an unwrapped pattern does not match`() {
                val users =
                    accessService.findAllForAdmin("bob", AdminUserStatusFilter.ALL, PageRequest.of(0, PAGE_SIZE))

                assertTrue(users.isEmpty(), "Wrapping the pattern is the caller's job, not the DAO's")
            }

            @ParameterizedTest(name = "[{index}] search={0} - ids {1}")
            @CsvSource(
                """%alice\_p%, '7'""",
                "%alice_p%,   '7,8'",
                """%\_%,       '7'""",
                """%\%%,       ''""",
            )
            @DisplayName("honours the backslash escape, so a literal underscore is not a wildcard")
            fun `honours the escape character`(
                search: String,
                expectedIds: String,
            ) {
                val users =
                    accessService.findAllForAdmin(search, AdminUserStatusFilter.ALL, PageRequest.of(0, PAGE_SIZE))

                assertEquals(expectedIds.toIds(), users.map { it.id })
            }

            @ParameterizedTest(name = "[{index}] page={0}, size={1} - ids {2}")
            @CsvSource(
                "0, 2, '3,2'",
                "1, 2, '1,4'",
                "2, 2, '5,6'",
                "3, 2, '7,8'",
            )
            @DisplayName("paginates without losing the ordering")
            fun `paginates keeping the order`(
                page: Int,
                size: Int,
                expectedIds: String,
            ) {
                val users = accessService.findAllForAdmin(null, AdminUserStatusFilter.ALL, PageRequest.of(page, size))

                assertEquals(expectedIds.toIds(), users.map { it.id })
            }

            @Test
            @DisplayName("a page past the last one comes back empty")
            fun `a page past the end is empty`() {
                val users = accessService.findAllForAdmin(null, AdminUserStatusFilter.ALL, PageRequest.of(4, 2))

                assertTrue(users.isEmpty())
            }

            @Test
            @DisplayName("maps every column of a soft-deleted row, its last activity included")
            fun `maps the row of a soft deleted user`() {
                val users = accessService.findAllForAdmin(null, AdminUserStatusFilter.ALL, PageRequest.of(0, PAGE_SIZE))

                val row = users.first { it.id == DELETED_USER_ID }
                assertEquals(DELETED_EXTERNAL_ID, row.externalUserId)
                assertEquals("Carol", row.firstName)
                assertEquals("Ivanova", row.lastName)
                assertEquals("carolgone", row.username)
                assertFalse(row.isAdmin)
                assertFalse(row.isBanned)
                assertTrue(row.deleted)
                assertEquals("Asia/Kolkata", row.timeZone)
                assertEquals(LocalDateTime.of(2026, 1, 3, 12, 0), row.created)
                assertEquals(LocalDateTime.of(2026, 3, 6, 12, 0), row.lastActivity)
            }

            @Test
            @DisplayName("counts only YES and SNOOZE as activity, so a user who only cancelled has none")
            fun `last activity ignores cancel events`() {
                val users = accessService.findAllForAdmin(null, AdminUserStatusFilter.ALL, PageRequest.of(0, PAGE_SIZE))

                assertNull(users.first { it.id == BANNED_USER_ID }.lastActivity)
            }
        }

        @Nested
        @DisplayName("countForAdmin()")
        inner class CountForAdmin {
            @Test
            @DisplayName("counts each status over the whole table, the three adding up to all")
            fun `counts every status`() {
                val counts = accessService.countForAdmin(null)

                assertEquals(8L, counts.all)
                assertEquals(5L, counts.active)
                assertEquals(1L, counts.inactive)
                assertEquals(2L, counts.banned)
                assertEquals(counts.all, counts.active + counts.inactive + counts.banned)
            }

            @Test
            @DisplayName("narrows every counter down to the search result")
            fun `counts follow the search`() {
                val counts = accessService.countForAdmin("%777%")

                assertEquals(4L, counts.all)
                assertEquals(1L, counts.active)
                assertEquals(1L, counts.inactive)
                assertEquals(2L, counts.banned)
                assertEquals(counts.all, counts.active + counts.inactive + counts.banned)
            }

            @Test
            @DisplayName("returns zeros when the search matches nothing")
            fun `counts are zero when nothing matches`() {
                val counts = accessService.countForAdmin("%zzz%")

                assertEquals(0L, counts.all)
                assertEquals(0L, counts.active)
                assertEquals(0L, counts.inactive)
                assertEquals(0L, counts.banned)
            }
        }

        @Nested
        @DisplayName("findByIdForAdmin()")
        inner class FindByIdForAdmin {
            @Test
            @DisplayName("returns the card of an active user")
            fun `returns an active user`() {
                val user = accessService.findByIdForAdmin(ADMIN_USER_ID)

                assertNotNull(user)
                assertEquals(ADMIN_EXTERNAL_ID, user!!.externalUserId)
                assertTrue(user.isAdmin)
                assertFalse(user.deleted)
                assertEquals(LocalDateTime.of(2026, 1, 1, 10, 0), user.created)
                assertEquals(LocalDateTime.of(2026, 3, 4, 9, 0), user.lastActivity)
            }

            @Test
            @DisplayName("returns a soft-deleted user instead of hiding them")
            fun `returns a soft deleted user`() {
                val user = accessService.findByIdForAdmin(DELETED_USER_ID)

                assertNotNull(user)
                assertTrue(user!!.deleted)
            }

            @Test
            @DisplayName("returns null for an unknown id")
            fun `returns null for an unknown id`() {
                assertNull(accessService.findByIdForAdmin(MISSING_USER_ID))
            }
        }

        @Nested
        @DisplayName("updateState()")
        inner class UpdateState {
            @ParameterizedTest(name = "[{index}] id={0} - deleted={1}")
            @CsvSource(
                "5, true",
                "3, false",
            )
            @DisplayName("writes the flag and the admin card reflects it")
            fun `updates the deleted flag`(
                id: Long,
                deleted: Boolean,
            ) {
                accessService.updateState(id, ADMIN_USER_ID, UserStateChangeDto(deleted = deleted))

                assertEquals(deleted, accessService.findByIdForAdmin(id)!!.deleted)
            }

            @ParameterizedTest(name = "[{index}] id={0} - banned={1}")
            @CsvSource(
                "5, true",
                "4, false",
            )
            @DisplayName("writes the ban flag and the admin card reflects it")
            fun `updates the ban flag`(
                id: Long,
                banned: Boolean,
            ) {
                accessService.updateState(id, ADMIN_USER_ID, UserStateChangeDto(banned = banned))

                assertEquals(banned, accessService.findByIdForAdmin(id)!!.isBanned)
            }

            @ParameterizedTest(name = "[{index}] id={0}")
            @ValueSource(longs = [5L, 3L])
            @DisplayName("a null field leaves its column as it is, so an untouched toggle keeps its value")
            fun `a null field changes nothing`(id: Long) {
                val before = accessService.findByIdForAdmin(id)!!

                accessService.updateState(id, ADMIN_USER_ID, UserStateChangeDto())

                val after = accessService.findByIdForAdmin(id)!!
                assertEquals(before.deleted, after.deleted)
                assertEquals(before.isBanned, after.isBanned)
                assertTrue(userStateEventRepository.findAll().isEmpty(), "Nothing changed, nothing to record")
            }

            @Test
            @DisplayName("answers with the flags it has just applied, the eviction key riding along with them")
            fun `answers with the applied flags`() {
                val applied =
                    accessService.updateState(
                        ACTIVE_USER_ID,
                        ADMIN_USER_ID,
                        UserStateChangeDto(deleted = true, banned = true),
                    )

                assertEquals(
                    UserAccessDto(ACTIVE_USER_ID, ACTIVE_EXTERNAL_ID, isBanned = true, isDeleted = true),
                    applied,
                )
            }

            @Test
            @DisplayName("both fields at once are applied and recorded as two separate events")
            fun `applies both fields at once`() {
                accessService.updateState(
                    ACTIVE_USER_ID,
                    ADMIN_USER_ID,
                    UserStateChangeDto(deleted = true, banned = true),
                )

                val user = accessService.findByIdForAdmin(ACTIVE_USER_ID)!!
                assertTrue(user.deleted)
                assertTrue(user.isBanned)
                assertEquals(
                    listOf(UserStateEventType.DEACTIVATED, UserStateEventType.BANNED),
                    userStateEventRepository.findAll().sortedBy { it.id }.map { it.eventType },
                )
            }

            // Each row picks a user for whom the transition really is a change: the active one for
            // DEACTIVATED and BANNED, the already deleted and banned one for RESTORED and UNBANNED.
            @ParameterizedTest(name = "[{index}] id={0} - deleted={1}, banned={2} - {3}")
            @CsvSource(
                "5, true,  ,      DEACTIVATED",
                "6, false, ,      RESTORED",
                "5, ,      true,  BANNED",
                "6, ,      false, UNBANNED",
            )
            @DisplayName("records who changed whose state and how")
            fun `records the event`(
                id: Long,
                deleted: Boolean?,
                banned: Boolean?,
                expected: UserStateEventType,
            ) {
                accessService.updateState(id, ADMIN_USER_ID, UserStateChangeDto(deleted = deleted, banned = banned))

                val event = userStateEventRepository.findAll().single()
                assertEquals(id, event.userId)
                assertEquals(ADMIN_USER_ID, event.actorUserId)
                assertEquals(expected, event.eventType)
                assertEquals(LocalDateTime.now(clock), event.eventTime)
            }

            @Test
            @DisplayName("repeating the same update leaves the flag as it is and records a single event")
            fun `repeating the same update is idempotent`() {
                accessService.updateState(ACTIVE_USER_ID, ADMIN_USER_ID, UserStateChangeDto(deleted = true))
                accessService.updateState(ACTIVE_USER_ID, ADMIN_USER_ID, UserStateChangeDto(deleted = true))

                assertTrue(accessService.findByIdForAdmin(ACTIVE_USER_ID)!!.deleted)
                assertEquals(2L, accessService.countForAdmin(null).inactive)
                assertEquals(1, userStateEventRepository.findAll().size, "The second call changed nothing")
            }

            @Test
            @DisplayName("an unknown id is reported as not found and changes nothing")
            fun `unknown id changes nothing`() {
                assertThrows<TelegramUserNotFoundException> {
                    accessService.updateState(MISSING_USER_ID, ADMIN_USER_ID, UserStateChangeDto(deleted = true))
                }

                val counts = accessService.countForAdmin(null)
                assertEquals(8L, counts.all)
                assertEquals(1L, counts.inactive)
                assertTrue(userStateEventRepository.findAll().isEmpty())
            }

            @Test
            @DisplayName("an audit record that cannot be written takes the flag change down with it")
            fun `a failed event write rolls the flag back`() {
                assertThrows<DataIntegrityViolationException> {
                    accessService.updateState(ACTIVE_USER_ID, MISSING_USER_ID, UserStateChangeDto(banned = true))
                }

                assertFalse(
                    accessService.findByIdForAdmin(ACTIVE_USER_ID)!!.isBanned,
                    "A state that outlives its audit record is exactly what the single door is for",
                )
                assertTrue(userStateEventRepository.findAll().isEmpty())
            }

            @Test
            @DisplayName("evicts the cached access flags of the user it touches")
            fun `evicts the cached access flags`() {
                val cache = cacheManager.getCache(CacheConfig.USER_ACCESS_FLAGS)!!
                accessService.resolveAccessFlags(ACTIVE_EXTERNAL_ID)
                assertNotNull(cache.get(ACTIVE_EXTERNAL_ID))

                accessService.updateState(ACTIVE_USER_ID, ADMIN_USER_ID, UserStateChangeDto(deleted = true))

                assertNull(cache.get(ACTIVE_EXTERNAL_ID))
            }

            @Test
            @DisplayName("evicts the flags even when every field is null and nothing is written")
            fun `evicts the cached access flags on a no-op update`() {
                val cache = cacheManager.getCache(CacheConfig.USER_ACCESS_FLAGS)!!
                accessService.resolveAccessFlags(ACTIVE_EXTERNAL_ID)
                assertNotNull(cache.get(ACTIVE_EXTERNAL_ID))

                accessService.updateState(ACTIVE_USER_ID, ADMIN_USER_ID, UserStateChangeDto(deleted = null))

                assertNull(cache.get(ACTIVE_EXTERNAL_ID))
            }
        }

        @Nested
        @DisplayName("restoreIfDeleted()")
        inner class RestoreIfDeleted {
            @Test
            @DisplayName("brings a soft-deleted user back and reports that it did")
            fun `restores a soft deleted user`() {
                val restored = accessService.restoreIfDeleted(DELETED_EXTERNAL_ID)

                assertTrue(restored)
                assertFalse(accessService.findByIdForAdmin(DELETED_USER_ID)!!.deleted)
            }

            @Test
            @DisplayName("records the restore with the user as their own actor, no admin behind it")
            fun `records the restore as the deed of the user themselves`() {
                accessService.restoreIfDeleted(DELETED_EXTERNAL_ID)

                val event = userStateEventRepository.findAll().single()
                assertEquals(DELETED_USER_ID, event.userId)
                assertEquals(DELETED_USER_ID, event.actorUserId, "A start is the user's own doing")
                assertEquals(UserStateEventType.RESTORED, event.eventType)
                assertEquals(LocalDateTime.now(clock), event.eventTime)
            }

            @ParameterizedTest(name = "[{index}] externalUserId={0}")
            @CsvSource(
                "1",
                "777003",
                "0",
            )
            @DisplayName("leaves a live or unknown user alone and reports that nothing happened")
            fun `restores nothing when there is nothing to restore`(externalUserId: Long) {
                val restored = accessService.restoreIfDeleted(externalUserId)

                assertFalse(restored)
                assertEquals(1L, accessService.countForAdmin(null).inactive, "No one else may be revived")
                assertTrue(userStateEventRepository.findAll().isEmpty(), "Nothing happened, nothing to record")
            }

            @Test
            @DisplayName("repeating the call is idempotent, only the first one restores")
            fun `repeating the call is idempotent`() {
                assertTrue(accessService.restoreIfDeleted(DELETED_EXTERNAL_ID))

                assertFalse(accessService.restoreIfDeleted(DELETED_EXTERNAL_ID))
                assertFalse(accessService.findByIdForAdmin(DELETED_USER_ID)!!.deleted)
            }

            @Test
            @DisplayName("evicts the cached access flags, so the stale isDeleted is not served on")
            fun `evicts the cached access flags`() {
                val cache = cacheManager.getCache(CacheConfig.USER_ACCESS_FLAGS)!!
                accessService.resolveAccessFlags(DELETED_EXTERNAL_ID)
                assertNotNull(cache.get(DELETED_EXTERNAL_ID))

                accessService.restoreIfDeleted(DELETED_EXTERNAL_ID)

                assertNull(cache.get(DELETED_EXTERNAL_ID))
            }

            @Test
            @DisplayName("keeps the cached flags of a live user, a call that restores nothing evicts nothing")
            fun `keeps the cached access flags when nothing was restored`() {
                val cache = cacheManager.getCache(CacheConfig.USER_ACCESS_FLAGS)!!
                accessService.resolveAccessFlags(ACTIVE_EXTERNAL_ID)
                assertNotNull(cache.get(ACTIVE_EXTERNAL_ID))

                accessService.restoreIfDeleted(ACTIVE_EXTERNAL_ID)

                assertNotNull(cache.get(ACTIVE_EXTERNAL_ID), "A no-op must not cost the cached flags")
            }
        }

        private fun String.toIds(): List<Long> = split(",").filter { it.isNotBlank() }.map { it.trim().toLong() }

        companion object {
            private const val PAGE_SIZE = 20
            private val PAGE = PageRequest.of(0, PAGE_SIZE)

            private const val ADMIN_USER_ID = 1L
            private const val DELETED_USER_ID = 3L
            private const val BANNED_USER_ID = 4L
            private const val ACTIVE_USER_ID = 5L
            private const val BANNED_DELETED_USER_ID = 6L
            private const val MISSING_USER_ID = 999L

            private const val ADMIN_EXTERNAL_ID = 1L
            private const val DELETED_EXTERNAL_ID = 777001L
            private const val ACTIVE_EXTERNAL_ID = 777003L
            private const val MISSING_EXTERNAL_ID = 0L
        }
    }

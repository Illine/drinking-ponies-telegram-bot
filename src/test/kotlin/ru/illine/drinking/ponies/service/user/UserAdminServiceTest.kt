package ru.illine.drinking.ponies.service.user

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageRequest
import ru.illine.drinking.ponies.dao.access.TelegramUserAccessService
import ru.illine.drinking.ponies.exception.InactiveUserPromotionException
import ru.illine.drinking.ponies.exception.LastAdminException
import ru.illine.drinking.ponies.exception.SelfStateChangeException
import ru.illine.drinking.ponies.exception.TelegramUserNotFoundException
import ru.illine.drinking.ponies.model.base.AdminUserStatusFilter
import ru.illine.drinking.ponies.model.dto.internal.AdminUserDto
import ru.illine.drinking.ponies.model.dto.internal.UserAccessDto
import ru.illine.drinking.ponies.model.dto.internal.UserCountsDto
import ru.illine.drinking.ponies.model.dto.internal.UserStateChangeDto
import ru.illine.drinking.ponies.model.dto.internal.UserStateDto
import ru.illine.drinking.ponies.service.user.impl.UserAdminServiceImpl
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.UnitTest
import java.time.LocalDateTime
import java.util.stream.Stream

@UnitTest
@DisplayName("UserAdminService Unit Test")
class UserAdminServiceTest {
    private lateinit var telegramUserAccessService: TelegramUserAccessService
    private lateinit var userAdminService: UserAdminService

    @BeforeEach
    fun setUp() {
        telegramUserAccessService = mock<TelegramUserAccessService>()
        userAdminService = UserAdminServiceImpl(telegramUserAccessService)
    }

    @ParameterizedTest(name = "[{index}] search=[{0}]")
    @NullSource
    @ValueSource(strings = ["", "   "])
    @DisplayName("getUsers(): a blank search reaches the DAO as no search, for the page and the counters alike")
    fun `getUsers normalizes a blank search`(search: String?) {
        stubEmptyPage()

        userAdminService.getUsers(search, AdminUserStatusFilter.BANNED, page = 1, size = 5)

        verify(telegramUserAccessService).findAllForAdmin(null, AdminUserStatusFilter.BANNED, PageRequest.of(1, 5))
        verify(telegramUserAccessService).countForAdmin(null)
    }

    @ParameterizedTest(name = "[{index}] search=[{0}] - pattern [{1}]")
    @CsvSource(
        "alice,    %alice%",
        "' alice ', '% alice %'",
        """alice_p,  '%alice\_p%'""",
        """50%,      '%50\%%'""",
        """'a\b',    '%a\\b%'""",
    )
    @DisplayName("getUsers(): wraps a real search in wildcards and escapes the ones the user typed")
    fun `getUsers wraps and escapes the search`(
        search: String,
        pattern: String,
    ) {
        stubEmptyPage()

        userAdminService.getUsers(search, AdminUserStatusFilter.ALL, page = 0, size = 20)

        verify(telegramUserAccessService).findAllForAdmin(pattern, AdminUserStatusFilter.ALL, PageRequest.of(0, 20))
        verify(telegramUserAccessService).countForAdmin(pattern)
    }

    @ParameterizedTest(name = "[{index}] status={0} - total {1}")
    @CsvSource(
        "ALL,      11",
        "ACTIVE,    8",
        "INACTIVE,  1",
        "BANNED,    2",
    )
    @DisplayName("getUsers(): total is the counter of the requested status, no extra count query")
    fun `getUsers takes total from the matching counter`(
        status: AdminUserStatusFilter,
        expectedTotal: Long,
    ) {
        val counts = UserCountsDto(all = 11L, active = 8L, inactive = 1L, banned = 2L)
        whenever(telegramUserAccessService.findAllForAdmin(anyOrNull(), any(), any())).thenReturn(emptyList())
        whenever(telegramUserAccessService.countForAdmin(anyOrNull())).thenReturn(counts)

        val result = userAdminService.getUsers(null, status, page = 0, size = 20)

        assertEquals(expectedTotal, result.total)
        assertEquals(11L, result.counts.all)
    }

    @Test
    @DisplayName("updateState(): an empty payload is rejected before the DAO is touched at all")
    fun `updateState rejects an empty payload`() {
        assertThrows<IllegalArgumentException> {
            userAdminService.updateState(USER_ID, ACTOR_ID, UserStateDto())
        }

        verifyNoInteractions(telegramUserAccessService)
    }

    @Test
    @DisplayName("updateState(): an unknown user is reported as not found and nothing is written")
    fun `updateState reports an unknown user`() {
        whenever(telegramUserAccessService.findByIdForAdmin(USER_ID)).thenReturn(null)

        assertThrows<TelegramUserNotFoundException> {
            userAdminService.updateState(USER_ID, ACTOR_ID, UserStateDto(isActive = false))
        }

        verify(telegramUserAccessService).findByIdForAdmin(USER_ID)
        verify(telegramUserAccessService, never()).updateState(any(), any(), any())
    }

    @Test
    @DisplayName("updateState(): an admin changing their own state is rejected before the row is even read")
    fun `updateState rejects a self change`() {
        assertThrows<SelfStateChangeException> {
            userAdminService.updateState(USER_ID, USER_ID, UserStateDto(isBanned = true))
        }

        verifyNoInteractions(telegramUserAccessService)
    }

    @ParameterizedTest(name = "[{index}] isActive={0}")
    @CsvSource("true", "false")
    @DisplayName("updateState(): writes the flag, answers with what the DAO applied and reads the row only once")
    fun `updateState answers with the freshly written flag`(isActive: Boolean) {
        whenever(telegramUserAccessService.findByIdForAdmin(USER_ID)).thenReturn(adminUser(deleted = isActive))
        stubApplied(deleted = !isActive)

        val result = userAdminService.updateState(USER_ID, ACTOR_ID, UserStateDto(isActive = isActive))

        assertEquals(!isActive, result.deleted)
        verify(telegramUserAccessService).updateState(
            USER_ID,
            ACTOR_ID,
            UserStateChangeDto(deleted = !isActive),
        )
        verify(telegramUserAccessService, times(1)).findByIdForAdmin(USER_ID)
    }

    @ParameterizedTest(name = "[{index}] isBanned={0}")
    @CsvSource("true", "false")
    @DisplayName("updateState(): writes the ban flag and answers with it, leaving activity alone")
    fun `updateState answers with the freshly written ban flag`(isBanned: Boolean) {
        whenever(telegramUserAccessService.findByIdForAdmin(USER_ID)).thenReturn(adminUser(deleted = true))
        stubApplied(deleted = true, banned = isBanned)

        val result = userAdminService.updateState(USER_ID, ACTOR_ID, UserStateDto(isBanned = isBanned))

        assertEquals(isBanned, result.isBanned)
        assertTrue(result.deleted, "An untouched field keeps its value")
        verify(telegramUserAccessService).updateState(
            USER_ID,
            ACTOR_ID,
            UserStateChangeDto(banned = isBanned),
        )
    }

    @Test
    @DisplayName("updateState(): the rest of the row comes back untouched")
    fun `updateState keeps the rest of the row`() {
        whenever(telegramUserAccessService.findByIdForAdmin(USER_ID)).thenReturn(adminUser(deleted = true))
        stubApplied(deleted = false)

        val result = userAdminService.updateState(USER_ID, ACTOR_ID, UserStateDto(isActive = true))

        assertEquals(adminUser(deleted = false), result)
    }

    @ParameterizedTest(name = "[{index}] isAdmin={0}")
    @CsvSource("true", "false")
    @DisplayName("updateState(): writes the admin flag and answers with it")
    fun `updateState answers with the freshly written admin flag`(isAdmin: Boolean) {
        whenever(telegramUserAccessService.findByIdForAdmin(USER_ID))
            .thenReturn(adminUser(deleted = false, isAdmin = !isAdmin))
        whenever(telegramUserAccessService.findActiveAdminIdsForUpdate()).thenReturn(listOf(ACTOR_ID, USER_ID))
        stubApplied(deleted = false, admin = isAdmin)

        val result = userAdminService.updateState(USER_ID, ACTOR_ID, UserStateDto(isAdmin = isAdmin))

        assertEquals(isAdmin, result.isAdmin)
        verify(telegramUserAccessService).updateState(
            USER_ID,
            ACTOR_ID,
            UserStateChangeDto(admin = isAdmin),
        )
    }

    @ParameterizedTest(name = "[{index}] banned={0}, deleted={1}")
    @CsvSource(
        "true,  false",
        "false, true",
        "true,  true",
    )
    @DisplayName("updateState(): refuses to promote a user who would stay locked out, and writes nothing")
    fun `updateState refuses to promote a locked out user`(
        banned: Boolean,
        deleted: Boolean,
    ) {
        whenever(telegramUserAccessService.findByIdForAdmin(USER_ID))
            .thenReturn(adminUser(deleted = deleted, banned = banned))

        assertThrows<InactiveUserPromotionException> {
            userAdminService.updateState(USER_ID, ACTOR_ID, UserStateDto(isAdmin = true))
        }

        verify(telegramUserAccessService, never()).updateState(any(), any(), any())
    }

    @ParameterizedTest(name = "[{index}] stored banned={0}, deleted={1} - {2}")
    @MethodSource("providePromotionsThatUnlock")
    @DisplayName("updateState(): a promotion that lifts the lockout in the same call goes through")
    fun `updateState promotes a user the same call unlocks`(
        banned: Boolean,
        deleted: Boolean,
        state: UserStateDto,
    ) {
        whenever(telegramUserAccessService.findByIdForAdmin(USER_ID))
            .thenReturn(adminUser(deleted = deleted, banned = banned))
        stubApplied(deleted = false, admin = true)

        val result = userAdminService.updateState(USER_ID, ACTOR_ID, state)

        assertTrue(result.isAdmin)
        verify(telegramUserAccessService).updateState(any(), any(), any())
    }

    @Test
    @DisplayName("updateState(): refuses to revoke the privileges of the only admin left and writes nothing")
    fun `updateState refuses to demote the last admin`() {
        whenever(telegramUserAccessService.findByIdForAdmin(USER_ID))
            .thenReturn(adminUser(deleted = false, isAdmin = true))
        whenever(telegramUserAccessService.findActiveAdminIdsForUpdate()).thenReturn(listOf(USER_ID))

        assertThrows<LastAdminException> {
            userAdminService.updateState(USER_ID, ACTOR_ID, UserStateDto(isAdmin = false))
        }

        verify(telegramUserAccessService, never()).updateState(any(), any(), any())
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("providePayloadsKeepingThePrivileges")
    @DisplayName("updateState(): a payload that does not revoke the privileges locks no admin rows")
    fun `updateState locks nothing outside a demotion`(state: UserStateDto) {
        whenever(telegramUserAccessService.findByIdForAdmin(USER_ID)).thenReturn(adminUser(deleted = false))
        stubApplied(deleted = false)

        userAdminService.updateState(USER_ID, ACTOR_ID, state)

        verify(telegramUserAccessService, never()).findActiveAdminIdsForUpdate()
    }

    @Test
    @DisplayName("updateState(): an admin revoking their own privileges is a self change, nothing is even read")
    fun `updateState rejects a self demotion`() {
        assertThrows<SelfStateChangeException> {
            userAdminService.updateState(USER_ID, USER_ID, UserStateDto(isAdmin = false))
        }

        verifyNoInteractions(telegramUserAccessService)
    }

    @Test
    @DisplayName("updateState(): the only admin left being somebody else does not stand in the way of a demotion")
    fun `updateState demotes a user who is not the last admin`() {
        whenever(telegramUserAccessService.findByIdForAdmin(USER_ID))
            .thenReturn(adminUser(deleted = false, isAdmin = true))
        whenever(telegramUserAccessService.findActiveAdminIdsForUpdate()).thenReturn(listOf(ACTOR_ID))
        stubApplied(deleted = false)

        val result = userAdminService.updateState(USER_ID, ACTOR_ID, UserStateDto(isAdmin = false))

        assertFalse(result.isAdmin)
        verify(telegramUserAccessService).updateState(
            USER_ID,
            ACTOR_ID,
            UserStateChangeDto(admin = false),
        )
    }

    @Test
    @DisplayName("updateState(): an unknown user is reported as not found on a demotion as well")
    fun `updateState reports an unknown user on a demotion`() {
        whenever(telegramUserAccessService.findByIdForAdmin(USER_ID)).thenReturn(null)

        assertThrows<TelegramUserNotFoundException> {
            userAdminService.updateState(USER_ID, ACTOR_ID, UserStateDto(isAdmin = false))
        }

        verify(telegramUserAccessService, never()).findActiveAdminIdsForUpdate()
        verify(telegramUserAccessService, never()).updateState(any(), any(), any())
    }

    @Test
    @DisplayName("updateState(): all three toggles reach the DAO as one change, the activity arriving inverted")
    fun `updateState forwards all three fields at once`() {
        whenever(telegramUserAccessService.findByIdForAdmin(USER_ID))
            .thenReturn(adminUser(deleted = true, banned = true))
        stubApplied(deleted = false, admin = true)

        val result =
            userAdminService.updateState(
                USER_ID,
                ACTOR_ID,
                UserStateDto(isActive = true, isBanned = false, isAdmin = true),
            )

        assertFalse(result.deleted)
        assertFalse(result.isBanned)
        assertTrue(result.isAdmin)
        verify(telegramUserAccessService).updateState(
            USER_ID,
            ACTOR_ID,
            UserStateChangeDto(deleted = false, banned = false, admin = true),
        )
    }

    private fun stubApplied(
        deleted: Boolean,
        banned: Boolean = false,
        admin: Boolean = false,
    ) {
        whenever(telegramUserAccessService.updateState(any(), any(), any()))
            .thenReturn(
                DtoGenerator.generateUserAccessDto(
                    id = USER_ID,
                    externalUserId = EXTERNAL_USER_ID,
                    isAdmin = admin,
                    isBanned = banned,
                    isDeleted = deleted,
                ),
            )
    }

    @Test
    @DisplayName("getUser(): an unknown user is reported as not found")
    fun `getUser reports an unknown user`() {
        whenever(telegramUserAccessService.findByIdForAdmin(USER_ID)).thenReturn(null)

        assertThrows<TelegramUserNotFoundException> {
            userAdminService.getUser(USER_ID)
        }

        verify(telegramUserAccessService).findByIdForAdmin(USER_ID)
    }

    private fun stubEmptyPage() {
        whenever(telegramUserAccessService.findAllForAdmin(anyOrNull(), any(), any())).thenReturn(emptyList())
        whenever(telegramUserAccessService.countForAdmin(anyOrNull())).thenReturn(EMPTY_COUNTS)
    }

    private fun adminUser(
        deleted: Boolean,
        isAdmin: Boolean = false,
        banned: Boolean = false,
    ): AdminUserDto =
        AdminUserDto(
            id = USER_ID,
            externalUserId = EXTERNAL_USER_ID,
            firstName = "Alisa",
            lastName = "Petrova",
            username = "alisaadmin",
            isAdmin = isAdmin,
            isBanned = banned,
            deleted = deleted,
            timeZone = "Europe/Moscow",
            created = LocalDateTime.of(2026, 1, 1, 10, 0),
            lastActivity = null,
        )

    companion object {
        @JvmStatic
        fun providePayloadsKeepingThePrivileges(): Stream<UserStateDto> =
            Stream.of(
                UserStateDto(isAdmin = true),
                UserStateDto(isActive = false, isBanned = true),
            )

        @JvmStatic
        fun providePromotionsThatUnlock(): Stream<Arguments> =
            Stream.of(
                Arguments.of(true, false, UserStateDto(isBanned = false, isAdmin = true)),
                Arguments.of(false, true, UserStateDto(isActive = true, isAdmin = true)),
                Arguments.of(true, true, UserStateDto(isActive = true, isBanned = false, isAdmin = true)),
            )

        private const val USER_ID = 1042L
        private const val EXTERNAL_USER_ID = 482719301L
        private const val ACTOR_ID = 1L

        private val EMPTY_COUNTS = UserCountsDto(all = 0L, active = 0L, inactive = 0L, banned = 0L)
    }
}

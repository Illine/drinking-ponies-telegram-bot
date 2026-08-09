package ru.illine.drinking.ponies.service.user

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
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
import ru.illine.drinking.ponies.exception.SelfStateChangeException
import ru.illine.drinking.ponies.exception.TelegramUserNotFoundException
import ru.illine.drinking.ponies.model.base.AdminUserStatusFilter
import ru.illine.drinking.ponies.model.dto.internal.AdminUserDto
import ru.illine.drinking.ponies.model.dto.internal.UserAccessDto
import ru.illine.drinking.ponies.model.dto.internal.UserCountsDto
import ru.illine.drinking.ponies.model.dto.internal.UserStateChangeDto
import ru.illine.drinking.ponies.model.dto.internal.UserStateDto
import ru.illine.drinking.ponies.service.user.impl.UserAdminServiceImpl
import ru.illine.drinking.ponies.test.tag.UnitTest
import java.time.LocalDateTime

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

    private fun stubApplied(
        deleted: Boolean,
        banned: Boolean = false,
    ) {
        whenever(telegramUserAccessService.updateState(any(), any(), any()))
            .thenReturn(UserAccessDto(USER_ID, EXTERNAL_USER_ID, isBanned = banned, isDeleted = deleted))
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

    private fun adminUser(deleted: Boolean): AdminUserDto =
        AdminUserDto(
            id = USER_ID,
            externalUserId = EXTERNAL_USER_ID,
            firstName = "Alisa",
            lastName = "Petrova",
            username = "alisaadmin",
            isAdmin = false,
            isBanned = false,
            deleted = deleted,
            timeZone = "Europe/Moscow",
            created = LocalDateTime.of(2026, 1, 1, 10, 0),
            lastActivity = null,
        )

    companion object {
        private const val USER_ID = 1042L
        private const val EXTERNAL_USER_ID = 482719301L
        private const val ACTOR_ID = 1L

        private val EMPTY_COUNTS = UserCountsDto(all = 0L, active = 0L, inactive = 0L, banned = 0L)
    }
}

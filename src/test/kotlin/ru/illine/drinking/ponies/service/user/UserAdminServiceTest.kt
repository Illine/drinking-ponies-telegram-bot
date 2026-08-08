package ru.illine.drinking.ponies.service.user

import org.junit.jupiter.api.Assertions.assertEquals
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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageRequest
import ru.illine.drinking.ponies.dao.access.TelegramUserAccessService
import ru.illine.drinking.ponies.dao.repository.AdminUserProjection
import ru.illine.drinking.ponies.dao.repository.UserCountsProjection
import ru.illine.drinking.ponies.exception.TelegramUserNotFoundException
import ru.illine.drinking.ponies.model.base.AdminUserStatusFilter
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
        // The counters cover every chip of the admin page, so the status must not narrow them down.
        verify(telegramUserAccessService).countForAdmin(null)
    }

    @ParameterizedTest(name = "[{index}] search=[{0}] - pattern [{1}]")
    @CsvSource(
        "alice,    %alice%",
        "' alice ', '% alice %'",
        // LIKE wildcards a user may legitimately type - a Telegram username often carries "_".
        """alice_p,  '%alice\_p%'""",
        """50%,      '%50\%%'""",
        // The escape character itself has to be escaped first, or it would swallow the next one.
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
        val counts =
            mock<UserCountsProjection> {
                on { all } doReturn 11L
                on { active } doReturn 8L
                on { inactive } doReturn 1L
                on { banned } doReturn 2L
            }
        whenever(telegramUserAccessService.findAllForAdmin(anyOrNull(), any(), any())).thenReturn(emptyList())
        whenever(telegramUserAccessService.countForAdmin(anyOrNull())).thenReturn(counts)

        val response = userAdminService.getUsers(null, status, page = 0, size = 20)

        assertEquals(expectedTotal, response.total)
        assertEquals(11L, response.counts.all)
    }

    @Test
    @DisplayName("updateState(): an empty payload is rejected before the DAO is touched at all")
    fun `updateState rejects an empty payload`() {
        assertThrows<IllegalArgumentException> {
            userAdminService.updateState(USER_ID, isActive = null)
        }

        verifyNoInteractions(telegramUserAccessService)
    }

    @Test
    @DisplayName("updateState(): an unknown user is reported as not found and nothing is written")
    fun `updateState reports an unknown user`() {
        whenever(telegramUserAccessService.findByIdForAdmin(USER_ID)).thenReturn(null)

        assertThrows<TelegramUserNotFoundException> {
            userAdminService.updateState(USER_ID, isActive = false)
        }

        verify(telegramUserAccessService).findByIdForAdmin(USER_ID)
        verify(telegramUserAccessService, never()).updateDeleted(any(), any(), any())
    }

    @ParameterizedTest(name = "[{index}] isActive={0}")
    @CsvSource("true", "false")
    @DisplayName("updateState(): writes the flag, answers with it and reads the row only once")
    fun `updateState answers with the freshly written flag`(isActive: Boolean) {
        val user =
            mock<AdminUserProjection> {
                on { id } doReturn USER_ID
                on { externalUserId } doReturn EXTERNAL_USER_ID
                // Stale on purpose - the row still holds the state from before the update,
                // so an answer built from it alone would report the opposite of the truth.
                on { deleted } doReturn isActive
                on { timeZone } doReturn "Europe/Moscow"
                on { created } doReturn LocalDateTime.of(2026, 1, 1, 10, 0)
            }
        whenever(telegramUserAccessService.findByIdForAdmin(USER_ID)).thenReturn(user)

        val response = userAdminService.updateState(USER_ID, isActive)

        assertEquals(isActive, response.isActive)
        verify(telegramUserAccessService).updateDeleted(USER_ID, EXTERNAL_USER_ID, !isActive)
        // The card comes from the row already read, so the update must not trigger a second read.
        verify(telegramUserAccessService, times(1)).findByIdForAdmin(USER_ID)
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
        whenever(telegramUserAccessService.countForAdmin(anyOrNull())).thenReturn(mock<UserCountsProjection>())
    }

    companion object {
        private const val USER_ID = 1042L
        private const val EXTERNAL_USER_ID = 482719301L
    }
}

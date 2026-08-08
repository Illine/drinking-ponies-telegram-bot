package ru.illine.drinking.ponies.config.web.interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import ru.illine.drinking.ponies.config.web.security.AuthErrorType
import ru.illine.drinking.ponies.dao.access.TelegramUserAccessService
import ru.illine.drinking.ponies.exception.InvalidAuthSignatureException
import ru.illine.drinking.ponies.model.dto.TelegramUserDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserProfile
import ru.illine.drinking.ponies.model.dto.internal.UserAccessDto
import ru.illine.drinking.ponies.service.telegram.TelegramValidatorService
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.telegram.TelegramGeneralConstants
import java.util.stream.Stream

@UnitTest
@DisplayName("TelegramAuthInterceptor Unit Test")
class TelegramAuthInterceptorTest {
    private val headerName = "X-Authorization-Telegram-Data"

    private lateinit var validatorService: TelegramValidatorService
    private lateinit var telegramUserAccessService: TelegramUserAccessService
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var interceptor: TelegramAuthInterceptor

    @BeforeEach
    fun setUp() {
        validatorService = mock<TelegramValidatorService>()
        telegramUserAccessService = mock<TelegramUserAccessService>()
        request = mock<HttpServletRequest>()
        response = mock<HttpServletResponse>()
        interceptor = TelegramAuthInterceptor(validatorService, telegramUserAccessService)
    }

    @Test
    @DisplayName("preHandle(): OPTIONS request - returns true without validation")
    fun `preHandle OPTIONS request returns true`() {
        whenever(request.method).thenReturn("OPTIONS")

        val result = interceptor.preHandle(request, response, Any())

        assertTrue(result)
        verifyNoInteractions(validatorService)
        verifyNoInteractions(telegramUserAccessService)
    }

    @ParameterizedTest(name = "[{index}] header={0} - returns false, 401, X-Auth-Error-Code invalid_auth_signature")
    @MethodSource("provideMissingOrBlankHeaders")
    @DisplayName(
        "preHandle(): missing/empty/blank header - returns false, 401, X-Auth-Error-Code invalid_auth_signature",
    )
    fun `preHandle missing or blank header returns false with 401 and invalid_auth_signature header`(
        headerValue: String?,
    ) {
        whenever(request.method).thenReturn("POST")
        whenever(request.getHeader(headerName)).thenReturn(headerValue)

        val result = interceptor.preHandle(request, response, Any())

        assertFalse(result)
        verify(response).status = HttpServletResponse.SC_UNAUTHORIZED
        verify(response).setHeader(AuthErrorType.HEADER_NAME, AuthErrorType.INVALID_AUTH_SIGNATURE.value)
        verifyNoInteractions(validatorService)
        verifyNoInteractions(telegramUserAccessService)
    }

    @Test
    @DisplayName(
        "preHandle(): malformed initData (verifySignature throws) - 401, X-Auth-Error-Code invalid_auth_signature",
    )
    fun `preHandle malformed initData returns false with 401 and invalid_auth_signature header`() {
        whenever(request.method).thenReturn("POST")
        whenever(request.getHeader(headerName)).thenReturn("malformed")
        whenever(validatorService.verifySignature(any()))
            .thenThrow(InvalidAuthSignatureException("Failed to decode 'initData'"))

        val result = interceptor.preHandle(request, response, Any())

        assertFalse(result)
        verify(response).status = HttpServletResponse.SC_UNAUTHORIZED
        verify(response).setHeader(AuthErrorType.HEADER_NAME, AuthErrorType.INVALID_AUTH_SIGNATURE.value)
        verifyNoInteractions(telegramUserAccessService)
    }

    @Test
    @DisplayName("preHandle(): unexpected exception during verify - 401, X-Auth-Error-Code unknown")
    fun `preHandle unexpected exception returns false with 401 and unknown header`() {
        whenever(request.method).thenReturn("POST")
        whenever(request.getHeader(headerName)).thenReturn("data")
        whenever(validatorService.verifySignature(any()))
            .thenThrow(RuntimeException("boom"))

        val result = interceptor.preHandle(request, response, Any())

        assertFalse(result)
        verify(response).status = HttpServletResponse.SC_UNAUTHORIZED
        verify(response).setHeader(AuthErrorType.HEADER_NAME, AuthErrorType.UNKNOWN.value)
        verifyNoInteractions(telegramUserAccessService)
    }

    @ParameterizedTest(name = "[{index}] isAdmin={0}, isBanned={1}, isDeleted={2}")
    @CsvSource(
        "true,  false, false",
        "false, true,  false",
        "false, false, true",
        "true,  true,  true",
    )
    @DisplayName("preHandle(): valid signature - enriches the access flags and sets telegramUser attribute")
    fun `preHandle valid signature enriches access flags`(
        isAdmin: Boolean,
        isBanned: Boolean,
        isDeleted: Boolean,
    ) {
        val initData = "valid-init-data"
        val telegramUser = TelegramUserDto(externalUserId = 1L, firstName = "Test", lastName = null, username = null)
        whenever(request.method).thenReturn("POST")
        whenever(request.getHeader(headerName)).thenReturn(initData)
        whenever(validatorService.verifySignature(any())).thenReturn(true)
        whenever(validatorService.map(initData)).thenReturn(telegramUser)
        whenever(telegramUserAccessService.resolveAccessFlags(any(), any()))
            .thenReturn(UserAccessDto(isAdmin = isAdmin, isBanned = isBanned, isDeleted = isDeleted))

        val result = interceptor.preHandle(request, response, Any())

        assertTrue(result)
        verify(request).setAttribute(
            TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE,
            // isActive is the opposite of the isDeleted the access service reports, and both
            // rows of the matrix would fail if that negation were dropped.
            telegramUser.copy(isAdmin = isAdmin, isBanned = isBanned, isActive = !isDeleted),
        )
        verifyNoMoreInteractions(response)
    }

    @ParameterizedTest(name = "[{index}] isDeleted={0} - isActive={1}")
    @CsvSource("true, false", "false, true")
    @DisplayName("preHandle(): valid signature - a deleted account is reported as inactive, not the other way round")
    fun `preHandle inverts isDeleted into isActive`(
        isDeleted: Boolean,
        expectedIsActive: Boolean,
    ) {
        // Spelled out on its own because the sign is easy to lose: the access service speaks in
        // isDeleted, the request attribute speaks in isActive, and equality on the whole DTO
        // makes for a poor failure message when only that one flag is wrong.
        val initData = "valid-init-data"
        val telegramUser = TelegramUserDto(externalUserId = 1L, firstName = "Test", lastName = null, username = null)
        whenever(request.method).thenReturn("POST")
        whenever(request.getHeader(headerName)).thenReturn(initData)
        whenever(validatorService.verifySignature(any())).thenReturn(true)
        whenever(validatorService.map(initData)).thenReturn(telegramUser)
        whenever(telegramUserAccessService.resolveAccessFlags(any(), any()))
            .thenReturn(UserAccessDto(isDeleted = isDeleted))

        interceptor.preHandle(request, response, Any())

        val captor = argumentCaptor<TelegramUserDto>()
        verify(request).setAttribute(eq(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE), captor.capture())
        assertEquals(expectedIsActive, captor.firstValue.isActive)
    }

    @Test
    @DisplayName("preHandle(): valid signature - forwards the initData profile to the access service")
    fun `preHandle valid signature forwards initData profile`() {
        val initData = "valid-init-data"
        val telegramUser =
            TelegramUserDto(
                externalUserId = 42L,
                firstName = "Alisa",
                lastName = "Petrova",
                username = "alisaadmin",
            )
        whenever(request.method).thenReturn("POST")
        whenever(request.getHeader(headerName)).thenReturn(initData)
        whenever(validatorService.verifySignature(any())).thenReturn(true)
        whenever(validatorService.map(initData)).thenReturn(telegramUser)
        whenever(telegramUserAccessService.resolveAccessFlags(any(), any())).thenReturn(UserAccessDto())

        interceptor.preHandle(request, response, Any())

        verify(telegramUserAccessService).resolveAccessFlags(
            eq(42L),
            eq(
                TelegramUserProfile(
                    firstName = "Alisa",
                    lastName = "Petrova",
                    username = "alisaadmin",
                ),
            ),
        )
    }

    @Test
    @DisplayName(
        "preHandle(): expired auth_date (verifySignature returns false) - 403, X-Auth-Error-Code session_expired",
    )
    fun `preHandle expired auth_date returns false with 403 and session_expired header`() {
        whenever(request.method).thenReturn("POST")
        whenever(request.getHeader(headerName)).thenReturn("expired-data")
        whenever(validatorService.verifySignature(any())).thenReturn(false)

        val result = interceptor.preHandle(request, response, Any())

        assertFalse(result)
        verify(response).status = HttpServletResponse.SC_FORBIDDEN
        verify(response).setHeader(AuthErrorType.HEADER_NAME, AuthErrorType.SESSION_EXPIRED.value)
        verifyNoInteractions(telegramUserAccessService)
    }

    companion object {
        @JvmStatic
        fun provideMissingOrBlankHeaders(): Stream<Arguments> =
            Stream.of(
                Arguments.of(null as String?), // header absent
                Arguments.of(""), // empty
                Arguments.of("   "), // whitespace only
            )
    }
}

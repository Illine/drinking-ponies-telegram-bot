package ru.illine.drinking.ponies.config.web.interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import ru.illine.drinking.ponies.config.web.security.AuthErrorType
import ru.illine.drinking.ponies.dao.access.TelegramUserAccessService
import ru.illine.drinking.ponies.exception.InvalidAuthSignatureException
import ru.illine.drinking.ponies.model.dto.internal.TelegramAuthUserDto
import ru.illine.drinking.ponies.model.dto.internal.UserAccessDto
import ru.illine.drinking.ponies.service.telegram.TelegramValidatorService
import ru.illine.drinking.ponies.test.generator.DtoGenerator
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

    @ParameterizedTest(name = "[{index}] isAdmin={0}")
    @CsvSource("true", "false")
    @DisplayName("preHandle(): valid signature - enriches the admin flag and sets telegramUser attribute")
    fun `preHandle valid signature enriches access flags`(isAdmin: Boolean) {
        val initData =
            stubValidRequest(
                user = DtoGenerator.generateTelegramAuthUserDto(lastName = "Petrova"),
                access = DtoGenerator.generateUserAccessDto(isAdmin = isAdmin),
            )

        val result = interceptor.preHandle(request, response, Any())

        assertTrue(result)
        verify(validatorService).map(initData)
        verify(request).setAttribute(
            TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE,
            DtoGenerator.generateTelegramAuthUserDto(lastName = "Petrova", isAdmin = isAdmin),
        )
        verifyNoMoreInteractions(response)
    }

    @Test
    @DisplayName("preHandle(): the admin flag comes from the access service, never from initData")
    fun `preHandle overrides the access flags carried by initData`() {
        stubValidRequest(
            user = DtoGenerator.generateTelegramAuthUserDto(isAdmin = true),
            access = DtoGenerator.generateUserAccessDto(isAdmin = false),
        )

        interceptor.preHandle(request, response, Any())

        verify(request).setAttribute(
            TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE,
            DtoGenerator.generateTelegramAuthUserDto(isAdmin = false),
        )
    }

    @ParameterizedTest(name = "[{index}] isBanned={0}, isDeleted={1} - {2}")
    @CsvSource(
        "true,  false, BANNED",
        "false, true,  DELETED",
        "true,  true,  BANNED",
    )
    @DisplayName("preHandle(): a banned or deleted caller - returns false, 403 and the code the mini app reads")
    fun `preHandle rejects a banned or deleted caller`(
        isBanned: Boolean,
        isDeleted: Boolean,
        expected: AuthErrorType,
    ) {
        stubValidRequest(
            user = DtoGenerator.generateTelegramAuthUserDto(),
            access = DtoGenerator.generateUserAccessDto(isBanned = isBanned, isDeleted = isDeleted),
        )

        val result = interceptor.preHandle(request, response, Any())

        assertFalse(result)
        verify(response).status = HttpServletResponse.SC_FORBIDDEN
        verify(response).setHeader(AuthErrorType.HEADER_NAME, expected.value)
        verify(request, never()).setAttribute(eq(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE), any())
    }

    @Test
    @DisplayName("preHandle(): the internal id of the stored account rides along, the admin endpoints need it")
    fun `preHandle carries the internal id into the request attribute`() {
        stubValidRequest(
            user = DtoGenerator.generateTelegramAuthUserDto(externalUserId = 42L),
            access = DtoGenerator.generateUserAccessDto(id = 1042L, externalUserId = 42L),
        )

        interceptor.preHandle(request, response, Any())

        verify(request).setAttribute(
            TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE,
            DtoGenerator.generateTelegramAuthUserDto(id = 1042L, externalUserId = 42L),
        )
    }

    @Test
    @DisplayName("preHandle(): a caller with no stored account keeps a null internal id")
    fun `preHandle leaves the internal id null for an unknown caller`() {
        stubValidRequest(
            user = DtoGenerator.generateTelegramAuthUserDto(externalUserId = 42L),
            access = DtoGenerator.generateUserAccessDto(externalUserId = 42L),
        )

        assertTrue(interceptor.preHandle(request, response, Any()))

        verify(request).setAttribute(
            TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE,
            DtoGenerator.generateTelegramAuthUserDto(externalUserId = 42L),
        )
    }

    @Test
    @DisplayName("preHandle(): valid signature - forwards the initData profile to the access service")
    fun `preHandle valid signature forwards initData profile`() {
        stubValidRequest(
            user =
                DtoGenerator.generateTelegramAuthUserDto(
                    externalUserId = 42L,
                    firstName = "Alisa",
                    lastName = "Petrova",
                    username = "alisaadmin",
                ),
        )

        interceptor.preHandle(request, response, Any())

        verify(telegramUserAccessService).syncProfile(
            eq(42L),
            eq(
                DtoGenerator.generateTelegramUserProfileDto(
                    firstName = "Alisa",
                    lastName = "Petrova",
                    username = "alisaadmin",
                ),
            ),
        )
    }

    @ParameterizedTest(name = "[{index}] isBanned={0}, isDeleted={1}")
    @CsvSource("true, false", "false, true")
    @DisplayName("preHandle(): a rejected caller does not get their profile refreshed either")
    fun `preHandle skips the profile sync of a rejected caller`(
        isBanned: Boolean,
        isDeleted: Boolean,
    ) {
        stubValidRequest(
            user = DtoGenerator.generateTelegramAuthUserDto(firstName = "Alisa"),
            access = DtoGenerator.generateUserAccessDto(isBanned = isBanned, isDeleted = isDeleted),
        )

        interceptor.preHandle(request, response, Any())

        verify(telegramUserAccessService, never()).syncProfile(any(), any())
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

    private fun stubValidRequest(
        user: TelegramAuthUserDto,
        access: UserAccessDto = DtoGenerator.generateUserAccessDto(),
    ): String {
        val initData = "valid-init-data"

        whenever(request.method).thenReturn("POST")
        whenever(request.getHeader(headerName)).thenReturn(initData)
        whenever(validatorService.verifySignature(any())).thenReturn(true)
        whenever(validatorService.map(initData)).thenReturn(user)
        whenever(telegramUserAccessService.resolveAccessFlags(any())).thenReturn(access)

        return initData
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

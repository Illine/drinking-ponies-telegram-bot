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
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import java.util.stream.Stream
import ru.illine.drinking.ponies.config.web.security.AuthErrorType
import ru.illine.drinking.ponies.dao.access.TelegramUserAccessService
import ru.illine.drinking.ponies.exception.InvalidAuthSignatureException
import ru.illine.drinking.ponies.model.dto.TelegramUserDto
import ru.illine.drinking.ponies.service.telegram.TelegramValidatorService
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.telegram.TelegramGeneralConstants

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
        validatorService = mock(TelegramValidatorService::class.java)
        telegramUserAccessService = mock(TelegramUserAccessService::class.java)
        request = mock(HttpServletRequest::class.java)
        response = mock(HttpServletResponse::class.java)
        interceptor = TelegramAuthInterceptor(validatorService, telegramUserAccessService)
    }

    @Test
    @DisplayName("preHandle(): OPTIONS request - returns true without validation")
    fun `preHandle OPTIONS request returns true`() {
        `when`(request.method).thenReturn("OPTIONS")

        val result = interceptor.preHandle(request, response, Any())

        assertTrue(result)
        verifyNoInteractions(validatorService)
        verifyNoInteractions(telegramUserAccessService)
    }

    @ParameterizedTest(name = "[{index}] header={0} - returns false, 401, X-Auth-Error-Code invalid_auth_signature")
    @MethodSource("provideMissingOrBlankHeaders")
    @DisplayName("preHandle(): missing/empty/blank header - returns false, 401, X-Auth-Error-Code invalid_auth_signature")
    fun `preHandle missing or blank header returns false with 401 and invalid_auth_signature header`(
        headerValue: String?,
    ) {
        `when`(request.method).thenReturn("POST")
        `when`(request.getHeader(headerName)).thenReturn(headerValue)

        val result = interceptor.preHandle(request, response, Any())

        assertFalse(result)
        verify(response).status = HttpServletResponse.SC_UNAUTHORIZED
        verify(response).setHeader(AuthErrorType.HEADER_NAME, AuthErrorType.INVALID_AUTH_SIGNATURE.value)
        verifyNoInteractions(validatorService)
        verifyNoInteractions(telegramUserAccessService)
    }

    @Test
    @DisplayName("preHandle(): malformed initData (verifySignature throws) - 401, X-Auth-Error-Code invalid_auth_signature")
    fun `preHandle malformed initData returns false with 401 and invalid_auth_signature header`() {
        `when`(request.method).thenReturn("POST")
        `when`(request.getHeader(headerName)).thenReturn("malformed")
        `when`(validatorService.verifySignature(any()))
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
        `when`(request.method).thenReturn("POST")
        `when`(request.getHeader(headerName)).thenReturn("data")
        `when`(validatorService.verifySignature(any()))
            .thenThrow(RuntimeException("boom"))

        val result = interceptor.preHandle(request, response, Any())

        assertFalse(result)
        verify(response).status = HttpServletResponse.SC_UNAUTHORIZED
        verify(response).setHeader(AuthErrorType.HEADER_NAME, AuthErrorType.UNKNOWN.value)
        verifyNoInteractions(telegramUserAccessService)
    }

    @Test
    @DisplayName("preHandle(): valid signature - enriches isAdmin=true and sets telegramUser attribute")
    fun `preHandle valid signature enriches isAdmin true`() {
        val initData = "valid-init-data"
        val telegramUser = TelegramUserDto(telegramId = 1L, firstName = "Test", lastName = null, username = null)
        `when`(request.method).thenReturn("POST")
        `when`(request.getHeader(headerName)).thenReturn(initData)
        `when`(validatorService.verifySignature(any())).thenReturn(true)
        `when`(validatorService.map(initData)).thenReturn(telegramUser)
        `when`(telegramUserAccessService.findIsAdminByExternalUserId(1L)).thenReturn(true)

        val result = interceptor.preHandle(request, response, Any())

        assertTrue(result)
        verify(request).setAttribute(
            TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE,
            telegramUser.copy(isAdmin = true)
        )
        verifyNoMoreInteractions(response)
    }

    @Test
    @DisplayName("preHandle(): valid signature - enriches isAdmin=false when access service returns false")
    fun `preHandle valid signature enriches isAdmin false`() {
        val initData = "valid-init-data"
        val telegramUser = TelegramUserDto(telegramId = 2L, firstName = "Test", lastName = null, username = null)
        `when`(request.method).thenReturn("POST")
        `when`(request.getHeader(headerName)).thenReturn(initData)
        `when`(validatorService.verifySignature(any())).thenReturn(true)
        `when`(validatorService.map(initData)).thenReturn(telegramUser)
        `when`(telegramUserAccessService.findIsAdminByExternalUserId(2L)).thenReturn(false)

        val result = interceptor.preHandle(request, response, Any())

        assertTrue(result)
        verify(request).setAttribute(
            TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE,
            telegramUser.copy(isAdmin = false)
        )
        verifyNoMoreInteractions(response)
    }

    @Test
    @DisplayName("preHandle(): expired auth_date (verifySignature returns false) - 403, X-Auth-Error-Code session_expired")
    fun `preHandle expired auth_date returns false with 403 and session_expired header`() {
        `when`(request.method).thenReturn("POST")
        `when`(request.getHeader(headerName)).thenReturn("expired-data")
        `when`(validatorService.verifySignature(any())).thenReturn(false)

        val result = interceptor.preHandle(request, response, Any())

        assertFalse(result)
        verify(response).status = HttpServletResponse.SC_FORBIDDEN
        verify(response).setHeader(AuthErrorType.HEADER_NAME, AuthErrorType.SESSION_EXPIRED.value)
        verifyNoInteractions(telegramUserAccessService)
    }

    companion object {

        @JvmStatic
        fun provideMissingOrBlankHeaders(): Stream<Arguments> = Stream.of(
            Arguments.of(null as String?), // header absent
            Arguments.of(""),              // empty
            Arguments.of("   "),           // whitespace only
        )
    }
}

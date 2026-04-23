package ru.illine.drinking.ponies.config.web.interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import ru.illine.drinking.ponies.model.dto.TelegramUserDto
import ru.illine.drinking.ponies.service.telegram.TelegramValidatorService
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.telegram.TelegramGeneralConstants

@UnitTest
@DisplayName("TelegramAuthInterceptor Unit Test")
class TelegramAuthInterceptorTest {

    private val headerName = "X-Authorization-Telegram-Data"

    private lateinit var validatorService: TelegramValidatorService
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var interceptor: TelegramAuthInterceptor

    @BeforeEach
    fun setUp() {
        validatorService = mock(TelegramValidatorService::class.java)
        request = mock(HttpServletRequest::class.java)
        response = mock(HttpServletResponse::class.java)
        interceptor = TelegramAuthInterceptor(validatorService)
    }

    @Test
    @DisplayName("preHandle(): OPTIONS request - returns true without validation")
    fun `preHandle OPTIONS request returns true`() {
        `when`(request.method).thenReturn("OPTIONS")

        val result = interceptor.preHandle(request, response, Any())

        assertTrue(result)
        verifyNoInteractions(validatorService)
    }

    @Test
    @DisplayName("preHandle(): missing header - returns false and sets 401")
    fun `preHandle missing header returns false with 401`() {
        `when`(request.method).thenReturn("POST")
        `when`(request.getHeader(headerName)).thenReturn(null)

        val result = interceptor.preHandle(request, response, Any())

        assertFalse(result)
        verify(response).status = HttpServletResponse.SC_UNAUTHORIZED
        verifyNoInteractions(validatorService)
    }

    @Test
    @DisplayName("preHandle(): empty header - returns false and sets 401")
    fun `preHandle empty header returns false with 401`() {
        `when`(request.method).thenReturn("GET")
        `when`(request.getHeader(headerName)).thenReturn("")

        val result = interceptor.preHandle(request, response, Any())

        assertFalse(result)
        verify(response).status = HttpServletResponse.SC_UNAUTHORIZED
        verifyNoInteractions(validatorService)
    }

    @Test
    @DisplayName("preHandle(): blank header - returns false and sets 401")
    fun `preHandle blank header returns false with 401`() {
        `when`(request.method).thenReturn("GET")
        `when`(request.getHeader(headerName)).thenReturn("   ")

        val result = interceptor.preHandle(request, response, Any())

        assertFalse(result)
        verify(response).status = HttpServletResponse.SC_UNAUTHORIZED
        verifyNoInteractions(validatorService)
    }

    @Test
    @DisplayName("preHandle(): valid signature - returns true and sets telegramUser attribute")
    fun `preHandle valid signature returns true`() {
        val initData = "valid-init-data"
        val telegramUser = TelegramUserDto(telegramId = 1L, firstName = "Test", lastName = null, username = null)
        `when`(request.method).thenReturn("POST")
        `when`(request.getHeader(headerName)).thenReturn(initData)
        `when`(validatorService.verifySignature(any(), any())).thenReturn(true)
        `when`(validatorService.map(initData)).thenReturn(telegramUser)

        val result = interceptor.preHandle(request, response, Any())

        assertTrue(result)
        verify(request).setAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE, telegramUser)
        verifyNoMoreInteractions(response)
    }

    @Test
    @DisplayName("preHandle(): invalid signature - returns false and sets 403")
    fun `preHandle invalid signature returns false with 403`() {
        `when`(request.method).thenReturn("POST")
        `when`(request.getHeader(headerName)).thenReturn("invalid-data")
        `when`(validatorService.verifySignature(any(), any())).thenReturn(false)

        val result = interceptor.preHandle(request, response, Any())

        assertFalse(result)
        verify(response).status = HttpServletResponse.SC_FORBIDDEN
    }
}

package ru.illine.drinking.ponies.config.web.interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import org.springframework.web.method.HandlerMethod
import ru.illine.drinking.ponies.config.web.security.AdminOnly
import ru.illine.drinking.ponies.config.web.security.AuthErrorType
import ru.illine.drinking.ponies.model.dto.TelegramUserDto
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.telegram.TelegramGeneralConstants

@UnitTest
@DisplayName("AdminAuthInterceptor Unit Test")
class AdminAuthInterceptorTest {

    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var handlerMethod: HandlerMethod
    private lateinit var interceptor: AdminAuthInterceptor

    private val adminUser = TelegramUserDto(
        telegramId = 1L,
        firstName = "Admin",
        lastName = null,
        username = null,
        isAdmin = true
    )

    private val nonAdminUser = TelegramUserDto(
        telegramId = 2L,
        firstName = "User",
        lastName = null,
        username = null,
        isAdmin = false
    )

    @BeforeEach
    fun setUp() {
        request = mock(HttpServletRequest::class.java)
        response = mock(HttpServletResponse::class.java)
        handlerMethod = mock(HandlerMethod::class.java)
        interceptor = AdminAuthInterceptor()
    }

    @Test
    @DisplayName("preHandle(): non-HandlerMethod (e.g. resource) - returns true")
    fun `non-handler-method returns true`() {
        val result = interceptor.preHandle(request, response, Any())

        assertTrue(result)
        verifyNoInteractions(request)
        verifyNoInteractions(response)
    }

    @Test
    @DisplayName("preHandle(): handler without @AdminOnly - returns true")
    fun `handler without AdminOnly returns true`() {
        `when`(handlerMethod.getMethodAnnotation(AdminOnly::class.java)).thenReturn(null)

        val result = interceptor.preHandle(request, response, handlerMethod)

        assertTrue(result)
        verifyNoInteractions(response)
    }

    @Test
    @DisplayName("preHandle(): @AdminOnly + admin user - returns true")
    fun `admin user returns true`() {
        `when`(handlerMethod.getMethodAnnotation(AdminOnly::class.java)).thenReturn(AdminOnly())
        `when`(request.getAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE)).thenReturn(adminUser)

        val result = interceptor.preHandle(request, response, handlerMethod)

        assertTrue(result)
        verifyNoInteractions(response)
    }

    @Test
    @DisplayName("preHandle(): @AdminOnly + non-admin user - returns false, 403, X-Auth-Error-Code forbidden_admin")
    fun `non-admin user returns false with 403 and forbidden_admin header`() {
        `when`(handlerMethod.getMethodAnnotation(AdminOnly::class.java)).thenReturn(AdminOnly())
        `when`(request.getAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE)).thenReturn(nonAdminUser)

        val result = interceptor.preHandle(request, response, handlerMethod)

        assertFalse(result)
        verify(response).status = HttpServletResponse.SC_FORBIDDEN
        verify(response).setHeader(AuthErrorType.HEADER_NAME, AuthErrorType.FORBIDDEN_ADMIN.value)
    }

    @Test
    @DisplayName("preHandle(): @AdminOnly + missing telegramUser attribute - throws IllegalStateException")
    fun `missing telegramUser attribute fails fast`() {
        `when`(handlerMethod.getMethodAnnotation(AdminOnly::class.java)).thenReturn(AdminOnly())
        `when`(request.getAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE)).thenReturn(null)

        assertThrows<IllegalStateException> {
            interceptor.preHandle(request, response, handlerMethod)
        }
    }
}

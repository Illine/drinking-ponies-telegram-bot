package ru.illine.drinking.ponies.config.web.interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.web.method.HandlerMethod
import ru.illine.drinking.ponies.config.web.security.AdminOnly
import ru.illine.drinking.ponies.config.web.security.AuthErrorType
import ru.illine.drinking.ponies.model.dto.internal.TelegramAuthUserDto
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.telegram.TelegramGeneralConstants

@UnitTest
@DisplayName("AdminAuthInterceptor Unit Test")
class AdminAuthInterceptorTest {
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var handlerMethod: HandlerMethod
    private lateinit var interceptor: AdminAuthInterceptor

    private val adminUser =
        TelegramAuthUserDto(
            externalUserId = 1L,
            firstName = "Admin",
            lastName = null,
            username = null,
            isAdmin = true,
        )

    private val nonAdminUser =
        TelegramAuthUserDto(
            externalUserId = 2L,
            firstName = "User",
            lastName = null,
            username = null,
            isAdmin = false,
        )

    @BeforeEach
    fun setUp() {
        request = mock<HttpServletRequest>()
        response = mock<HttpServletResponse>()
        handlerMethod = mock<HandlerMethod>()
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
    @DisplayName("preHandle(): handler with @AdminOnly neither on the method nor on the class - returns true")
    fun `handler without AdminOnly returns true`() {
        whenever(handlerMethod.getMethodAnnotation(AdminOnly::class.java)).thenReturn(null)
        doReturn(OpenController::class.java).whenever(handlerMethod).beanType

        val result = interceptor.preHandle(request, response, handlerMethod)

        assertTrue(result)
        verifyNoInteractions(response)
    }

    @Test
    @DisplayName("preHandle(): @AdminOnly on the controller class + admin user - returns true")
    fun `class level AdminOnly lets an admin through`() {
        whenever(handlerMethod.getMethodAnnotation(AdminOnly::class.java)).thenReturn(null)
        doReturn(GuardedController::class.java).whenever(handlerMethod).beanType
        whenever(request.getAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE)).thenReturn(adminUser)

        val result = interceptor.preHandle(request, response, handlerMethod)

        assertTrue(result)
        verifyNoInteractions(response)
    }

    @Test
    @DisplayName(
        "preHandle(): @AdminOnly on the controller class + non-admin - returns false, 403, forbidden_admin",
    )
    fun `class level AdminOnly rejects a non admin`() {
        whenever(handlerMethod.getMethodAnnotation(AdminOnly::class.java)).thenReturn(null)
        doReturn(GuardedController::class.java).whenever(handlerMethod).beanType
        whenever(request.getAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE)).thenReturn(nonAdminUser)

        val result = interceptor.preHandle(request, response, handlerMethod)

        assertFalse(result)
        verify(response).status = HttpServletResponse.SC_FORBIDDEN
        verify(response).setHeader(AuthErrorType.HEADER_NAME, AuthErrorType.FORBIDDEN_ADMIN.value)
    }

    @Test
    @DisplayName("preHandle(): @AdminOnly + admin user - returns true")
    fun `admin user returns true`() {
        whenever(handlerMethod.getMethodAnnotation(AdminOnly::class.java)).thenReturn(AdminOnly())
        whenever(request.getAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE)).thenReturn(adminUser)

        val result = interceptor.preHandle(request, response, handlerMethod)

        assertTrue(result)
        verifyNoInteractions(response)
    }

    @Test
    @DisplayName("preHandle(): @AdminOnly + non-admin user - returns false, 403, X-Auth-Error-Code forbidden_admin")
    fun `non-admin user returns false with 403 and forbidden_admin header`() {
        whenever(handlerMethod.getMethodAnnotation(AdminOnly::class.java)).thenReturn(AdminOnly())
        whenever(request.getAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE)).thenReturn(nonAdminUser)

        val result = interceptor.preHandle(request, response, handlerMethod)

        assertFalse(result)
        verify(response).status = HttpServletResponse.SC_FORBIDDEN
        verify(response).setHeader(AuthErrorType.HEADER_NAME, AuthErrorType.FORBIDDEN_ADMIN.value)
    }

    @Test
    @DisplayName("preHandle(): @AdminOnly + missing telegramUser attribute - throws IllegalStateException")
    fun `missing telegramUser attribute fails fast`() {
        whenever(handlerMethod.getMethodAnnotation(AdminOnly::class.java)).thenReturn(AdminOnly())
        whenever(request.getAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE)).thenReturn(null)

        assertThrows<IllegalStateException> {
            interceptor.preHandle(request, response, handlerMethod)
        }
    }

    @AdminOnly
    private class GuardedController

    private class OpenController
}

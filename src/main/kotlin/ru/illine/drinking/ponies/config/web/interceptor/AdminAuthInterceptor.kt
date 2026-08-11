package ru.illine.drinking.ponies.config.web.interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import ru.illine.drinking.ponies.config.web.security.AdminOnly
import ru.illine.drinking.ponies.config.web.security.AuthErrorType
import ru.illine.drinking.ponies.model.base.AppLogger
import ru.illine.drinking.ponies.model.dto.internal.TelegramAuthUserDto
import ru.illine.drinking.ponies.util.telegram.TelegramGeneralConstants

@Component
class AdminAuthInterceptor : HandlerInterceptor {
    private val logger = AppLogger.INTERCEPTOR.logger

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (handler !is HandlerMethod) return true
        handler.getMethodAnnotation(AdminOnly::class.java)
            ?: handler.beanType.getAnnotation(AdminOnly::class.java)
            ?: return true

        val telegramUser =
            request.getAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE)
                as? TelegramAuthUserDto ?: error("@AdminOnly used on endpoint without TelegramAuthInterceptor")

        if (telegramUser.isAdmin) {
            return true
        }

        logger.warn("Forbidden admin access for externalUserId [{}]", telegramUser.externalUserId)
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.setHeader(AuthErrorType.HEADER_NAME, AuthErrorType.FORBIDDEN_ADMIN.value)
        return false
    }
}

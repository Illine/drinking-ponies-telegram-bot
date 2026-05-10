package ru.illine.drinking.ponies.config.web.interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import ru.illine.drinking.ponies.config.web.security.AuthErrorType
import ru.illine.drinking.ponies.dao.access.TelegramUserAccessService
import ru.illine.drinking.ponies.exception.InvalidAuthSignatureException
import ru.illine.drinking.ponies.service.telegram.TelegramValidatorService
import ru.illine.drinking.ponies.util.telegram.TelegramGeneralConstants

@Component
class TelegramAuthInterceptor(
    private val telegramValidatorService: TelegramValidatorService,
    private val telegramUserAccessService: TelegramUserAccessService,
) : HandlerInterceptor {

    private val logger = LoggerFactory.getLogger("INTERCEPTOR")

    private val defaultHeaderName = "X-Authorization-Telegram-Data"


    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (request.method == HttpMethod.OPTIONS.name()) {
            return true
        }

        val initData = request.getHeader(defaultHeaderName)
        if (initData.isNullOrBlank()) {
            logger.error("Not found required header '$defaultHeaderName', return false")
            rejectResponse(response, HttpServletResponse.SC_UNAUTHORIZED, AuthErrorType.INVALID_AUTH_SIGNATURE)
            return false
        }

        val validSignature = try {
            telegramValidatorService.verifySignature(initData)
        } catch (e: InvalidAuthSignatureException) {
            logger.warn("Invalid signature: ${e.message}")
            rejectResponse(response, HttpServletResponse.SC_UNAUTHORIZED, AuthErrorType.INVALID_AUTH_SIGNATURE)
            return false
        } catch (e: Exception) {
            logger.error("Unexpected error", e)
            rejectResponse(response, HttpServletResponse.SC_UNAUTHORIZED)
            return false
        }

        if (validSignature) {
            val telegramUser = telegramValidatorService.map(initData)
            val isAdmin = telegramUserAccessService.findIsAdminByExternalUserId(telegramUser.telegramId)
            val enriched = telegramUser.copy(isAdmin = isAdmin)
            request.setAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE, enriched)
            return true
        }

        rejectResponse(response, HttpServletResponse.SC_FORBIDDEN, AuthErrorType.SESSION_EXPIRED)
        return false
    }

    private fun rejectResponse(
        response: HttpServletResponse, status: Int, errorCode: AuthErrorType = AuthErrorType.UNKNOWN
    ) {
        response.status = status
        response.setHeader(AuthErrorType.HEADER_NAME, errorCode.value)
    }
}

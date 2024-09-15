package ru.illine.drinking.ponies.config.web.interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import ru.illine.drinking.ponies.service.TelegramValidatorService
import ru.illine.drinking.ponies.util.TelegramConstants

@Component
class TelegramAuthInterceptor(
    private val telegramValidatorService: TelegramValidatorService
) : HandlerInterceptor {

    private val logger = LoggerFactory.getLogger("INTERCEPTOR")

    private val defaultHeaderName = "X-Authorization-Telegram-Data"


    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (request.method == HttpMethod.OPTIONS.name()) {
            return true
        }

        val initData = request.getHeader(defaultHeaderName)
        if (initData == null) {
            logger.error("Not found required header '$defaultHeaderName', return false")
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            return false
        }

        if (telegramValidatorService.verifySignature(initData)) {
            val telegramUser = telegramValidatorService.map(initData)
            request.setAttribute(TelegramConstants.TELEGRAM_USER_ATTRIBUTE, telegramUser)
            return true
        }

        response.status = HttpServletResponse.SC_FORBIDDEN
        return false
    }
}
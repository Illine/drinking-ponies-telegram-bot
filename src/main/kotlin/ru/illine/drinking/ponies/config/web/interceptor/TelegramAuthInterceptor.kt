package ru.illine.drinking.ponies.config.web.interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import ru.illine.drinking.ponies.config.web.security.AuthErrorType
import ru.illine.drinking.ponies.dao.access.TelegramUserAccessService
import ru.illine.drinking.ponies.exception.InvalidAuthSignatureException
import ru.illine.drinking.ponies.model.base.AppLogger
import ru.illine.drinking.ponies.model.dto.internal.TelegramAuthUserDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserProfileDto
import ru.illine.drinking.ponies.model.dto.internal.UserAccessDto
import ru.illine.drinking.ponies.service.telegram.TelegramValidatorService
import ru.illine.drinking.ponies.util.telegram.TelegramGeneralConstants

@Component
class TelegramAuthInterceptor(
    private val telegramValidatorService: TelegramValidatorService,
    private val telegramUserAccessService: TelegramUserAccessService,
) : HandlerInterceptor {
    private val logger = AppLogger.INTERCEPTOR.logger

    private val defaultHeaderName = "X-Authorization-Telegram-Data"

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (request.method == HttpMethod.OPTIONS.name()) {
            return true
        }

        val initData = readInitData(request, response) ?: return false
        val telegramUser = authenticate(initData, response) ?: return false
        val access = telegramUserAccessService.resolveAccessFlags(telegramUser.externalUserId)
        if (!admit(access, response)) {
            return false
        }

        syncProfile(telegramUser)
        request.setAttribute(
            TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE,
            telegramUser.copy(id = access.id, isAdmin = access.isAdmin),
        )

        return true
    }

    private fun readInitData(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): String? {
        val initData = request.getHeader(defaultHeaderName)
        if (initData.isNullOrBlank()) {
            logger.error("Not found required header '$defaultHeaderName', return false")
            rejectResponse(response, HttpServletResponse.SC_UNAUTHORIZED, AuthErrorType.INVALID_AUTH_SIGNATURE)
            return null
        }

        return initData
    }

    private fun authenticate(
        initData: String,
        response: HttpServletResponse,
    ): TelegramAuthUserDto? {
        val rejection = verifySignature(initData)
        if (rejection != null) {
            rejectResponse(response, rejection.status, rejection.errorCode)
            return null
        }

        return telegramValidatorService.map(initData)
    }

    private fun verifySignature(initData: String): Rejection? =
        try {
            if (telegramValidatorService.verifySignature(initData)) {
                null
            } else {
                Rejection(HttpServletResponse.SC_FORBIDDEN, AuthErrorType.SESSION_EXPIRED)
            }
        } catch (e: InvalidAuthSignatureException) {
            logger.warn("Invalid signature: ${e.message}")
            Rejection(HttpServletResponse.SC_UNAUTHORIZED, AuthErrorType.INVALID_AUTH_SIGNATURE)
        } catch (e: Exception) {
            logger.error("Unexpected error", e)
            Rejection(HttpServletResponse.SC_UNAUTHORIZED, AuthErrorType.UNKNOWN)
        }

    private fun admit(
        access: UserAccessDto,
        response: HttpServletResponse,
    ): Boolean {
        val rejection =
            when {
                access.isBanned -> AuthErrorType.BANNED
                access.isDeleted -> AuthErrorType.DELETED
                else -> return true
            }

        logger.info("Rejecting externalUserId [{}] as {}", access.externalUserId, rejection.value)
        rejectResponse(response, HttpServletResponse.SC_FORBIDDEN, rejection)

        return false
    }

    private fun syncProfile(telegramUser: TelegramAuthUserDto) {
        telegramUserAccessService.syncProfile(
            telegramUser.externalUserId,
            TelegramUserProfileDto(
                firstName = telegramUser.firstName,
                lastName = telegramUser.lastName,
                username = telegramUser.username,
            ),
        )
    }

    private fun rejectResponse(
        response: HttpServletResponse,
        status: Int,
        errorCode: AuthErrorType = AuthErrorType.UNKNOWN,
    ) {
        response.status = status
        response.setHeader(AuthErrorType.HEADER_NAME, errorCode.value)
    }

    private data class Rejection(
        val status: Int,
        val errorCode: AuthErrorType,
    )
}

package ru.illine.drinking.ponies.config.web.security

enum class AuthErrorType(val value: String) {
    UNKNOWN("unknown"),
    INVALID_AUTH_SIGNATURE("invalid_auth_signature"),
    SESSION_EXPIRED("session_expired"),
    FORBIDDEN_ADMIN("forbidden_admin");

    companion object {
        const val HEADER_NAME = "X-Auth-Error-Code"
    }
}

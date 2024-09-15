package ru.illine.drinking.ponies.config.web

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import ru.illine.drinking.ponies.model.dto.response.ErrorResponse

@RestControllerAdvice
class DefaultExceptionHandler {

    private val logger = LoggerFactory.getLogger("EXCEPTION-HANDLER")

    @ExceptionHandler(value = [MissingServletRequestParameterException::class])
    fun handleMissingParams(e: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> {
        logger.error("Missing required parameter: ${e.parameterName}", e)
        val response = ErrorResponse("missing required parameter")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    @ExceptionHandler(value = [MethodArgumentNotValidException::class])
    fun handleValidationExceptions(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = e.bindingResult.allErrors
        val errorMessages = errors.joinToString(", ") {
            val fieldError = it as FieldError
            "${fieldError.field}: ${fieldError.defaultMessage}"
        }
        logger.error("Validation failed: $errorMessages", e)

        val response = ErrorResponse("validation failed")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    @ExceptionHandler(value = [Exception::class])
    fun exception(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unknown error: {}", e.message, e)
        val response = ErrorResponse("unknown server error")
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }
}
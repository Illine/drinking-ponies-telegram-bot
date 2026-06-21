package ru.illine.drinking.ponies.config.web

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import ru.illine.drinking.ponies.test.tag.UnitTest

@UnitTest
@DisplayName("DefaultExceptionHandler Unit Test")
class DefaultExceptionHandlerTest {
    private lateinit var handler: DefaultExceptionHandler

    @BeforeEach
    fun setUp() {
        handler = DefaultExceptionHandler()
    }

    @Test
    @DisplayName("handleMissingParamsException(): returns 400 with 'missing required parameter'")
    fun `handleMissingParamsException returns 400`() {
        val exception = MissingServletRequestParameterException("initData", "String")

        val response = handler.handleMissingParamsException(exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)
        assertEquals("missing required parameter", response.body?.message)
    }

    @Test
    @DisplayName("handleValidationException(): MethodArgumentNotValidException - returns 400 with 'validation failed'")
    fun `handleValidationException with MethodArgumentNotValidException returns 400`() {
        val bindingResult = mock<BindingResult>()
        val fieldError = FieldError("obj", "field", "must not be null")
        whenever(bindingResult.fieldErrors).thenReturn(listOf(fieldError))
        val exception = mock<MethodArgumentNotValidException>()
        whenever(exception.bindingResult).thenReturn(bindingResult)

        val response = handler.handleValidationException(exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("validation failed", response.body?.message)
    }

    @Test
    @DisplayName("handleValidationException(): IllegalArgumentException - returns 400 with 'validation failed'")
    fun `handleValidationException with IllegalArgumentException returns 400`() {
        val exception = IllegalArgumentException("invalid data")

        val response = handler.handleValidationException(exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("validation failed", response.body?.message)
    }

    @Test
    @DisplayName("handleValidationException(): HttpMessageNotReadableException - returns 400 with 'validation failed'")
    fun `handleValidationException with HttpMessageNotReadableException returns 400`() {
        val exception = mock<HttpMessageNotReadableException>()

        val response = handler.handleValidationException(exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("validation failed", response.body?.message)
    }

    @Test
    @DisplayName(
        "handleValidationException(): MethodArgumentTypeMismatchException - returns 400 with 'validation failed'",
    )
    fun `handleValidationException with MethodArgumentTypeMismatchException returns 400`() {
        val exception = mock<MethodArgumentTypeMismatchException>()

        val response = handler.handleValidationException(exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("validation failed", response.body?.message)
    }

    @Test
    @DisplayName("handleNoResourceFound(): returns 404 with 'resource not found'")
    fun `handleNoResourceFound returns 404`() {
        val exception = mock<NoResourceFoundException>()
        whenever(exception.resourcePath).thenReturn("/unknown/path")

        val response = handler.handleNoResourceFound(exception)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals("resource not found", response.body?.message)
    }

    @Test
    @DisplayName("handleUnknownException(): returns 500 with 'unknown server error'")
    fun `handleUnknownException returns 500`() {
        val exception = RuntimeException("something went wrong")

        val response = handler.handleUnknownException(exception)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("unknown server error", response.body?.message)
    }
}

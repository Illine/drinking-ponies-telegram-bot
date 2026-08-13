package ru.illine.drinking.ponies.config.web

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlConfig
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import ru.illine.drinking.ponies.config.cache.CacheConfig
import ru.illine.drinking.ponies.config.web.WebConfigAuthInterceptorIntegrationTest.DefaultSecureTestConfig
import ru.illine.drinking.ponies.config.web.security.AuthErrorType
import ru.illine.drinking.ponies.service.telegram.TelegramValidatorService
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest
import java.util.stream.Stream

@SpringIntegrationTest
@DisplayName("WebConfig default-secure interceptor Spring Integration Test")
@Import(DefaultSecureTestConfig::class)
@Sql(
    scripts = ["classpath:sql/access/TelegramUserAccessService.sql"],
    config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
)
@Sql(
    scripts = ["classpath:sql/clear.sql"],
    config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
)
class WebConfigAuthInterceptorIntegrationTest
    @Autowired
    constructor(
        private val restTemplate: TestRestTemplate,
        private val cacheManager: CacheManager,
        @Qualifier("requestMappingHandlerMapping")
        private val handlerMapping: RequestMappingHandlerMapping,
    ) {
        @MockitoBean
        private lateinit var telegramValidatorService: TelegramValidatorService

        private val telegramUser = DtoGenerator.generateTelegramAuthUserDto()

        @BeforeEach
        fun setUp() {
            cacheManager.getCache(CacheConfig.USER_ACCESS_FLAGS)?.clear()
            whenever(telegramValidatorService.verifySignature(any())).thenReturn(true)
            whenever(telegramValidatorService.map(any())).thenReturn(telegramUser)
        }

        private fun buildHeaders(): HttpHeaders =
            HttpHeaders().apply {
                set("X-Authorization-Telegram-Data", "test-init-data")
            }

        @Nested
        @DisplayName("default-secure: endpoint outside any exclude pattern is protected by default")
        inner class DefaultSecureEndpoint {
            private val url = "/test-default-secure"

            @Test
            @DisplayName("missing auth header - returns 401 with X-Auth-Error-Code invalid_auth_signature")
            fun `missing auth header returns 401 with invalid_auth_signature header`() {
                val response =
                    restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        HttpEntity<Void>(HttpHeaders()),
                        Void::class.java,
                    )

                assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
                assertEquals(
                    AuthErrorType.INVALID_AUTH_SIGNATURE.value,
                    response.headers.getFirst(AuthErrorType.HEADER_NAME),
                )
                verify(telegramValidatorService, never()).verifySignature(any())
                verify(telegramValidatorService, never()).map(any())
            }

            @Test
            @DisplayName("valid auth header - returns 200")
            fun `valid auth header returns 200`() {
                val response =
                    restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        HttpEntity<Void>(buildHeaders()),
                        String::class.java,
                    )

                assertEquals(HttpStatus.OK, response.statusCode)
                verify(telegramValidatorService).verifySignature("test-init-data")
                verify(telegramValidatorService).map("test-init-data")
            }
        }

        @Nested
        @DisplayName("excluded public path: /v3/api-docs is reachable without auth header")
        inner class ExcludedPublicPath {
            @Test
            @DisplayName("GET /v3/api-docs without auth header - returns 200 and is not blocked by auth")
            fun `api docs without auth header returns 200`() {
                val response =
                    restTemplate.exchange(
                        "/v3/api-docs",
                        HttpMethod.GET,
                        HttpEntity<Void>(HttpHeaders()),
                        String::class.java,
                    )

                assertEquals(HttpStatus.OK, response.statusCode)
                assertNotEquals(
                    AuthErrorType.INVALID_AUTH_SIGNATURE.value,
                    response.headers.getFirst(AuthErrorType.HEADER_NAME),
                )
                verify(telegramValidatorService, never()).verifySignature(any())
            }
        }

        @Nested
        @DisplayName("a banned or deleted caller is turned away on every route of the application")
        inner class RejectedCaller {
            @ParameterizedTest(name = "[{index}] {1} - {0}")
            @MethodSource(
                "ru.illine.drinking.ponies.config.web.WebConfigAuthInterceptorIntegrationTest#provideRoutesAndStates",
            )
            @DisplayName("returns 403 with the state code, the admin check never getting a say")
            fun `turns a rejected caller away`(
                route: Route,
                expected: AuthErrorType,
                externalUserId: Long,
            ) {
                whenever(telegramValidatorService.map(any()))
                    .thenReturn(telegramUser.copy(externalUserId = externalUserId))

                val response =
                    restTemplate.exchange(
                        route.path,
                        route.method,
                        HttpEntity(route.body, buildHeaders().apply { contentType = MediaType.APPLICATION_JSON }),
                        String::class.java,
                    )

                assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
                assertEquals(expected.value, response.headers.getFirst(AuthErrorType.HEADER_NAME))
            }

            // An unmapped path answers 403 as well - the barrier sits in front of the resource handler too - so a
            // typo above would read as a guarantee while walking nothing, and a route added later would go
            // unwalked. Both are caught by comparing the list to what the application really publishes.
            @Test
            @DisplayName("guard: the walked routes are exactly the routes the application publishes")
            fun `the walked routes are the published ones`() {
                val published =
                    handlerMapping.handlerMethods
                        .filterValues { it.beanType.packageName.startsWith(APPLICATION_PACKAGE) }
                        .keys
                        .flatMap { info ->
                            val patterns = info.pathPatternsCondition?.patternValues.orEmpty()
                            info.methodsCondition.methods.flatMap { method ->
                                patterns.map { "${method.name} $it" }
                            }
                        }.toSet()

                assertEquals(
                    EVERY_ROUTE.map { it.toString() }.toSet(),
                    published,
                    "A route nobody walks here is a route nobody knows the barrier covers",
                )
            }
        }

        @TestConfiguration
        class DefaultSecureTestConfig {
            @RestController
            @RequestMapping("/test-default-secure")
            class DefaultSecureTestController {
                @GetMapping
                fun defaultSecure(): String = "ok"
            }
        }

        data class Route(
            val method: HttpMethod,
            val pattern: String,
            val path: String = pattern,
            val body: String? = null,
        ) {
            override fun toString(): String = "$method $pattern"
        }

        companion object {
            private const val APPLICATION_PACKAGE = "ru.illine.drinking.ponies"
            private const val BANNED_EXTERNAL_ID = 777002L
            private const val DELETED_EXTERNAL_ID = 777001L
            private const val EMPTY_BODY = "{}"

            private val EVERY_ROUTE =
                listOf(
                    Route(HttpMethod.GET, "/test-default-secure"),
                    Route(HttpMethod.GET, "/systems/version"),
                    Route(HttpMethod.GET, "/users/me"),
                    Route(HttpMethod.GET, "/users"),
                    Route(HttpMethod.GET, "/users/{id}", "/users/1"),
                    Route(HttpMethod.PATCH, "/users/{id}", "/users/1", EMPTY_BODY),
                    Route(HttpMethod.GET, "/systems/loggers"),
                    Route(HttpMethod.GET, "/systems/loggers/{name}", "/systems/loggers/SERVICE"),
                    Route(HttpMethod.PUT, "/systems/loggers/{name}", "/systems/loggers/SERVICE", EMPTY_BODY),
                    Route(HttpMethod.POST, "/systems/loggers/reset"),
                    Route(HttpMethod.GET, "/settings"),
                    Route(HttpMethod.PUT, "/settings/quiet-mode", body = EMPTY_BODY),
                    Route(HttpMethod.PUT, "/settings/timezone", body = EMPTY_BODY),
                    Route(HttpMethod.PUT, "/settings/interval", body = EMPTY_BODY),
                    Route(HttpMethod.PUT, "/settings/notification-status", body = EMPTY_BODY),
                    Route(HttpMethod.PUT, "/settings/goal", body = EMPTY_BODY),
                    Route(HttpMethod.GET, "/statistics"),
                    Route(HttpMethod.GET, "/statistics/today"),
                    Route(HttpMethod.POST, "/statistics/water", body = EMPTY_BODY),
                    Route(HttpMethod.GET, "/notifications/next"),
                    Route(HttpMethod.GET, "/notifications/pause"),
                    Route(HttpMethod.PUT, "/notifications/pause", body = EMPTY_BODY),
                    Route(HttpMethod.GET, "/notifications/history"),
                    Route(HttpMethod.PATCH, "/notifications/history/{id}", "/notifications/history/1", EMPTY_BODY),
                )

            @JvmStatic
            fun provideRoutesAndStates(): Stream<Arguments> =
                EVERY_ROUTE
                    .flatMap {
                        listOf(
                            Arguments.of(it, AuthErrorType.BANNED, BANNED_EXTERNAL_ID),
                            Arguments.of(it, AuthErrorType.DELETED, DELETED_EXTERNAL_ID),
                        )
                    }.stream()
        }
    }

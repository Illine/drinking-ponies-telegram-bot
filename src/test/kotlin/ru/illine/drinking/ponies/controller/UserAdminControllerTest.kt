package ru.illine.drinking.ponies.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.cache.CacheManager
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlConfig
import ru.illine.drinking.ponies.config.cache.CacheConfig
import ru.illine.drinking.ponies.config.web.security.AuthErrorType
import ru.illine.drinking.ponies.dao.access.TelegramUserAccessService
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserProfileDto
import ru.illine.drinking.ponies.model.dto.response.UserDetailsResponse
import ru.illine.drinking.ponies.model.dto.response.UsersResponse
import ru.illine.drinking.ponies.service.telegram.TelegramValidatorService
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest
import java.time.Instant
import java.util.stream.Stream

@SpringIntegrationTest
@DisplayName("UserAdminController Spring Integration Test")
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
class UserAdminControllerTest
    @Autowired
    constructor(
        private val restTemplate: TestRestTemplate,
        private val objectMapper: ObjectMapper,
        private val cacheManager: CacheManager,
        private val telegramUserAccessService: TelegramUserAccessService,
    ) {
        @MockitoBean
        private lateinit var telegramValidatorService: TelegramValidatorService

        private val adminUser =
            DtoGenerator.generateTelegramUserDto(
                externalUserId = ADMIN_EXTERNAL_ID,
                firstName = "Alisa",
                lastName = "Petrova",
                username = "alisaadmin",
            )

        private val nonAdminUser =
            DtoGenerator.generateTelegramUserDto(
                externalUserId = PLAIN_EXTERNAL_ID,
                firstName = "Bob",
                lastName = "Smith",
                username = "bobsmith",
            )

        @BeforeEach
        fun setUp() {
            cacheManager.getCache(CacheConfig.USER_ACCESS_FLAGS)?.clear()
            whenever(telegramValidatorService.verifySignature(any())).thenReturn(true)
            whenever(telegramValidatorService.map(any())).thenReturn(adminUser)
        }

        private fun buildHeaders(): HttpHeaders =
            HttpHeaders().apply {
                set("X-Authorization-Telegram-Data", "test-init-data")
                contentType = MediaType.APPLICATION_JSON
            }

        private fun getUsers(query: String = ""): UsersResponse {
            val response =
                restTemplate.exchange(
                    "/users$query",
                    HttpMethod.GET,
                    HttpEntity<Void>(buildHeaders()),
                    UsersResponse::class.java,
                )

            assertEquals(HttpStatus.OK, response.statusCode)
            return response.body!!
        }

        private fun searchFor(search: String): UsersResponse {
            val response =
                restTemplate.exchange(
                    "/users?search={search}",
                    HttpMethod.GET,
                    HttpEntity<Void>(buildHeaders()),
                    UsersResponse::class.java,
                    mapOf("search" to search),
                )

            assertEquals(HttpStatus.OK, response.statusCode)
            return response.body!!
        }

        private fun getUser(id: Long): UserDetailsResponse {
            val response =
                restTemplate.exchange(
                    "/users/$id",
                    HttpMethod.GET,
                    HttpEntity<Void>(buildHeaders()),
                    UserDetailsResponse::class.java,
                )

            assertEquals(HttpStatus.OK, response.statusCode)
            return response.body!!
        }

        private fun patchState(
            id: Long,
            body: String,
        ) = restTemplate.exchange(
            "/users/$id",
            HttpMethod.PATCH,
            HttpEntity(body, buildHeaders()),
            String::class.java,
        )

        @Nested
        @DisplayName("GET /users")
        inner class GetUsers {
            @Test
            @DisplayName("no parameters - returns every user including the soft-deleted one, newest activity first")
            fun `returns the whole first page`() {
                val body = getUsers()

                assertEquals(listOf(3L, 2L, 1L, 4L, 5L, 6L, 7L, 8L), body.users.map { it.id })
                assertEquals(0, body.page)
                assertEquals(20, body.size)
                assertEquals(8L, body.total)
                assertEquals(8L, body.counts.all)
                assertEquals(5L, body.counts.active)
                assertEquals(1L, body.counts.inactive)
                assertEquals(2L, body.counts.banned)
            }

            @ParameterizedTest(name = "[{index}] status={0} - ids {1}, total {2}")
            @CsvSource(
                "ALL,      '3,2,1,4,5,6,7,8', 8",
                "ACTIVE,   '2,1,5,7,8',       5",
                "INACTIVE, '3',               1",
                "BANNED,   '4,6',             2",
            )
            @DisplayName("narrows the page down to the requested status chip and reports its counter as total")
            fun `filters by status`(
                status: String,
                expectedIds: String,
                expectedTotal: Long,
            ) {
                val body = getUsers("?status=$status")

                assertEquals(expectedIds.split(",").map { it.toLong() }, body.users.map { it.id })
                assertEquals(expectedTotal, body.total)
            }

            @Test
            @DisplayName("a banned and deleted user shows on the BANNED chip only, a ban winning over a deletion")
            fun `a ban wins over a deletion`() {
                assertTrue(getUsers("?status=BANNED").users.map { it.id }.contains(BANNED_DELETED_USER_ID))
                assertFalse(getUsers("?status=INACTIVE").users.map { it.id }.contains(BANNED_DELETED_USER_ID))
                assertTrue(getUsers().users.map { it.id }.contains(BANNED_DELETED_USER_ID))
            }

            @Test
            @DisplayName("status only filters the page - the chip counters keep covering everyone")
            fun `counts ignore the requested status`() {
                val body = getUsers("?status=BANNED")

                assertEquals(listOf(4L, 6L), body.users.map { it.id })
                assertEquals(8L, body.counts.all)
                assertEquals(5L, body.counts.active)
                assertEquals(1L, body.counts.inactive)
                assertEquals(2L, body.counts.banned)
            }

            @Test
            @DisplayName("the chip counters do follow the search")
            fun `counts follow the search`() {
                val body = getUsers("?search=777")

                assertEquals(4L, body.counts.all)
                assertEquals(1L, body.counts.active)
                assertEquals(1L, body.counts.inactive)
                assertEquals(2L, body.counts.banned)
            }

            @ParameterizedTest(name = "[{index}] search={0} - ids {1}")
            @CsvSource(
                "bob,      '2'",
                "petrova,  '1'",
                "evefresh, '5'",
                "777002,   '4'",
                "5,        '5'",
            )
            @DisplayName("searches over name, username, telegram id and internal id")
            fun `searches over name username and both ids`(
                search: String,
                expectedIds: String,
            ) {
                val body = getUsers("?search=$search")

                assertEquals(expectedIds.split(",").map { it.toLong() }, body.users.map { it.id })
            }

            @ParameterizedTest(name = "[{index}] search=[{0}]")
            @ValueSource(strings = ["", "   "])
            @DisplayName("a blank search is the same as no search at all")
            fun `blank search matches everyone`(search: String) {
                val body = searchFor(search)

                assertEquals(8L, body.total)
                assertEquals(8, body.users.size)
            }

            @ParameterizedTest(name = "[{index}] search=[{0}] - ids {1}")
            @CsvSource(
                "alice_p, '7'",
                "_,       '7'",
                "%,       ''",
                """\,      ''""",
            )
            @DisplayName("escapes the LIKE wildcards a user may type into the search box")
            fun `escapes the wildcards of the search box`(
                search: String,
                expectedIds: String,
            ) {
                val body = searchFor(search)

                assertEquals(
                    expectedIds.split(",").filter { it.isNotBlank() }.map { it.toLong() },
                    body.users.map { it.id },
                )
            }

            @ParameterizedTest(name = "[{index}] page={0}, size={1} - ids {2}")
            @CsvSource(
                "0, 2, '3,2'",
                "1, 2, '1,4'",
                "2, 2, '5,6'",
                "3, 2, '7,8'",
            )
            @DisplayName("paginates while total and the counters stay over the whole result")
            fun `paginates the result`(
                page: Int,
                size: Int,
                expectedIds: String,
            ) {
                val body = getUsers("?page=$page&size=$size")

                assertEquals(expectedIds.split(",").map { it.toLong() }, body.users.map { it.id })
                assertEquals(page, body.page)
                assertEquals(size, body.size)
                assertEquals(8L, body.total)
                assertEquals(8L, body.counts.all)
            }

            @ParameterizedTest(name = "[{index}] query={0}")
            @ValueSource(strings = ["?page=-1", "?size=0", "?size=101", "?status=BOGUS"])
            @DisplayName("rejects parameters outside of the contract with 400")
            fun `rejects invalid parameters`(query: String) {
                val response =
                    restTemplate.exchange(
                        "/users$query",
                        HttpMethod.GET,
                        HttpEntity<Void>(buildHeaders()),
                        String::class.java,
                    )

                assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            }

            @Test
            @DisplayName("serializes a row with the telegram id under telegramUserId and UTC timestamps")
            fun `serializes a row under the published keys`() {
                val response =
                    restTemplate.exchange(
                        "/users?status=INACTIVE",
                        HttpMethod.GET,
                        HttpEntity<Void>(buildHeaders()),
                        String::class.java,
                    )

                val row = objectMapper.readTree(response.body).path("users").path(0)
                assertEquals(DELETED_USER_ID, row.path("id").asLong())
                assertEquals(DELETED_EXTERNAL_ID, row.path("telegramUserId").asLong())
                assertEquals("Carol", row.path("firstName").asText())
                assertEquals("carolgone", row.path("username").asText())
                assertFalse(row.path("isActive").asBoolean())
                assertEquals("2026-03-06T12:00:00Z", row.path("lastActivity").asText())
            }

            @Test
            @DisplayName("serializes a null last activity as null, not as a fallback timestamp")
            fun `serializes a null last activity in a row`() {
                val row = getUsers("?search=$ACTIVE_EXTERNAL_ID").users.single()

                assertEquals(ACTIVE_USER_ID, row.id)
                assertNull(row.lastActivity)
            }
        }

        @Nested
        @DisplayName("GET /users/{id}")
        inner class GetUserCard {
            @Test
            @DisplayName("returns 200 with the full card of an existing user")
            fun `returns 200 with the card`() {
                val body = getUser(ADMIN_USER_ID)

                assertEquals(ADMIN_USER_ID, body.id)
                assertEquals(ADMIN_EXTERNAL_ID, body.externalUserId)
                assertEquals("Alisa", body.firstName)
                assertEquals("Petrova", body.lastName)
                assertEquals("alisaadmin", body.username)
                assertTrue(body.isAdmin)
                assertFalse(body.isBanned)
                assertTrue(body.isActive)
                assertEquals("Europe/Moscow", body.timeZone)
                assertEquals(Instant.parse("2026-01-01T10:00:00Z"), body.registeredAt)
                assertEquals(Instant.parse("2026-03-04T09:00:00Z"), body.lastActivity)
            }

            @Test
            @DisplayName("returns 200 for a soft-deleted user - they stay reachable for the admin")
            fun `returns 200 for a soft deleted user`() {
                val body = getUser(DELETED_USER_ID)

                assertEquals(DELETED_USER_ID, body.id)
                assertFalse(body.isActive)
            }

            @Test
            @DisplayName("returns 200 with a null last activity for a user who never answered")
            fun `returns 200 with null last activity`() {
                assertNull(getUser(ACTIVE_USER_ID).lastActivity)
            }

            @Test
            @DisplayName("unknown id - returns 404")
            fun `returns 404 for an unknown id`() {
                val response =
                    restTemplate.exchange(
                        "/users/$MISSING_USER_ID",
                        HttpMethod.GET,
                        HttpEntity<Void>(buildHeaders()),
                        String::class.java,
                    )

                assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
            }
        }

        @Nested
        @DisplayName("PATCH /users/{id}")
        inner class UpdateUserState {
            @Test
            @DisplayName("isActive=false - returns 200 with the soft-deleted card and moves the user to INACTIVE")
            fun `soft deletes the user`() {
                val response = patchState(ACTIVE_USER_ID, """{"isActive": false}""")

                assertEquals(HttpStatus.OK, response.statusCode)
                assertFalse(objectMapper.readTree(response.body).path("isActive").asBoolean())
                assertFalse(getUser(ACTIVE_USER_ID).isActive)
                assertEquals(listOf(DELETED_USER_ID, ACTIVE_USER_ID), getUsers("?status=INACTIVE").users.map { it.id })
            }

            @Test
            @DisplayName("isActive=true - returns 200 with the restored card and moves the user back to ACTIVE")
            fun `restores the user`() {
                val response = patchState(DELETED_USER_ID, """{"isActive": true}""")

                assertEquals(HttpStatus.OK, response.statusCode)
                assertTrue(objectMapper.readTree(response.body).path("isActive").asBoolean())
                assertTrue(getUser(DELETED_USER_ID).isActive)
                assertTrue(getUsers("?status=INACTIVE").users.isEmpty())
            }

            @Test
            @DisplayName("the returned card carries the rest of the profile untouched")
            fun `returns the rest of the card unchanged`() {
                val response = patchState(DELETED_USER_ID, """{"isActive": true}""")

                val body = objectMapper.readTree(response.body)
                assertEquals(DELETED_USER_ID, body.path("id").asLong())
                assertEquals(DELETED_EXTERNAL_ID, body.path("telegramUserId").asLong())
                assertEquals("Carol", body.path("firstName").asText())
                assertEquals("Asia/Kolkata", body.path("timeZone").asText())
                assertEquals("2026-03-06T12:00:00Z", body.path("lastActivity").asText())
                assertEquals("2026-01-03T12:00:00Z", body.path("registeredAt").asText())
            }

            @ParameterizedTest(name = "[{index}] isActive={0}")
            @CsvSource("true", "false")
            @DisplayName("repeating the same update is idempotent")
            fun `repeating the same update is idempotent`(isActive: Boolean) {
                patchState(ACTIVE_USER_ID, """{"isActive": $isActive}""")

                val response = patchState(ACTIVE_USER_ID, """{"isActive": $isActive}""")

                assertEquals(HttpStatus.OK, response.statusCode)
                assertEquals(isActive, getUser(ACTIVE_USER_ID).isActive)
            }

            @Test
            @DisplayName("empty payload - returns 400 and leaves the user alone")
            fun `returns 400 for an empty payload`() {
                val response = patchState(ACTIVE_USER_ID, "{}")

                assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
                assertTrue(getUser(ACTIVE_USER_ID).isActive)
            }

            @Test
            @DisplayName("unknown id - returns 404")
            fun `returns 404 for an unknown id`() {
                val response = patchState(MISSING_USER_ID, """{"isActive": false}""")

                assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
            }

            @Test
            @DisplayName("evicts the cached access flags, so the next request of that user is resolved afresh")
            fun `evicts the cached access flags of the updated user`() {
                val cache = cacheManager.getCache(CacheConfig.USER_ACCESS_FLAGS)!!
                telegramUserAccessService.resolveAccessFlags(ACTIVE_EXTERNAL_ID, TelegramUserProfileDto())
                assertNotNull(cache.get(ACTIVE_EXTERNAL_ID))

                patchState(ACTIVE_USER_ID, """{"isActive": false}""")

                assertNull(cache.get(ACTIVE_EXTERNAL_ID))
            }
        }

        @Nested
        @DisplayName("class-level @AdminOnly guard")
        inner class AdminGuard {
            @ParameterizedTest(name = "[{index}] {0} {1}")
            @MethodSource("ru.illine.drinking.ponies.controller.UserAdminControllerTest#provideAdminEndpoints")
            @DisplayName("non-admin caller - returns 403 with X-Auth-Error-Code forbidden_admin on every route")
            fun `rejects a non admin on every admin endpoint`(
                method: HttpMethod,
                path: String,
                body: String?,
            ) {
                whenever(telegramValidatorService.map(any())).thenReturn(nonAdminUser)

                val response =
                    restTemplate.exchange(path, method, HttpEntity<String?>(body, buildHeaders()), String::class.java)

                assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
                assertEquals(
                    AuthErrorType.FORBIDDEN_ADMIN.value,
                    response.headers.getFirst(AuthErrorType.HEADER_NAME),
                )
            }

            @Test
            @DisplayName("non-admin caller - the rejected update never reaches the database")
            fun `rejected update leaves the user untouched`() {
                whenever(telegramValidatorService.map(any())).thenReturn(nonAdminUser)
                patchState(ACTIVE_USER_ID, """{"isActive": false}""")

                whenever(telegramValidatorService.map(any())).thenReturn(adminUser)
                assertTrue(getUser(ACTIVE_USER_ID).isActive)
            }
        }

        companion object {
            private const val ADMIN_USER_ID = 1L
            private const val DELETED_USER_ID = 3L
            private const val ACTIVE_USER_ID = 5L
            private const val BANNED_DELETED_USER_ID = 6L
            private const val MISSING_USER_ID = 999L

            private const val ADMIN_EXTERNAL_ID = 1L
            private const val PLAIN_EXTERNAL_ID = 2L
            private const val DELETED_EXTERNAL_ID = 777001L
            private const val ACTIVE_EXTERNAL_ID = 777003L

            @JvmStatic
            fun provideAdminEndpoints(): Stream<Arguments> =
                Stream.of(
                    Arguments.of(HttpMethod.GET, "/users", null),
                    Arguments.of(HttpMethod.GET, "/users/$ADMIN_USER_ID", null),
                    Arguments.of(HttpMethod.PATCH, "/users/$ADMIN_USER_ID", """{"isActive": false}"""),
                )
        }
    }

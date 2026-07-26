package ru.illine.drinking.ponies.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import ru.illine.drinking.ponies.exception.NotificationHistoryEntryNotEditableException
import ru.illine.drinking.ponies.exception.NotificationHistoryEntryNotFoundException
import ru.illine.drinking.ponies.exception.NotificationSettingsNotFoundException
import ru.illine.drinking.ponies.model.base.NotificationHistoryStatus
import ru.illine.drinking.ponies.model.dto.response.ErrorResponse
import ru.illine.drinking.ponies.model.dto.response.NotificationHistoryDay
import ru.illine.drinking.ponies.model.dto.response.NotificationHistoryEvent
import ru.illine.drinking.ponies.model.dto.response.NotificationHistoryResponse
import ru.illine.drinking.ponies.model.dto.response.NotificationNextResponse
import ru.illine.drinking.ponies.model.dto.response.PauseStateResponse
import ru.illine.drinking.ponies.service.notification.NotificationHistoryService
import ru.illine.drinking.ponies.service.notification.NotificationSettingsService
import ru.illine.drinking.ponies.service.telegram.TelegramValidatorService
import ru.illine.drinking.ponies.test.generator.DtoGenerator
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest
import java.time.Instant
import java.time.LocalDate
import java.util.stream.Stream

@SpringIntegrationTest
@DisplayName("NotificationController Spring Integration Test")
class NotificationControllerTest
    @Autowired
    constructor(
        private val restTemplate: TestRestTemplate,
        private val objectMapper: ObjectMapper,
    ) {
        @MockitoBean
        private lateinit var telegramValidatorService: TelegramValidatorService

        @MockitoBean
        private lateinit var notificationSettingsService: NotificationSettingsService

        @MockitoBean
        private lateinit var notificationHistoryService: NotificationHistoryService

        private val telegramUser = DtoGenerator.generateTelegramUserDto()

        @BeforeEach
        fun setUp() {
            whenever(telegramValidatorService.verifySignature(any())).thenReturn(true)
            whenever(telegramValidatorService.map(any())).thenReturn(telegramUser)
        }

        private fun buildHeaders(): HttpHeaders =
            HttpHeaders().apply {
                set("X-Authorization-Telegram-Data", "test-init-data")
            }

        @Nested
        @DisplayName("GET /notifications/next")
        inner class GetNextNotification {
            @Test
            @DisplayName("valid request - returns 200 with next notification time")
            fun `returns 200 with next notification time`() {
                val expectedInstant = Instant.parse("2025-01-01T14:00:00Z")
                whenever(notificationSettingsService.getNextNotificationAt(any())).thenReturn(expectedInstant)
                val headers = buildHeaders()

                val response =
                    restTemplate.exchange(
                        "/notifications/next",
                        HttpMethod.GET,
                        HttpEntity<Void>(headers),
                        NotificationNextResponse::class.java,
                    )

                assertEquals(HttpStatus.OK, response.statusCode)
                assertNotNull(response.body)
                assertEquals(expectedInstant, response.body!!.nextNotificationAt)
                verify(notificationSettingsService).getNextNotificationAt(any())
            }

            @Test
            @DisplayName("service throws IllegalArgumentException - returns 400")
            fun `returns 400 when service throws`() {
                doThrow(IllegalArgumentException("User not found"))
                    .whenever(notificationSettingsService)
                    .getNextNotificationAt(any())
                val headers = buildHeaders()

                val response =
                    restTemplate.exchange(
                        "/notifications/next",
                        HttpMethod.GET,
                        HttpEntity<Void>(headers),
                        Void::class.java,
                    )

                assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            }

            @Test
            @DisplayName("missing auth header - returns 401")
            fun `returns 401`() {
                val response =
                    restTemplate.exchange(
                        "/notifications/next",
                        HttpMethod.GET,
                        HttpEntity<Void>(HttpHeaders()),
                        Void::class.java,
                    )

                assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
                verifyNoInteractions(notificationSettingsService)
            }

            @Test
            @DisplayName("settings not found (e.g. disabled user) - returns 404")
            fun `returns 404 when settings not found`() {
                doThrow(NotificationSettingsNotFoundException("Not found"))
                    .whenever(notificationSettingsService)
                    .getNextNotificationAt(any())
                val headers = buildHeaders()

                val response =
                    restTemplate.exchange(
                        "/notifications/next",
                        HttpMethod.GET,
                        HttpEntity<Void>(headers),
                        Void::class.java,
                    )

                assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
                verify(notificationSettingsService).getNextNotificationAt(any())
            }
        }

        @Nested
        @DisplayName("GET /notifications/pause")
        inner class GetPauseState {
            @Test
            @DisplayName("paused user - returns 200 with paused=true and pauseUntil")
            fun `returns 200 with paused true`() {
                val expectedPauseUntil = Instant.parse("2025-01-01T18:00:00Z")
                val expected = PauseStateResponse(paused = true, pauseUntil = expectedPauseUntil)
                whenever(notificationSettingsService.getPauseState(any())).thenReturn(expected)
                val headers = buildHeaders()

                val response =
                    restTemplate.exchange(
                        "/notifications/pause",
                        HttpMethod.GET,
                        HttpEntity<Void>(headers),
                        PauseStateResponse::class.java,
                    )

                assertEquals(HttpStatus.OK, response.statusCode)
                assertNotNull(response.body)
                assertEquals(true, response.body!!.paused)
                assertEquals(expectedPauseUntil, response.body!!.pauseUntil)
                verify(notificationSettingsService).getPauseState(any())
            }

            @Test
            @DisplayName("not paused - returns 200 with paused=false and null pauseUntil")
            fun `returns 200 with paused false`() {
                val expected = PauseStateResponse(paused = false, pauseUntil = null)
                whenever(notificationSettingsService.getPauseState(any())).thenReturn(expected)
                val headers = buildHeaders()

                val response =
                    restTemplate.exchange(
                        "/notifications/pause",
                        HttpMethod.GET,
                        HttpEntity<Void>(headers),
                        PauseStateResponse::class.java,
                    )

                assertEquals(HttpStatus.OK, response.statusCode)
                assertNotNull(response.body)
                assertEquals(false, response.body!!.paused)
                assertEquals(null, response.body!!.pauseUntil)
            }

            @Test
            @DisplayName("missing auth header - returns 401")
            fun `returns 401`() {
                val response =
                    restTemplate.exchange(
                        "/notifications/pause",
                        HttpMethod.GET,
                        HttpEntity<Void>(HttpHeaders()),
                        Void::class.java,
                    )

                assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
                verifyNoInteractions(notificationSettingsService)
            }

            @Test
            @DisplayName("settings not found (e.g. disabled user) - returns 404")
            fun `returns 404 when settings not found`() {
                doThrow(NotificationSettingsNotFoundException("Not found"))
                    .whenever(notificationSettingsService)
                    .getPauseState(any())
                val headers = buildHeaders()

                val response =
                    restTemplate.exchange(
                        "/notifications/pause",
                        HttpMethod.GET,
                        HttpEntity<Void>(headers),
                        Void::class.java,
                    )

                assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
            }
        }

        @Nested
        @DisplayName("PUT /notifications/pause")
        inner class ChangePause {
            @Test
            @DisplayName("minutes=60 - returns 204 and calls pauseNotifications")
            fun `returns 204 on pause`() {
                val headers = buildHeaders()

                val response =
                    restTemplate.exchange(
                        "/notifications/pause?minutes=60",
                        HttpMethod.PUT,
                        HttpEntity<Void>(headers),
                        Void::class.java,
                    )

                assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
                verify(notificationSettingsService).pauseNotifications(telegramUser.externalUserId, 60)
                verify(notificationSettingsService, never()).cancelPause(any<Long>())
            }

            @Test
            @DisplayName("minutes=0 - returns 204 and calls cancelPause")
            fun `returns 204 on cancel`() {
                val headers = buildHeaders()

                val response =
                    restTemplate.exchange(
                        "/notifications/pause?minutes=0",
                        HttpMethod.PUT,
                        HttpEntity<Void>(headers),
                        Void::class.java,
                    )

                assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
                verify(notificationSettingsService).cancelPause(telegramUser.externalUserId)
                verify(notificationSettingsService, never()).pauseNotifications(any<Long>(), any<Long>())
            }

            @ParameterizedTest(name = "[{index}] minutes={0} - returns 400 from @Min/@Max validation")
            @ValueSource(longs = [-1, 301])
            @DisplayName("minutes out of [0, 300] range - returns 400 from validation")
            fun `returns 400 on out-of-range minutes`(minutes: Long) {
                val headers = buildHeaders()

                val response =
                    restTemplate.exchange(
                        "/notifications/pause?minutes=$minutes",
                        HttpMethod.PUT,
                        HttpEntity<Void>(headers),
                        Void::class.java,
                    )

                assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
                verifyNoInteractions(notificationSettingsService)
            }

            @Test
            @DisplayName("missing minutes param - returns 400")
            fun `returns 400 on missing minutes`() {
                val headers = buildHeaders()

                val response =
                    restTemplate.exchange(
                        "/notifications/pause",
                        HttpMethod.PUT,
                        HttpEntity<Void>(headers),
                        Void::class.java,
                    )

                assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
                verifyNoInteractions(notificationSettingsService)
            }

            @Test
            @DisplayName("invalid minutes type - returns 400")
            fun `returns 400 on invalid minutes type`() {
                val headers = buildHeaders()

                val response =
                    restTemplate.exchange(
                        "/notifications/pause?minutes=abc",
                        HttpMethod.PUT,
                        HttpEntity<Void>(headers),
                        Void::class.java,
                    )

                assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
                verifyNoInteractions(notificationSettingsService)
            }

            @Test
            @DisplayName("missing auth header - returns 401")
            fun `returns 401`() {
                val response =
                    restTemplate.exchange(
                        "/notifications/pause?minutes=60",
                        HttpMethod.PUT,
                        HttpEntity<Void>(HttpHeaders()),
                        Void::class.java,
                    )

                assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
                verifyNoInteractions(notificationSettingsService)
            }

            @Test
            @DisplayName("settings not found (e.g. disabled user) - returns 404")
            fun `returns 404 when settings not found`() {
                doThrow(NotificationSettingsNotFoundException("Not found"))
                    .whenever(notificationSettingsService)
                    .pauseNotifications(any<Long>(), any<Long>())
                val headers = buildHeaders()

                val response =
                    restTemplate.exchange(
                        "/notifications/pause?minutes=60",
                        HttpMethod.PUT,
                        HttpEntity<Void>(headers),
                        Void::class.java,
                    )

                assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
            }
        }

        @Nested
        @DisplayName("GET /notifications/history")
        inner class GetHistory {
            private val from = LocalDate.of(2026, 5, 4)
            private val to = LocalDate.of(2026, 5, 10)

            private fun journal() =
                NotificationHistoryResponse(
                    days =
                        listOf(
                            NotificationHistoryDay(
                                date = from,
                                events =
                                    listOf(
                                        DtoGenerator.generateNotificationHistoryEvent(
                                            eventTime = Instant.parse("2026-05-04T09:30:00Z"),
                                            status = NotificationHistoryStatus.MISSED,
                                            amountMl = 0,
                                        ),
                                    ),
                            ),
                        ),
                )

            private fun get(query: String) =
                restTemplate.exchange(
                    "/notifications/history?$query",
                    HttpMethod.GET,
                    HttpEntity<Void>(buildHeaders()),
                    String::class.java,
                )

            @Test
            @DisplayName("valid range - returns 200 with the journal and forwards from/to")
            fun `returns 200 with the journal`() {
                whenever(notificationHistoryService.getHistory(any(), any(), any())).thenReturn(journal())

                val response = get("from=$from&to=$to")

                assertEquals(HttpStatus.OK, response.statusCode)
                assertEquals(journal(), objectMapper.readValue(response.body, NotificationHistoryResponse::class.java))
                // Dates and instants go over the wire as ISO 8601 strings, not as numbers.
                val day = objectMapper.readTree(response.body).path("days").path(0)
                val event = day.path("events").path(0)
                assertEquals("2026-05-04", day.path("date").asText())
                assertEquals("2026-05-04T09:30:00Z", event.path("eventTime").asText())
                verify(notificationHistoryService).getHistory(telegramUser.externalUserId, from, to)
            }

            @Test
            @DisplayName("no entries in the range - returns 200 with an empty days array")
            fun `returns 200 with empty days`() {
                whenever(notificationHistoryService.getHistory(any(), any(), any()))
                    .thenReturn(NotificationHistoryResponse(days = emptyList()))

                val response = get("from=$from&to=$to")

                assertEquals(HttpStatus.OK, response.statusCode)
                assertTrue(
                    objectMapper.readValue(response.body, NotificationHistoryResponse::class.java).days.isEmpty(),
                )
            }

            @Test
            @DisplayName("service rejects the range - returns 400")
            fun `returns 400 when the service rejects the range`() {
                doThrow(IllegalArgumentException("Invalid parameter"))
                    .whenever(notificationHistoryService)
                    .getHistory(any(), any(), any())

                assertEquals(HttpStatus.BAD_REQUEST, get("from=$from&to=$to").statusCode)
            }

            @ParameterizedTest(name = "[{index}] query={0} - returns 400")
            @ValueSource(strings = ["from=foobar&to=2026-05-10", "from=2026-05-04", "to=2026-05-10", "from=&to="])
            @DisplayName("malformed or missing dates - returns 400")
            fun `returns 400 on malformed dates`(query: String) {
                assertEquals(HttpStatus.BAD_REQUEST, get(query).statusCode)
                verifyNoInteractions(notificationHistoryService)
            }

            @Test
            @DisplayName("missing auth header - returns 401")
            fun `returns 401`() {
                val response =
                    restTemplate.exchange(
                        "/notifications/history?from=$from&to=$to",
                        HttpMethod.GET,
                        HttpEntity<Void>(HttpHeaders()),
                        Void::class.java,
                    )

                assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
                verifyNoInteractions(notificationHistoryService)
            }

            @Test
            @DisplayName("settings not found (e.g. disabled user) - returns 404")
            fun `returns 404 when settings not found`() {
                doThrow(NotificationSettingsNotFoundException("Not found"))
                    .whenever(notificationHistoryService)
                    .getHistory(any(), any(), any())

                assertEquals(HttpStatus.NOT_FOUND, get("from=$from&to=$to").statusCode)
            }
        }

        @Nested
        @DisplayName("PATCH /notifications/history/{id}")
        inner class UpdateHistoryEntry {
            private fun patch(
                id: String,
                body: String,
            ) = restTemplate.exchange(
                "/notifications/history/$id",
                HttpMethod.PATCH,
                HttpEntity(body, buildHeaders().apply { contentType = MediaType.APPLICATION_JSON }),
                String::class.java,
            )

            @ParameterizedTest(name = "[{index}] body={0} - forwards status={1} and amountMl={2}")
            @MethodSource("ru.illine.drinking.ponies.controller.NotificationControllerTest#updatePayloads")
            @DisplayName("valid payload - returns 200 with the updated entry and forwards the payload as it came")
            fun `returns 200 on a valid payload`(
                body: String,
                expectedStatus: NotificationHistoryStatus,
                expectedAmountMl: Int?,
            ) {
                val updated =
                    DtoGenerator.generateNotificationHistoryEvent(
                        status = expectedStatus,
                        amountMl = expectedAmountMl ?: 0,
                    )
                whenever(notificationHistoryService.updateEntry(any(), any(), any(), anyOrNull())).thenReturn(updated)

                val response = patch("1042", body)

                assertEquals(HttpStatus.OK, response.statusCode)
                assertEquals(updated, objectMapper.readValue(response.body, NotificationHistoryEvent::class.java))
                verify(notificationHistoryService)
                    .updateEntry(telegramUser.externalUserId, 1042L, expectedStatus, expectedAmountMl)
            }

            @ParameterizedTest(name = "[{index}] body={0} - returns 400")
            @ValueSource(
                strings = [
                    """{"status":"UNKNOWN","amountMl":300}""",
                    """{"amountMl":300}""",
                    "{}",
                ],
            )
            @DisplayName("malformed payload - returns 400")
            fun `returns 400 on malformed payload`(body: String) {
                assertEquals(HttpStatus.BAD_REQUEST, patch("1042", body).statusCode)
                verifyNoInteractions(notificationHistoryService)
            }

            @Test
            @DisplayName("service rejects the payload (an out-of-range amount, for one) - returns 400")
            fun `returns 400 when the service rejects the payload`() {
                doThrow(IllegalArgumentException("Invalid parameter: 'amountMl' must be within 50..1000"))
                    .whenever(notificationHistoryService)
                    .updateEntry(any(), any(), any(), anyOrNull())

                val response = patch("1042", """{"status":"CONFIRMED","amountMl":1001}""")

                assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            }

            @Test
            @DisplayName("non-numeric id - returns 400")
            fun `returns 400 on non-numeric id`() {
                val response = patch("abc", """{"status":"MISSED"}""")

                assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
                verifyNoInteractions(notificationHistoryService)
            }

            @Test
            @DisplayName("unknown, foreign or non-journal entry - returns 404")
            fun `returns 404 when entry not found`() {
                doThrow(NotificationHistoryEntryNotFoundException("Not found"))
                    .whenever(notificationHistoryService)
                    .updateEntry(any(), any(), any(), anyOrNull())

                val response = patch("1042", """{"status":"MISSED"}""")

                assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
                assertEquals(
                    ErrorResponse("notification history entry not found"),
                    objectMapper.readValue(response.body, ErrorResponse::class.java),
                )
            }

            @Test
            @DisplayName("entry outside the edit window - returns 409")
            fun `returns 409 when entry is not editable`() {
                doThrow(NotificationHistoryEntryNotEditableException("Too old"))
                    .whenever(notificationHistoryService)
                    .updateEntry(any(), any(), any(), anyOrNull())

                val response = patch("1042", """{"status":"MISSED"}""")

                assertEquals(HttpStatus.CONFLICT, response.statusCode)
                assertEquals(
                    ErrorResponse("notification history entry is not editable"),
                    objectMapper.readValue(response.body, ErrorResponse::class.java),
                )
            }

            @Test
            @DisplayName("missing auth header - returns 401")
            fun `returns 401`() {
                val response =
                    restTemplate.exchange(
                        "/notifications/history/1042",
                        HttpMethod.PATCH,
                        HttpEntity(
                            """{"status":"MISSED"}""",
                            HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON },
                        ),
                        Void::class.java,
                    )

                assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
                verifyNoInteractions(notificationHistoryService)
            }
        }

        companion object {
            @JvmStatic
            fun updatePayloads(): Stream<Arguments> =
                Stream.of(
                    Arguments.of(
                        """{"status":"CONFIRMED","amountMl":300}""",
                        NotificationHistoryStatus.CONFIRMED,
                        300,
                    ),
                    // A missed entry ignores the amount, but whatever the form sent still reaches the service.
                    Arguments.of("""{"status":"MISSED"}""", NotificationHistoryStatus.MISSED, null),
                    Arguments.of("""{"status":"MISSED","amountMl":0}""", NotificationHistoryStatus.MISSED, 0),
                )
        }
    }

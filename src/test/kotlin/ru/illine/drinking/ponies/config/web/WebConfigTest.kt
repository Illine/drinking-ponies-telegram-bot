package ru.illine.drinking.ponies.config.web

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.RequestEntity
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest
import java.net.URI

@SpringIntegrationTest
@DisplayName("WebConfig Spring Integration Test")
class WebConfigTest @Autowired constructor(
    private val restTemplate: TestRestTemplate
) {

    private val url = "/settings/modes/silent"

    @ParameterizedTest
    @ValueSource(strings = ["http://localhost:3000", "http://localhost:4000"])
    @DisplayName("OPTIONS request from allowed origin - returns 200 with CORS headers")
    fun `cors preflight from allowed origin returns cors headers`(allowedOrigin: String) {
        val headers = HttpHeaders().apply {
            set("Origin", allowedOrigin)
            set("Access-Control-Request-Method", "PUT")
        }
        val request = RequestEntity<Void>(headers, HttpMethod.OPTIONS, URI(url))

        val response = restTemplate.exchange(request, Void::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(allowedOrigin, response.headers["Access-Control-Allow-Origin"]?.first())
        assertTrue(response.headers["Access-Control-Allow-Methods"]?.first()?.contains("PUT") == true)
    }

    @Test
    @DisplayName("request from disallowed origin - does not return CORS allow header")
    fun `cors request from disallowed origin has no allow-origin header`() {
        val headers = HttpHeaders().apply {
            set("Origin", "http://evil.example.com")
            set("Access-Control-Request-Method", "PUT")
        }
        val request = RequestEntity<Void>(headers, HttpMethod.OPTIONS, URI(url))

        val response = restTemplate.exchange(request, Void::class.java)

        assertTrue(response.headers["Access-Control-Allow-Origin"].isNullOrEmpty())
    }
}

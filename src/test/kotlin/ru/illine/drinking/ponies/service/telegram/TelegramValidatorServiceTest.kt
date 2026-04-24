package ru.illine.drinking.ponies.service.telegram

import org.apache.commons.codec.digest.HmacUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.illine.drinking.ponies.config.property.TelegramBotProperties
import ru.illine.drinking.ponies.service.telegram.impl.TelegramValidatorServiceImpl
import ru.illine.drinking.ponies.test.tag.UnitTest
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant

@UnitTest
@DisplayName("TelegramValidatorService Unit Test")
class TelegramValidatorServiceTest {

    private val token = "token"
    private val userJson = """{"id":1,"first_name":"First Name"}"""
    private val queryId = "query"

    private lateinit var service: TelegramValidatorService

    @BeforeEach
    fun setUp() {
        val properties = TelegramBotProperties(
            version = "1.0.0",
            token = token,
            username = "username",
            creatorId = 1L,
            autoUpdateCommands = false,
            authDateExpirationSeconds = 3600,
            http = TelegramBotProperties.Http(connectionTimeToLiveInSec = 30, maxConnectionTotal = 10)
        )
        service = TelegramValidatorServiceImpl(properties)
    }

    @Test
    @DisplayName("verifySignature(): returns true for valid initData with correct hash and fresh auth_date")
    fun `verifySignature valid`() {
        val initData = buildInitData(Instant.now().epochSecond)

        assertTrue(service.verifySignature(initData))
    }

    @Test
    @DisplayName("verifySignature(): returns false when auth_date is expired")
    fun `verifySignature expired auth date`() {
        val expiredAuthDate = Instant.now().epochSecond - 7200
        val initData = buildInitData(expiredAuthDate)

        assertFalse(service.verifySignature(initData))
    }

    @Test
    @DisplayName("verifySignature(): returns false when hash is invalid")
    fun `verifySignature bad hash`() {
        val authDate = Instant.now().epochSecond
        val encodedUser = URLEncoder.encode(userJson, StandardCharsets.UTF_8)
        val initData = "auth_date=$authDate&hash=badhash&query_id=$queryId&user=$encodedUser"

        assertFalse(service.verifySignature(initData))
    }

    @Test
    @DisplayName("map(): maps valid initData with user field to TelegramUserDto")
    fun `map valid initData`() {
        val encodedUser = URLEncoder.encode(userJson, StandardCharsets.UTF_8)
        val initData = "auth_date=1234567890&user=$encodedUser"

        val result = service.map(initData)

        assertEquals(1L, result.telegramId)
        assertEquals("First Name", result.firstName)
    }

    @Test
    @DisplayName("map(): throws when user field is missing in initData")
    fun `map missing user field throws`() {
        val initData = "auth_date=1234567890&query_id=$queryId"

        assertThrows(IllegalArgumentException::class.java) {
            service.map(initData)
        }
    }

    private fun buildInitData(authDate: Long): String {
        val fields = sortedMapOf(
            "auth_date" to authDate.toString(),
            "query_id" to queryId,
            "user" to userJson
        )
        val dataCheckString = fields.entries.joinToString("\n") { "${it.key}=${it.value}" }
        val secretKey = HmacUtils("HmacSHA256", "WebAppData").hmac(token)
        val hash = HmacUtils("HmacSHA256", secretKey).hmacHex(dataCheckString)
        val encodedUser = URLEncoder.encode(userJson, StandardCharsets.UTF_8)
        return "auth_date=$authDate&hash=$hash&query_id=$queryId&user=$encodedUser"
    }
}
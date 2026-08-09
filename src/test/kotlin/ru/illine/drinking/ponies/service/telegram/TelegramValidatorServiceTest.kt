package ru.illine.drinking.ponies.service.telegram

import org.apache.commons.codec.digest.HmacUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import ru.illine.drinking.ponies.config.property.TelegramBotProperties
import ru.illine.drinking.ponies.exception.InvalidAuthSignatureException
import ru.illine.drinking.ponies.service.telegram.impl.TelegramValidatorServiceImpl
import ru.illine.drinking.ponies.test.generator.DtoGenerator
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
        val properties =
            TelegramBotProperties(
                token = token,
                username = "username",
                miniAppUrl = "https://t.me/Test/app",
                autoUpdateTelegramConfig = false,
                authDateExpirationSeconds = 3600,
                http = TelegramBotProperties.Http(connectionTimeToLiveInSec = 30, maxConnectionTotal = 10),
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

    @ParameterizedTest(name = "[{index}] initData=\"{0}\" - throws InvalidAuthSignatureException")
    @ValueSource(strings = ["no-equals-sign-here", ""])
    @DisplayName("verifySignature(): throws InvalidAuthSignatureException for malformed/empty initData")
    fun `verifySignature throws on malformed or empty initData`(initData: String) {
        assertThrows(InvalidAuthSignatureException::class.java) {
            service.verifySignature(initData)
        }
    }

    @Test
    @DisplayName("map(): maps valid initData with user field to TelegramUserDto")
    fun `map valid initData`() {
        val encodedUser = URLEncoder.encode(userJson, StandardCharsets.UTF_8)
        val initData = "auth_date=1234567890&user=$encodedUser"

        val result = service.map(initData)

        assertEquals(1L, result.externalUserId)
        assertEquals("First Name", result.firstName)
        assertNull(result.lastName, "an absent optional field stays null instead of getting a default")
        assertNull(result.username, "an absent optional field stays null instead of getting a default")
    }

    @Test
    @DisplayName("map(): carries every telegram field into the internal DTO and leaves the access flags unset")
    fun `map carries every telegram field`() {
        // language_code is a real init data field we deliberately do not model - it must not break the parsing.
        // The access flags are sent by a hostile client on purpose: they must not survive into the internal DTO.
        val fullUserJson =
            """
            {"id":42,"first_name":"Alisa","last_name":"Petrova","username":"alisa","language_code":"ru",
             "is_admin":true,"is_banned":true,"is_active":true}
            """.trimIndent()
        val encodedUser = URLEncoder.encode(fullUserJson, StandardCharsets.UTF_8)
        val initData = "auth_date=1234567890&user=$encodedUser"

        val result = service.map(initData)

        assertEquals(
            DtoGenerator.generateTelegramAuthUserDto(
                externalUserId = 42L,
                firstName = "Alisa",
                lastName = "Petrova",
                username = "alisa",
            ),
            result,
        )
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
        val fields =
            sortedMapOf(
                "auth_date" to authDate.toString(),
                "query_id" to queryId,
                "user" to userJson,
            )
        val dataCheckString = fields.entries.joinToString("\n") { "${it.key}=${it.value}" }
        val secretKey = HmacUtils("HmacSHA256", "WebAppData").hmac(token)
        val hash = HmacUtils("HmacSHA256", secretKey).hmacHex(dataCheckString)
        val encodedUser = URLEncoder.encode(userJson, StandardCharsets.UTF_8)
        return "auth_date=$authDate&hash=$hash&query_id=$queryId&user=$encodedUser"
    }
}

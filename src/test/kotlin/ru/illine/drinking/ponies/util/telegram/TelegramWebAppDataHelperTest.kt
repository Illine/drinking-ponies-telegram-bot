package ru.illine.drinking.ponies.util.telegram

import org.apache.commons.codec.digest.HmacUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.illine.drinking.ponies.test.tag.UnitTest
import java.time.Duration
import java.time.Instant

@UnitTest
@DisplayName("TelegramWebAppDataHelper Unit Test")
class TelegramWebAppDataHelperTest {

    @Test
    @DisplayName("validateAuthDate(): returns true when auth_date is present and not expired")
    fun `validateAuthDate valid`() {
        val authDate = Instant.now().epochSecond
        val data = mapOf(TelegramWebAppDataHelper.AUTH_DATE_FIELD_NAME to authDate.toString())

        assertTrue(TelegramWebAppDataHelper.validateAuthDate(data, Duration.ofHours(1)))
    }

    @Test
    @DisplayName("validateAuthDate(): returns false when auth_date is expired")
    fun `validateAuthDate expired`() {
        val expiration = Duration.ofHours(1)
        val authDate = Instant.now().epochSecond - expiration.seconds - 1
        val data = mapOf(TelegramWebAppDataHelper.AUTH_DATE_FIELD_NAME to authDate.toString())

        assertFalse(TelegramWebAppDataHelper.validateAuthDate(data, expiration))
    }

    @Test
    @DisplayName("validateAuthDate(): returns false when auth_date field is missing")
    fun `validateAuthDate no field`() {
        val data = mapOf(TelegramWebAppDataHelper.QUERY_ID_FIELD_NAME to "id")

        assertFalse(TelegramWebAppDataHelper.validateAuthDate(data, Duration.ofHours(1)))
    }

    @Test
    @DisplayName("validateHash(): returns true when hash is valid")
    fun `validateHash valid`() {
        val token = "test-bot-token"
        val dataFields = sortedMapOf(
            TelegramWebAppDataHelper.AUTH_DATE_FIELD_NAME to "1234567890",
            TelegramWebAppDataHelper.QUERY_ID_FIELD_NAME to "test-query-id",
            TelegramWebAppDataHelper.USER_FIELD_NAME to "{\"id\":1}"
        )
        val dataCheckString = dataFields.map { "${it.key}=${it.value}" }.joinToString("\n")
        val secretKey = HmacUtils("HmacSHA256", "WebAppData").hmac(token)
        val expectedHash = HmacUtils("HmacSHA256", secretKey).hmacHex(dataCheckString)

        val dataWithHash = dataFields.toMutableMap().apply {
            put(TelegramWebAppDataHelper.HASH_FIELD_NAME, expectedHash)
        }

        assertTrue(TelegramWebAppDataHelper.validateHash(dataWithHash, token))
    }

    @Test
    @DisplayName("validateHash(): returns false when hash is invalid")
    fun `validateHash invalid`() {
        val data = mapOf(
            TelegramWebAppDataHelper.AUTH_DATE_FIELD_NAME to "1234567890",
            TelegramWebAppDataHelper.HASH_FIELD_NAME to "wrong-hash"
        )

        assertFalse(TelegramWebAppDataHelper.validateHash(data, "test-bot-token"))
    }

    @Test
    @DisplayName("validateHash(): returns false when hash field is missing")
    fun `validateHash no hash`() {
        val data = mapOf(TelegramWebAppDataHelper.AUTH_DATE_FIELD_NAME to "1234567890")

        assertFalse(TelegramWebAppDataHelper.validateHash(data, "test-bot-token"))
    }

    @Test
    @DisplayName("decode(): correctly decodes URL-encoded init data into a map")
    fun `decode valid`() {
        val initData = "query_id=1&auth_date=1234567890&hash=hash"

        val result = TelegramWebAppDataHelper.decode(initData)

        assertEquals("1", result[TelegramWebAppDataHelper.QUERY_ID_FIELD_NAME])
        assertEquals("1234567890", result[TelegramWebAppDataHelper.AUTH_DATE_FIELD_NAME])
        assertEquals("hash", result[TelegramWebAppDataHelper.HASH_FIELD_NAME])
    }

    @Test
    @DisplayName("decode(): correctly decodes URL-encoded special characters")
    fun `decode with encoded characters`() {
        val initData = "user=%7B%22id%22%3A1%7D&auth_date=1234567890"

        val result = TelegramWebAppDataHelper.decode(initData)

        assertEquals("{\"id\":1}", result[TelegramWebAppDataHelper.USER_FIELD_NAME])
        assertEquals("1234567890", result[TelegramWebAppDataHelper.AUTH_DATE_FIELD_NAME])
    }
}

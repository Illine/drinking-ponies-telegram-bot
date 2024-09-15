import org.apache.commons.codec.digest.HmacUtils
import org.slf4j.LoggerFactory
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object TelegramWebAppDataHelper {

    private val logger = LoggerFactory.getLogger("HELPER")

    private val ALGORITHM_NAME = "HmacSHA256"
    private val ALGORITHM_HASH_KEY = "WebAppData"

    val HASH_FIELD_NAME = "hash"
    val AUTH_DATE_FIELD_NAME = "auth_date"
    val QUERY_ID_FIELD_NAME = "query_id"
    val USER_FIELD_NAME = "user"

    fun validateAuthDate(decodedData: Map<String, String>, expirationTime: Duration): Boolean {
        val hashTimestamp = decodedData[AUTH_DATE_FIELD_NAME]?.toLongOrNull()

        if (hashTimestamp == null) {
            logger.warn("Field 'auth_date' hasn't been received or is not a valid number!")
            return false
        }

        val currentDateTime = ZonedDateTime.now(ZoneId.systemDefault()).toLocalDateTime()
        val hashDateTime = Instant.ofEpochSecond(hashTimestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
        val durationSinceHash = Duration.between(hashDateTime, currentDateTime)

        if (durationSinceHash > expirationTime) {
            logger.debug("Hash has expired: currentTime = {}, hashTime = {}", currentDateTime, hashDateTime)
            return false
        }
        return true
    }

    fun validateHash(decodedData: Map<String, String>, token: String): Boolean {
        val receivedHash = decodedData[HASH_FIELD_NAME]
        if (receivedHash == null) {
            logger.warn("Field 'hash' hasn't been received!")
            return false
        }
        logger.debug("Received hash: {}", receivedHash)

        val dataCheckString = decodedData
            .filter { it.key != HASH_FIELD_NAME }
            .toSortedMap()
            .map { "${it.key}=${it.value}" }
            .joinToString("\n")
        logger.debug("Data string for HMAC: \n{}", dataCheckString)

        val secretKey = HmacUtils(ALGORITHM_NAME, ALGORITHM_HASH_KEY).hmac(token)
        val checkedHash = HmacUtils(ALGORITHM_NAME, secretKey).hmacHex(dataCheckString)
        logger.debug("Computed hash (Hex): {}", checkedHash)

        val equalHash = receivedHash.equals(checkedHash, ignoreCase = true)
        if (!equalHash) {
            logger.debug("Hashes not equals, return false")
            return false
        }

        return true
    }

    fun decode(initData: String): Map<String, String> {
        val decodedData = URLDecoder.decode(initData, StandardCharsets.UTF_8.name())
        return decodedData.split("&").associate {
            val (key, value) = it.split("=")
            key to value
        }
    }
}

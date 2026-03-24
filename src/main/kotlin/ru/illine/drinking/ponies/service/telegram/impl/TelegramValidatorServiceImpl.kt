package ru.illine.drinking.ponies.service.telegram.impl

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.illine.drinking.ponies.config.property.TelegramBotProperties
import ru.illine.drinking.ponies.model.dto.TelegramUserDto
import ru.illine.drinking.ponies.service.telegram.TelegramValidatorService
import ru.illine.drinking.ponies.util.telegram.TelegramWebAppDataHelper.QUERY_ID_FIELD_NAME
import ru.illine.drinking.ponies.util.telegram.TelegramWebAppDataHelper.USER_FIELD_NAME
import ru.illine.drinking.ponies.util.telegram.TelegramWebAppDataHelper.decode
import ru.illine.drinking.ponies.util.telegram.TelegramWebAppDataHelper.validateAuthDate
import ru.illine.drinking.ponies.util.telegram.TelegramWebAppDataHelper.validateHash
import java.time.Duration

@Service
class TelegramValidatorServiceImpl(
    private val telegramBotProperties: TelegramBotProperties
) : TelegramValidatorService {

    private val logger = LoggerFactory.getLogger("SERVICE")

    private val objectMapper = jacksonObjectMapper()

    override fun verifySignature(initData: String, expirationTime: Duration): Boolean {
        val token = telegramBotProperties.token
        val decodedData = decode(initData)
        logger.debug("Validate 'initDate' with query_id: {}", decodedData[QUERY_ID_FIELD_NAME])

        return validateAuthDate(decodedData, expirationTime) && validateHash(decodedData, token)
    }

    override fun map(initData: String): TelegramUserDto {
        val decodedData = decode(initData)
        val decodedUser = decodedData[USER_FIELD_NAME]?.let { objectMapper.readValue<TelegramUserDto>(it) }

        return requireNotNull(decodedUser, { "Failed to map, invalid data: $initData" })
    }
}

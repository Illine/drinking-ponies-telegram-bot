package ru.illine.drinking.ponies.service.statistic.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.illine.drinking.ponies.dao.access.WaterStatisticAccessService
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.WaterEntrySourceType
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserDto
import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto
import ru.illine.drinking.ponies.service.statistic.WaterStatisticService
import ru.illine.drinking.ponies.util.water.WaterEntryConstants
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Service
class WaterStatisticServiceImpl(
    private val waterStatisticAccessService: WaterStatisticAccessService,
    private val clock: Clock
) : WaterStatisticService {

    private val logger = LoggerFactory.getLogger("SERVICE")

    override fun recordEvent(telegramUser: TelegramUserDto, eventType: AnswerNotificationType, waterAmountMl: Int) {
        logger.info(
            "Recording water statistic event [{}] for user [{}], amount [{}] ml",
            eventType,
            telegramUser.externalUserId,
            waterAmountMl
        )

        waterStatisticAccessService.save(
            WaterStatisticDto(
                telegramUser = telegramUser,
                eventTime = LocalDateTime.now(clock),
                eventType = eventType,
                waterAmountMl = waterAmountMl
            )
        )
    }

    override fun recordEvents(telegramUsers: Collection<TelegramUserDto>, eventType: AnswerNotificationType) {
        logger.info("Recording [{}] water statistic events with type [{}]", telegramUsers.size, eventType)

        waterStatisticAccessService.saveAll(
            telegramUsers.map {
                WaterStatisticDto(
                    telegramUser = it,
                    eventTime = LocalDateTime.now(clock),
                    eventType = eventType
                )
            }
        )
    }

    override fun manualRecordEvent(externalUserId: Long, consumedAt: Instant, amountMl: Int) {
        require(amountMl.toLong() in WaterEntryConstants.MIN_ML..WaterEntryConstants.MAX_ML) {
            "Water amount must be between ${WaterEntryConstants.MIN_ML} and ${WaterEntryConstants.MAX_ML} ml, got: $amountMl"
        }

        val now = Instant.now(clock)
        require(!consumedAt.isAfter(now)) {
            "consumedAt must not be in the future, got: $consumedAt (now: $now)"
        }
        require(!consumedAt.isBefore(now.minus(WaterEntryConstants.MAX_DAYS_AGO, ChronoUnit.DAYS))) {
            "consumedAt must not be older than ${WaterEntryConstants.MAX_DAYS_AGO} days, got: $consumedAt"
        }

        logger.info(
            "Recording manual water entry for user [{}], amount [{}] ml at [{}]",
            externalUserId,
            amountMl,
            consumedAt
        )

        waterStatisticAccessService.save(
            WaterStatisticDto(
                telegramUser = TelegramUserDto.create(externalUserId),
                eventTime = LocalDateTime.ofInstant(consumedAt, ZoneOffset.UTC),
                eventType = AnswerNotificationType.YES,
                waterAmountMl = amountMl,
                source = WaterEntrySourceType.MANUAL
            )
        )
    }

}

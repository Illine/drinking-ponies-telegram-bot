package ru.illine.drinking.ponies.service.statistic.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.illine.drinking.ponies.dao.access.WaterStatisticAccessService
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserDto
import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto
import ru.illine.drinking.ponies.service.statistic.WaterStatisticService
import java.time.Clock
import java.time.LocalDateTime

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

}

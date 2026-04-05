package ru.illine.drinking.ponies.dao.access.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.illine.drinking.ponies.builder.TelegramUserBuilder
import ru.illine.drinking.ponies.builder.WaterStatisticBuilder
import ru.illine.drinking.ponies.dao.access.WaterStatisticAccessService
import ru.illine.drinking.ponies.dao.repository.TelegramUserRepository
import ru.illine.drinking.ponies.dao.repository.WaterStatisticRepository
import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto

@Service
class WaterStatisticAccessServiceImpl(
    private val waterStatisticRepository: WaterStatisticRepository,
    private val userRepository: TelegramUserRepository,
) : WaterStatisticAccessService {

    private val logger = LoggerFactory.getLogger("ACCESS-SERVICE")

    @Transactional
    override fun save(dto: WaterStatisticDto): WaterStatisticDto {
        logger.debug("Saving a water statistic record for a telegram user: [${dto.telegramUser.externalUserId}]")

        val userEntity = requireNotNull(
            userRepository.findByExternalUserId(dto.telegramUser.externalUserId),
            { "Not found a Telegram User by externalUserId [${dto.telegramUser.externalUserId}]" }
        )

        return waterStatisticRepository.save(WaterStatisticBuilder.toEntity(dto, userEntity))
            .let { WaterStatisticBuilder.toDto(it, TelegramUserBuilder.toDto(it.telegramUser)) }
    }

    @Transactional
    override fun saveAll(statistics: Collection<WaterStatisticDto>): List<WaterStatisticDto> {
        logger.debug("Saving [${statistics.size}] water statistic records")

        return statistics
            .map {
                val userEntity = requireNotNull(
                    userRepository.findByExternalUserId(it.telegramUser.externalUserId),
                    { "Not found a Telegram User by externalUserId [${it.telegramUser.externalUserId}]" }
                )
                WaterStatisticBuilder.toEntity(it, userEntity)
            }
            .let { waterStatisticRepository.saveAll(it) }
            .map { WaterStatisticBuilder.toDto(it, TelegramUserBuilder.toDto(it.telegramUser)) }
    }

}

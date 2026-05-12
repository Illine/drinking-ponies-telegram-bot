package ru.illine.drinking.ponies.service.statistic

import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserDto
import java.time.Instant

interface WaterStatisticService {

    fun recordEvent(telegramUser: TelegramUserDto, eventType: AnswerNotificationType, waterAmountMl: Int = 0)

    fun recordEvents(telegramUsers: Collection<TelegramUserDto>, eventType: AnswerNotificationType)

    fun manualRecordEvent(externalUserId: Long, consumedAt: Instant, amountMl: Int)

}

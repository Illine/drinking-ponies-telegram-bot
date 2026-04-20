package ru.illine.drinking.ponies.model.dto.internal

import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import java.time.LocalDateTime

data class WaterStatisticDto(
    var id: Long? = null,

    val telegramUser: TelegramUserDto,

    val eventTime: LocalDateTime,

    val eventType: AnswerNotificationType,

    val waterAmountMl: Int = 0
)

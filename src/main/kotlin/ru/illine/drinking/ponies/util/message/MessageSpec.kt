package ru.illine.drinking.ponies.util.message

import ru.illine.drinking.ponies.model.dto.message.InsightStatsContext
import ru.illine.drinking.ponies.model.dto.message.MessageContext

sealed class MessageSpec<C : MessageContext>(
    val id: String
) {

    object InsightStats : MessageSpec<InsightStatsContext>(id = "insight_stats")

}

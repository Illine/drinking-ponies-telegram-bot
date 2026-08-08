package ru.illine.drinking.ponies.util.message.templates

import ru.illine.drinking.ponies.model.dto.internal.NoContext
import ru.illine.drinking.ponies.util.message.RuleBucket
import ru.illine.drinking.ponies.util.message.singlePhrase

object LocalNotificationQuestion {
    val PHRASES: List<RuleBucket<NoContext>> = singlePhrase { "Водица выпита?" }
}

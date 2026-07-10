package ru.illine.drinking.ponies.util.message.templates

import ru.illine.drinking.ponies.model.dto.message.NoContext
import ru.illine.drinking.ponies.util.message.RuleBucket
import ru.illine.drinking.ponies.util.message.singlePhrase

object LocalNotificationAnswerCancel {
    val PHRASES: List<RuleBucket<NoContext>> = singlePhrase { "Милый зайчик, пожалуйста, напейся в следующий раз!" }
}

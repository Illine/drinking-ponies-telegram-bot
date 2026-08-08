package ru.illine.drinking.ponies.util.message.templates

import ru.illine.drinking.ponies.model.dto.internal.NotificationQuestionEditedContext
import ru.illine.drinking.ponies.util.message.RuleBucket
import ru.illine.drinking.ponies.util.message.singlePhrase

object LocalNotificationQuestionEdited {
    val PHRASES: List<RuleBucket<NotificationQuestionEditedContext>> =
        singlePhrase { "Водица выпита?\nБыло выбрано: *${it.answerDisplayName}*" }
}

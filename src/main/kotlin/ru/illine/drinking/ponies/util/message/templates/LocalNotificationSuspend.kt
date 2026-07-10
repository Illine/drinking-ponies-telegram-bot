package ru.illine.drinking.ponies.util.message.templates

import ru.illine.drinking.ponies.model.dto.message.NotificationSuspendContext
import ru.illine.drinking.ponies.util.message.RuleBucket
import ru.illine.drinking.ponies.util.message.singlePhrase

object LocalNotificationSuspend {
    val PHRASES: List<RuleBucket<NotificationSuspendContext>> =
        singlePhrase {
            """
            Котик, твое уведомление отложено!
            Через ${it.durationDisplayName} я тебя снова побеспокою, жди!
            """.trimIndent()
        }
}

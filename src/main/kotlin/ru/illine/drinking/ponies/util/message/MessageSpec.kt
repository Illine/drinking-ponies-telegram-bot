package ru.illine.drinking.ponies.util.message

import ru.illine.drinking.ponies.model.dto.message.DefaultSettingsContext
import ru.illine.drinking.ponies.model.dto.message.GreetingContext
import ru.illine.drinking.ponies.model.dto.message.InsightStatsContext
import ru.illine.drinking.ponies.model.dto.message.MessageContext
import ru.illine.drinking.ponies.model.dto.message.NoContext
import ru.illine.drinking.ponies.model.dto.message.NotificationQuestionEditedContext
import ru.illine.drinking.ponies.model.dto.message.NotificationSuspendContext

sealed class MessageSpec<C : MessageContext>(
    val id: String,
) {
    object InsightStats : MessageSpec<InsightStatsContext>(id = "insight_stats")

    object NotificationAnswerYes : MessageSpec<NoContext>(id = "notification_answer_yes")

    object NotificationQuestion : MessageSpec<NoContext>(id = "notification_question")

    object NotificationSnoozeMenu : MessageSpec<NoContext>(id = "notification_snooze_menu")

    object NotificationWaterAmountMenu : MessageSpec<NoContext>(id = "notification_water_amount_menu")

    object NotificationAnswerCancel : MessageSpec<NoContext>(id = "notification_answer_cancel")

    object NotificationQuestionEdited :
        MessageSpec<NotificationQuestionEditedContext>(id = "notification_question_edited")

    object NotificationSuspend : MessageSpec<NotificationSuspendContext>(id = "notification_suspend")

    object Greeting : MessageSpec<GreetingContext>(id = "greeting")

    object DefaultSettings : MessageSpec<DefaultSettingsContext>(id = "default_settings")
}

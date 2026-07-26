package ru.illine.drinking.ponies.model.base

enum class NotificationHistoryStatus(
    val eventType: AnswerNotificationType,
) {
    CONFIRMED(AnswerNotificationType.YES),
    MISSED(AnswerNotificationType.CANCEL),
    ;

    companion object {
        // Event types without a status here are not journal entries at all: SNOOZE is such a case.
        fun of(eventType: AnswerNotificationType): NotificationHistoryStatus? =
            entries.find { it.eventType == eventType }
    }
}

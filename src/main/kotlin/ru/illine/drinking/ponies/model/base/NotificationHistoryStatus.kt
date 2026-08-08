package ru.illine.drinking.ponies.model.base

enum class NotificationHistoryStatus(
    val eventType: AnswerNotificationType,
) {
    CONFIRMED(AnswerNotificationType.YES),
    MISSED(AnswerNotificationType.CANCEL),
    ;

    companion object {
        fun of(eventType: AnswerNotificationType): NotificationHistoryStatus? =
            entries.find { it.eventType == eventType }
    }
}

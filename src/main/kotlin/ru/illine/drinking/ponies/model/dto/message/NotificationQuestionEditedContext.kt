package ru.illine.drinking.ponies.model.dto.message

data class NotificationQuestionEditedContext(
    val answerDisplayName: String,
) : MessageContext

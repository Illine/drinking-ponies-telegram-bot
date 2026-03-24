package ru.illine.drinking.ponies.model.base

import org.junit.jupiter.api.DisplayName

@DisplayName("AnswerNotificationType Unit Test")
class AnswerNotificationTypeTest : EnumTypeOfTest<AnswerNotificationType>(
    AnswerNotificationType.entries,
    AnswerNotificationType::typeOf,
    AnswerNotificationType::queryData
)

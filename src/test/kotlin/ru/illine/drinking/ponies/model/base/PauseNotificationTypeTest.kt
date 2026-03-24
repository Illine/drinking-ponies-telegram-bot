package ru.illine.drinking.ponies.model.base

import org.junit.jupiter.api.DisplayName

@DisplayName("PauseNotificationType Unit Test")
class PauseNotificationTypeTest : EnumTypeOfTest<PauseNotificationType>(
    PauseNotificationType.entries,
    PauseNotificationType::typeOf,
    PauseNotificationType::queryData
)

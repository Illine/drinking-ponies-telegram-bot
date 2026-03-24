package ru.illine.drinking.ponies.model.base

import org.junit.jupiter.api.DisplayName

@DisplayName("SnoozeNotificationType Unit Test")
class SnoozeNotificationTypeTest : EnumTypeOfTest<SnoozeNotificationType>(
    SnoozeNotificationType.entries,
    SnoozeNotificationType::typeOf,
    SnoozeNotificationType::queryData
)

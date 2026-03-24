package ru.illine.drinking.ponies.model.base

import org.junit.jupiter.api.DisplayName

@DisplayName("IntervalNotificationType Unit Test")
class IntervalNotificationTypeTest : EnumTypeOfTest<IntervalNotificationType>(
    IntervalNotificationType.entries,
    IntervalNotificationType::typeOf,
    IntervalNotificationType::queryData
)

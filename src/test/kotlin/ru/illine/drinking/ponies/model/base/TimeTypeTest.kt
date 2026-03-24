package ru.illine.drinking.ponies.model.base

import org.junit.jupiter.api.DisplayName

@DisplayName("TimeType Unit Test")
class TimeTypeTest : EnumTypeOfTest<TimeType>(
    TimeType.entries,
    TimeType::typeOf,
    TimeType::queryData
)

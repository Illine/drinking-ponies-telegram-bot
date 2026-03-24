package ru.illine.drinking.ponies.model.base

import org.junit.jupiter.api.DisplayName

@DisplayName("QuietModeType Unit Test")
class QuietModeTypeTest : EnumTypeOfTest<QuietModeType>(
    QuietModeType.entries,
    QuietModeType::typeOf,
    QuietModeType::queryData
)

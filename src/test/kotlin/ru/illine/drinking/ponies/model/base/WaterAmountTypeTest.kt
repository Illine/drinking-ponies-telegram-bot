package ru.illine.drinking.ponies.model.base

import org.junit.jupiter.api.DisplayName

@DisplayName("WaterAmountType Unit Test")
class WaterAmountTypeTest : EnumTypeOfTest<WaterAmountType>(
    WaterAmountType.entries,
    WaterAmountType::typeOf,
    WaterAmountType::queryData
)

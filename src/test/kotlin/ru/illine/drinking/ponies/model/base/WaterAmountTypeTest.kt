package ru.illine.drinking.ponies.model.base

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("WaterAmountType Unit Test")
class WaterAmountTypeTest :
    EnumTypeOfTest<WaterAmountType>(
        WaterAmountType.entries,
        WaterAmountType::typeOf,
        WaterAmountType::queryData,
    ) {
    @Test
    @DisplayName("entries: amounts are distinct and declared in ascending order")
    fun `amounts are distinct and ascending`() {
        val amounts = WaterAmountType.entries.map { it.amountMl }

        assertEquals(amounts.distinct().sorted(), amounts)
    }
}

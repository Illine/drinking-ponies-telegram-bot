package ru.illine.drinking.ponies.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import ru.illine.drinking.ponies.test.tag.UnitTest

@UnitTest
@DisplayName("PluralizationHelper Unit Test")
class PluralizationHelperTest {
    @ParameterizedTest(name = "[{index}] n={0} -> {1}")
    @CsvSource(
        "1,         день",
        "21,        день",
        "31,        день",
        "101,       день",
        "1001,      день",
        "2,         дня",
        "3,         дня",
        "4,         дня",
        "22,        дня",
        "24,        дня",
        "33,        дня",
        "104,       дня",
        "0,         дней",
        "5,         дней",
        "10,        дней",
        "11,        дней",
        "12,        дней",
        "13,        дней",
        "14,        дней",
        "15,        дней",
        "20,        дней",
        "25,        дней",
        "100,       дней",
        "111,       дней",
        "112,       дней",
        "113,       дней",
        "114,       дней",
        "366,       дней",
        "-1,        день",
        "-2,        дня",
        "-5,        дней",
        "-11,       дней",
        "-21,       день",
    )
    fun `pluralizeDays returns russian form by mod100 and mod10 rules`(
        n: Int,
        expected: String,
    ) {
        val result = PluralizationHelper.pluralizeDays(n)

        assertEquals(expected, result)
    }
}

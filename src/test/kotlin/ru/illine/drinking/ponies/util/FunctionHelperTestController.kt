package ru.illine.drinking.ponies.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.FunctionHelper.catchAny
import ru.illine.drinking.ponies.util.FunctionHelper.catchAnyWithReturn
import ru.illine.drinking.ponies.util.FunctionHelper.check

@UnitTest
@DisplayName("FunctionHelper Unit Test")
class FunctionHelperTestController {


    // check

    @Test
    @DisplayName("check(): returns necessary value when true")
    fun `successful check true`() {
        val expected = "true"
        val actual = true.check(
            ifTrue = { "true" },
            ifFalse = { "false" }
        )

        assertEquals(expected, actual)
    }

    @Test
    @DisplayName("check(): returns necessary value when false")
    fun `successful check false`() {
        val expected = "false"
        val actual = false.check(
            ifTrue = { "true" },
            ifFalse = { "false" }
        )

        assertEquals(expected, actual)
    }

    // catchAny

    @Test
    @DisplayName("catchAny(): doesn't executes ifException when there isn't any exception")
    fun `successful catchAny action`() {
        var expected = false

        catchAny(
            action = {  },
            ifException = { expected = true },
            errorLogging = { /* No-op for test */ }
        )

        assertFalse(expected)
    }

    @Test
    @DisplayName("catchAny(): executes ifException when any exception")
    fun `successful catchAny ifException`() {
        var expected = false

        catchAny(
            action = { throw RuntimeException() },
            ifException = { expected = true },
            errorLogging = { /* No-op for test */ }
        )

        assertTrue(expected)
    }

    @Test
    @DisplayName("catchAny(): executes errorLogging when any exception")
    fun `successful catchAny errorLogging`() {
        var expected = false

        catchAny(
            action = { throw RuntimeException() },
            ifException = { /* No-op for test */ },
            errorLogging = { expected = true }
        )

        assertTrue(expected)
    }

    // catchAnyWithReturn

    @Test
    @DisplayName("catchAnyWithReturn(): doesn't executes ifException when there isn't any exception")
    fun `successful catchAnyWithReturn action`() {

        val expected = true

        val actual: Boolean? = catchAnyWithReturn(
            action = { true },
            ifException = { false },
            errorLogging = { /* No-op for test */ }
        )

        assertEquals(expected, actual)
    }

    @Test
    @DisplayName("catchAnyWithReturn(): executes ifException when any exception")
    fun `successful catchAnyWithReturn ifException`() {

        val expected = false

        val actual: Boolean? = catchAnyWithReturn(
            action = { throw RuntimeException() },
            ifException = { false },
            errorLogging = { /* No-op for test */ }
        )

        assertEquals(expected, actual)
    }

    @Test
    @DisplayName("catchAnyWithReturn(): executes errorLogging when any exception")
    fun `successful catchAnyWithReturn errorLogging`() {
        var expected = false

        catchAnyWithReturn(
            action = { throw RuntimeException() },
            ifException = { true },
            errorLogging = { expected = true }
        )

        assertTrue(expected)
    }
}
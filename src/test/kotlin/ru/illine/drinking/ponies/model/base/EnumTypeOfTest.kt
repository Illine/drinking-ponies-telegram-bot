package ru.illine.drinking.ponies.model.base

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import ru.illine.drinking.ponies.test.tag.UnitTest
import java.util.*

@UnitTest
abstract class EnumTypeOfTest<T>(
    private val entries: List<T>,
    private val typeOf: (String) -> T?,
    private val queryData: (T) -> UUID
) {

    @Test
    fun `typeOf returns entry for known queryData`() {
        entries.forEach { entry ->
            assertEquals(entry, typeOf(queryData(entry).toString()))
        }
    }

    @Test
    fun `typeOf returns null for unknown queryData`() {
        assertNull(typeOf("00000000-0000-0000-0000-000000000000"))
    }

    @Test
    fun `typeOf returns null for invalid queryData`() {
        assertNull(typeOf("not-a-uuid"))
    }
}

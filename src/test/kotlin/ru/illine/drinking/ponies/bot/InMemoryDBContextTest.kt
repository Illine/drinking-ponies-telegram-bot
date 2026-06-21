package ru.illine.drinking.ponies.bot

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.illine.drinking.ponies.test.tag.UnitTest

@UnitTest
@DisplayName("InMemoryDBContext Unit Test")
class InMemoryDBContextTest {
    private lateinit var db: InMemoryDBContext

    @BeforeEach
    fun setUp() {
        db = InMemoryDBContext()
    }

    @Test
    @DisplayName("getMap(): returns the same live map for the same name")
    fun `getMap returns same instance`() {
        val first = db.getMap<String, Int>("counters")
        first["a"] = 1
        val second = db.getMap<String, Int>("counters")

        assertSame(first, second)
        assertEquals(1, second["a"])
    }

    @Test
    @DisplayName("getSet(): returns the same live set for the same name")
    fun `getSet returns same instance`() {
        val first = db.getSet<Long>("admins")
        first.add(42L)
        val second = db.getSet<Long>("admins")

        assertSame(first, second)
        assertTrue(second.contains(42L))
    }

    @Test
    @DisplayName("getList(): returns the same live list for the same name")
    fun `getList returns same instance`() {
        val first = db.getList<String>("events")
        first.add("started")
        val second = db.getList<String>("events")

        assertSame(first, second)
        assertEquals(listOf("started"), second.toList())
    }

    @Test
    @DisplayName("getVar(): persists the value across lookups")
    fun `getVar persists value`() {
        db.getVar<String>("token").set("secret")

        assertEquals("secret", db.getVar<String>("token").get())
    }

    @Test
    @DisplayName("contains(): true only after a structure is created")
    fun `contains reflects created structures`() {
        assertFalse(db.contains("users"))

        db.getMap<Long, String>("users")

        assertTrue(db.contains("users"))
    }

    @Test
    @DisplayName("clear(): empties structures but keeps them registered")
    fun `clear empties structures but keeps them registered`() {
        db.getMap<String, Int>("counters")["a"] = 1
        db.getSet<Long>("admins").add(42L)

        db.clear()

        assertTrue(db.getMap<String, Int>("counters").isEmpty())
        assertTrue(db.getSet<Long>("admins").isEmpty())
        assertTrue(db.contains("counters"))
    }

    @Test
    @DisplayName("backup()/recover(): round-trips the stored structures")
    fun `backup and recover round-trip`() {
        db.getMap<String, Int>("counters")["a"] = 1

        val restored = InMemoryDBContext()
        val recovered = restored.recover(db.backup())

        assertTrue(recovered)
        assertEquals(1, restored.getMap<String, Int>("counters")["a"])
    }

    @Test
    @DisplayName("info(): reports type and size of a structure")
    fun `info reports structure type and size`() {
        db.getSet<Long>("admins").add(42L)

        assertEquals("admins - Set - 1", db.info("admins"))
    }
}

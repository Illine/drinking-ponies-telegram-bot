package ru.illine.drinking.ponies.bot

import org.telegram.telegrambots.abilitybots.api.db.DBContext
import org.telegram.telegrambots.abilitybots.api.db.Var
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Heap-backed [DBContext] for [AbilityBot]. The bot keeps its state in PostgreSQL and never reads the
 * ability db, so this stub replaces the default MapDB-based context.
 */
@Suppress("TooManyFunctions")
class InMemoryDBContext : DBContext {
    private val structures = ConcurrentHashMap<String, Any>()

    @Suppress("UNCHECKED_CAST")
    override fun <T> getList(name: String): MutableList<T> =
        structures.computeIfAbsent(name) { Collections.synchronizedList(mutableListOf<T>()) } as MutableList<T>

    @Suppress("UNCHECKED_CAST")
    override fun <K, V> getMap(name: String): MutableMap<K, V> =
        structures.computeIfAbsent(name) { ConcurrentHashMap<K & Any, V & Any>() } as MutableMap<K, V>

    @Suppress("UNCHECKED_CAST")
    override fun <T> getSet(name: String): MutableSet<T> =
        structures.computeIfAbsent(name) { ConcurrentHashMap.newKeySet<T>() } as MutableSet<T>

    @Suppress("UNCHECKED_CAST")
    override fun <T> getVar(name: String): Var<T> = structures.computeIfAbsent(name) { InMemoryVar<T>() } as Var<T>

    override fun summary(): String = structures.keys.joinToString("\n", transform = ::info)

    override fun backup(): Any = HashMap(structures)

    override fun recover(backup: Any): Boolean {
        if (backup !is Map<*, *>) return false
        structures.clear()
        backup.forEach { (key, value) -> if (key is String && value != null) structures[key] = value }
        return true
    }

    override fun info(name: String): String {
        val structure = structures[name] ?: error("DB structure with name [$name] does not exist")
        val description =
            when (structure) {
                is Set<*> -> "Set - ${structure.size}"
                is List<*> -> "List - ${structure.size}"
                is Map<*, *> -> "Map - ${structure.size}"
                else -> "Var"
            }
        return "$name - $description"
    }

    override fun commit() = Unit

    override fun clear() {
        structures.values.forEach { structure ->
            when (structure) {
                is MutableCollection<*> -> structure.clear()
                is MutableMap<*, *> -> structure.clear()
            }
        }
    }

    override fun contains(name: String): Boolean = structures.containsKey(name)

    override fun close() = Unit

    private class InMemoryVar<T> : Var<T> {
        @Volatile
        private var value: Any? = null

        @Suppress("UNCHECKED_CAST")
        override fun get(): T = value as T

        override fun set(value: T) {
            this.value = value
        }
    }
}

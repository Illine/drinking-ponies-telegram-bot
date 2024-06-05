package ru.illine.drinking.ponies.model.base

import java.util.*

@Suppress("unused")
enum class QuiteModeType(
    val displayName: String,
    val times: Set<TimeType>,
    val queryData: UUID
) {
    START(
        "Начало тихого режима",
        TimeType.values().toSet(),
        UUID.fromString("d273aabe-e37e-4ee2-b322-215128ece23c")
    ),
    END(
        "Конец тихого режима",
        TimeType.values().toSet(),
        UUID.fromString("4abac12a-7b7e-4a90-8151-3ff4b2c047ca")
    ),
    DISABLED(
        "Отключение тихого режима",
        TimeType.values().toSet(),
        UUID.fromString("c0a00d0e-b535-43a9-8235-ed303bcb5cd4")
    );

    companion object {

        fun typeOf(queryData: String): QuiteModeType? {
            return QuiteModeType.values()
                .find { Objects.equals(queryData, it.queryData.toString()) }
        }

    }
}
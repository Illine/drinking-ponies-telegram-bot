package ru.illine.drinking.ponies.model.base

import java.util.*

@Suppress("unused")
enum class AnswerNotificationType(
    val displayName: String,
    val queryData: UUID
) {

    YES("Да", UUID.fromString("1906562b-d065-4cb9-bfe2-2150f62cb053")),
    DELAY("Отложить", UUID.fromString("3d1122fd-f091-4fa7-ae0c-731f8d203e9f")),
    CANCEL("Отменить", UUID.fromString("16854759-b421-44bb-acee-51b4e5140a1a"));

    companion object {

        fun typeOf(queryData: String): AnswerNotificationType? {
            return AnswerNotificationType.values()
                .find { Objects.equals(queryData, it.queryData.toString()) }
        }

    }

}
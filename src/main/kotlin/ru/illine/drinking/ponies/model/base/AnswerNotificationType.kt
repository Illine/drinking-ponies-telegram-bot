package ru.illine.drinking.ponies.model.base

import java.util.*

@Suppress("unused")
enum class AnswerNotificationType(
    val displayName: String,
    val queryData: UUID
) {

    YES("Да", UUID.randomUUID()),
    DELAY("Отложить", UUID.randomUUID()),
    CANCEL("Отменить", UUID.randomUUID());

    companion object {

        fun typeOf(queryData: String): AnswerNotificationType? {
            return EnumSet.allOf(AnswerNotificationType::class.java)
                .find { Objects.equals(queryData, it.queryData.toString()) }
        }

    }

}
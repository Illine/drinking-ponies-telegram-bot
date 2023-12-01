package ru.illine.drinking.ponies.model.base

import java.util.EnumSet

enum class AnswerNotificationType(
    val displayName: String
) {

    YES("Да"),
    NO("Нет");

    companion object {

        val BUTTON_IDS =
            EnumSet.allOf(AnswerNotificationType::class.java)
                .stream()
                .map { it.name }
                .toList()

    }

}
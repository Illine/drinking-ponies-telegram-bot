package ru.illine.drinking.ponies.util.message.templates

import ru.illine.drinking.ponies.model.dto.internal.DefaultSettingsContext
import ru.illine.drinking.ponies.util.message.RuleBucket
import ru.illine.drinking.ponies.util.message.singlePhrase

object LocalDefaultSettings {
    val PHRASES: List<RuleBucket<DefaultSettingsContext>> =
        singlePhrase {
            """
            Установлены настройки по-умолчанию:
            Периодичность уведомлений: ${it.intervalDisplayName}
            Часовой пояс: Москва
            Время тихого режима: с 23:00 до 11:00
            """.trimIndent()
        }
}

package ru.illine.drinking.ponies.util.message.templates

import ru.illine.drinking.ponies.model.dto.message.GreetingContext
import ru.illine.drinking.ponies.util.message.RuleBucket
import ru.illine.drinking.ponies.util.message.singlePhrase

object LocalGreeting {
    val PHRASES: List<RuleBucket<GreetingContext>> =
        singlePhrase {
            """
            Здравствуй, ${it.userName}!
            Я бот Пьющие Поняшки. Я полностью понимающий и знаю, что всем нужно пить!
            """.trimIndent()
        }
}

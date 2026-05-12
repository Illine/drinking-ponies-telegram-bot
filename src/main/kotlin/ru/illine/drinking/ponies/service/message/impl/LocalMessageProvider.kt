package ru.illine.drinking.ponies.service.message.impl

import org.springframework.stereotype.Service
import ru.illine.drinking.ponies.model.dto.message.MessageContext
import ru.illine.drinking.ponies.model.dto.message.MessageDto
import ru.illine.drinking.ponies.service.message.MessageProvider
import ru.illine.drinking.ponies.util.message.MessageSpec
import ru.illine.drinking.ponies.util.message.TemplateRule
import ru.illine.drinking.ponies.util.message.templates.LocalInsightStats
import kotlin.random.Random

@Service
class LocalMessageProvider(
    private val random: Random
) : MessageProvider {

    private val rulesBySpec: Map<MessageSpec<*>, List<TemplateRule<*>>> =
        mapOf(MessageSpec.InsightStats to LocalInsightStats.RULES)

    override fun <C : MessageContext> getMessage(spec: MessageSpec<C>, context: C): MessageDto {
        @Suppress("UNCHECKED_CAST")
        val rules = rulesBySpec[spec] as? List<TemplateRule<C>>
            ?: error("No local templates registered for message spec [${spec.id}]")
        val rule = rules.first { it.predicate(context) }
        return MessageDto(text = rule.templates.random(random)(context))
    }

}

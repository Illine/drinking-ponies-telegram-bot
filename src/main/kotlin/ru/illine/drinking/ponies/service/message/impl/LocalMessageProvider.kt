package ru.illine.drinking.ponies.service.message.impl

import org.springframework.stereotype.Service
import ru.illine.drinking.ponies.exception.MessageTemplateException
import ru.illine.drinking.ponies.model.dto.message.MessageContext
import ru.illine.drinking.ponies.model.dto.message.MessageDto
import ru.illine.drinking.ponies.service.message.MessageProvider
import ru.illine.drinking.ponies.util.message.MessageSpec
import ru.illine.drinking.ponies.util.message.RuleBucket
import ru.illine.drinking.ponies.util.message.templates.LocalInsightStats
import kotlin.random.Random

@Service
class LocalMessageProvider(
    private val random: Random
) : MessageProvider {

    private val rulesBySpec: Map<MessageSpec<*>, List<RuleBucket<*>>> =
        mapOf(MessageSpec.InsightStats to LocalInsightStats.PHRASES)

    override fun <C : MessageContext> getMessage(spec: MessageSpec<C>, context: C): MessageDto {
        @Suppress("UNCHECKED_CAST")
        val buckets = rulesBySpec[spec] as? List<RuleBucket<C>>
            ?: throw MessageTemplateException("No local templates registered for message spec [${spec.id}]")
        // Guarantee comes from a `{ true }` fallback bucket placed after the specific ones.
        // Explicit error makes a missing fallback fail loudly instead of throwing a bare NoSuchElementException.
        val candidates = buckets.firstNotNullOfOrNull { bucket ->
            bucket.filter { it.predicate(context) }.takeIf { it.isNotEmpty() }
        } ?: throw MessageTemplateException(
            "No matching rule for message spec [${spec.id}] - is the fallback bucket missing?"
        )
        val rule = candidates.random(random)
        return MessageDto(text = rule.templates.random(random)(context))
    }

}

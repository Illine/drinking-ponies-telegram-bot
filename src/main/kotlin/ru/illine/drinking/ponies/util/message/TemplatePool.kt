package ru.illine.drinking.ponies.util.message

import ru.illine.drinking.ponies.model.dto.internal.MessageContext

/**
 * A pool with one always-matching phrase - the common shape for static or single-substitution
 * messages. The `{ true }` rule also serves as the mandatory fallback the provider requires.
 * Multi-phrase pools with predicate buckets stay on the explicit structure (see LocalInsightStats).
 */
fun <C : MessageContext> singlePhrase(template: (C) -> String): List<RuleBucket<C>> =
    listOf(listOf(TemplateRule(predicate = { true }, templates = listOf(template))))

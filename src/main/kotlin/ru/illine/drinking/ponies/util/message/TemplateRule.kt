package ru.illine.drinking.ponies.util.message

import ru.illine.drinking.ponies.model.dto.message.MessageContext

// Rules with equal priority - the provider picks first non-empty bucket, then random within it.
typealias RuleBucket<C> = List<TemplateRule<C>>

data class TemplateRule<C : MessageContext>(
    val predicate: (C) -> Boolean,
    val templates: List<(C) -> String>
) {

    init {
        require(templates.isNotEmpty()) { "TemplateRule must have at least one template" }
    }

}

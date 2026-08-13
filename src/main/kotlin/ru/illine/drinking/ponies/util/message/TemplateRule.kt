package ru.illine.drinking.ponies.util.message

import ru.illine.drinking.ponies.model.dto.internal.MessageContext

typealias RuleBucket<C> = List<TemplateRule<C>>

data class TemplateRule<C : MessageContext>(
    val predicate: (C) -> Boolean,
    val templates: List<(C) -> String>,
) {
    init {
        require(templates.isNotEmpty()) { "TemplateRule must have at least one template" }
    }
}

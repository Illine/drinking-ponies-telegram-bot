package ru.illine.drinking.ponies.service.message

import ru.illine.drinking.ponies.model.dto.internal.MessageContext
import ru.illine.drinking.ponies.model.dto.internal.MessageDto
import ru.illine.drinking.ponies.util.message.MessageSpec

interface MessageProvider {
    fun <C : MessageContext> getMessage(
        spec: MessageSpec<C>,
        context: C,
    ): MessageDto
}

package ru.illine.drinking.ponies.service.button.impl

import org.springframework.stereotype.Service
import ru.illine.drinking.ponies.service.button.ReplyButtonFactory
import ru.illine.drinking.ponies.service.button.ReplyButtonStrategy

@Service
class ReplyButtonFactoryImpl(
    private val strategies: List<ReplyButtonStrategy>
) : ReplyButtonFactory {

    override fun getStrategy(queryData: String): ReplyButtonStrategy {
        return strategies.find { it.isQueryData(queryData) }
            ?: throw IllegalArgumentException("Unknown query data: $queryData")
    }
}

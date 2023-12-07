package ru.illine.drinking.ponies.service.impl

import org.springframework.stereotype.Service
import ru.illine.drinking.ponies.service.ReplayButtonFactory
import ru.illine.drinking.ponies.service.ReplyButtonStrategy

@Service
class ReplayButtonFactoryImpl(
    private val strategies: List<ReplyButtonStrategy>
) : ReplayButtonFactory {

    override fun getStrategy(queryData: String): ReplyButtonStrategy {
        return requireNotNull(
            strategies.find { it.isQueryData(queryData) }
        )
    }
}
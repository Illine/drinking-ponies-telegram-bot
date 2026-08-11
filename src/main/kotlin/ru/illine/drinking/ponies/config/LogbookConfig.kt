package ru.illine.drinking.ponies.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.zalando.logbook.Correlation
import org.zalando.logbook.HttpLogWriter
import org.zalando.logbook.Precorrelation
import ru.illine.drinking.ponies.model.base.AppLogger

@Configuration
class LogbookConfig {
    @Bean
    fun writer() = DefaultHttpLogWriter()

    class DefaultHttpLogWriter : HttpLogWriter {
        private val logger = AppLogger.API.logger

        override fun isActive(): Boolean = logger.isInfoEnabled

        override fun write(
            precorrelation: Precorrelation,
            request: String,
        ) {
            logger.info(request)
        }

        override fun write(
            correlation: Correlation,
            response: String,
        ) {
            logger.info(response)
        }
    }
}

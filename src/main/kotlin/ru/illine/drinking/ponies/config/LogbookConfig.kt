package ru.illine.drinking.ponies.config

import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.zalando.logbook.Correlation
import org.zalando.logbook.HttpLogWriter
import org.zalando.logbook.Logbook
import org.zalando.logbook.Precorrelation
import org.zalando.logbook.httpclient.LogbookHttpRequestInterceptor
import org.zalando.logbook.httpclient.LogbookHttpResponseInterceptor
import ru.illine.drinking.ponies.config.property.LogbookProperties

@Configuration
class LogbookConfig {

    @Bean
    fun telegramLogbookRequestInterceptor(logbook: Logbook) = LogbookHttpRequestInterceptor(logbook)

    @Bean
    fun telegramLogbookResponseInterceptor() = LogbookHttpResponseInterceptor()

    @Bean
    fun writer(properties: LogbookProperties) = DefaultHttpLogWriter(properties)

    class DefaultHttpLogWriter(properties: LogbookProperties) : HttpLogWriter {
        private val logger: Logger

        override fun isActive(): Boolean {
            return logger.isInfoEnabled
        }

        override fun write(precorrelation: Precorrelation, request: String) {
            logger.info(request)
        }

        override fun write(correlation: Correlation, response: String) {
            logger.info(response)
        }

        init {
            logger = LoggerFactory.getILoggerFactory().getLogger(properties.name) as Logger
        }
    }
}

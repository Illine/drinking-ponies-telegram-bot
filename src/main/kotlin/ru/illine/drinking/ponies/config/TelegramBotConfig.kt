package ru.illine.drinking.ponies.config

import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.generics.BotSession
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import org.zalando.logbook.httpclient.LogbookHttpRequestInterceptor
import org.zalando.logbook.httpclient.LogbookHttpResponseInterceptor
import ru.illine.drinking.ponies.bot.DrinkingPoniesTelegramBot
import ru.illine.drinking.ponies.config.property.TelegramBotProperties
import ru.illine.drinking.ponies.util.TelegramBotHelper
import java.util.concurrent.TimeUnit

@Configuration
class TelegramBotConfig {

    @Bean
    fun telegramHttpClient(
        telegramBotProperties: TelegramBotProperties,
        telegramLogbookRequestInterceptor: LogbookHttpRequestInterceptor,
        telegramLogbookResponseInterceptor: LogbookHttpResponseInterceptor
    ): CloseableHttpClient {
        return HttpClientBuilder.create()
            .setSSLHostnameVerifier(NoopHostnameVerifier())
            .setConnectionTimeToLive(telegramBotProperties.http.connectionTimeToLiveInSec, TimeUnit.SECONDS)
            .setMaxConnTotal(telegramBotProperties.http.maxConnectionTotal)
            .addInterceptorFirst(telegramLogbookRequestInterceptor)
            .addInterceptorFirst(telegramLogbookResponseInterceptor)
            .build()
    }

    @Bean
    fun telegramBotApi() = TelegramBotsApi(DefaultBotSession::class.java)

    @Bean(destroyMethod = "stop")
    fun drinkingPoniesTelegramBotSession(
        telegramBotsApi: TelegramBotsApi,
        drinkingPoniesTelegramBot: DrinkingPoniesTelegramBot,
        telegramHttpClient: CloseableHttpClient
    ): BotSession {
        return telegramBotsApi.registerBot(drinkingPoniesTelegramBot)
            .apply { TelegramBotHelper.replaceBotSessionHttpClient(this, telegramHttpClient) }
    }
}
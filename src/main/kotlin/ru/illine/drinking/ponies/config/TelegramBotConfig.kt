package ru.illine.drinking.ponies.config

import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.abilitybots.api.bot.BaseAbilityBot
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import org.telegram.telegrambots.meta.generics.TelegramClient
import org.zalando.logbook.Logbook
import org.zalando.logbook.okhttp.LogbookInterceptor
import ru.illine.drinking.ponies.bot.DrinkingPoniesTelegramBot
import ru.illine.drinking.ponies.config.property.TelegramBotProperties
import ru.illine.drinking.ponies.service.CommandService
import ru.illine.drinking.ponies.service.NotificationService
import ru.illine.drinking.ponies.service.button.ReplayButtonFactory
import java.util.concurrent.TimeUnit

@Configuration
class TelegramBotConfig {

    @Bean(destroyMethod = "stop")
    fun telegramBotsApplication(
        abilityBot: BaseAbilityBot,
        telegramBotProperties: TelegramBotProperties,
    ): TelegramBotsLongPollingApplication {
        return TelegramBotsLongPollingApplication()
            .apply {
                this.registerBot(telegramBotProperties.token, abilityBot)
            }
    }

    @Bean(initMethod = "onRegister")
    fun abilityBot(
        telegramClient: TelegramClient,
        telegramBotProperties: TelegramBotProperties,
        notificationService: NotificationService,
        replayButtonFactory: ReplayButtonFactory,
        commandService: CommandService
    ): BaseAbilityBot {
        return DrinkingPoniesTelegramBot(
            telegramClient,
            telegramBotProperties,
            notificationService,
            replayButtonFactory,
            commandService
        )
    }

    @Bean
    fun telegramClient(
        telegramBotProperties: TelegramBotProperties,
        okHttpClient: OkHttpClient
    ): TelegramClient {
        return OkHttpTelegramClient(okHttpClient, telegramBotProperties.token)
    }

    @Bean
    fun okHttpClient(
        connectionPool: ConnectionPool,
        logbook: Logbook
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addNetworkInterceptor(LogbookInterceptor(logbook))
            .connectionPool(connectionPool)
            .build()
    }

    @Bean
    fun connectionPool(
        telegramBotProperties: TelegramBotProperties
    ) = ConnectionPool(
        telegramBotProperties.http.maxConnectionTotal,
        telegramBotProperties.http.connectionTimeToLiveInSec,
        TimeUnit.SECONDS
    )
}
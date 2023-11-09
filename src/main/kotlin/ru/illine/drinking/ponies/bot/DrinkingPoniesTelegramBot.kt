package ru.illine.drinking.ponies.bot

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.objects.Update
import ru.illine.drinking.ponies.config.property.TelegramBotProperties
import ru.illine.drinking.ponies.dao.access.UserNotificationAccessService

@Component
class DrinkingPoniesTelegramBot(
    telegramBotProperties: TelegramBotProperties,
    private val userNotificationAccessService: UserNotificationAccessService
) : TelegramLongPollingBot(telegramBotProperties.token) {

    private val log = LoggerFactory.getLogger("BOT")

    override fun getBotUsername() = "Drinking Ponies"

    override fun onUpdateReceived(update: Update) {
        log.debug("Success")
    }

}
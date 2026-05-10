package ru.illine.drinking.ponies.service.command.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.menubutton.SetChatMenuButton
import org.telegram.telegrambots.meta.api.objects.menubutton.MenuButtonWebApp
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.config.property.TelegramBotProperties
import ru.illine.drinking.ponies.service.command.CommandService
import ru.illine.drinking.ponies.util.telegram.TelegramMenuConstants

@Service
class CommandServiceImpl(
    private val telegramBotProperties: TelegramBotProperties,
    private val sender: TelegramClient
) : CommandService {

    private val logger = LoggerFactory.getLogger("SERVICE")

    override fun register() {
        if (!telegramBotProperties.autoUpdateTelegramConfig) {
            logger.info("Telegram config will not be updated!")
            return
        }
        logger.warn("Telegram config will be updated!")

        registerMenu()

        logger.info("The Telegram config has been updated")
    }

    private fun registerMenu() {
        val menuButton = MenuButtonWebApp.builder()
            .text(TelegramMenuConstants.MENU_BUTTON_TEXT)
            .webAppInfo(WebAppInfo.builder().url(telegramBotProperties.miniAppUrl).build())
            .build()
        SetChatMenuButton.builder()
            .menuButton(menuButton)
            .build()
            .apply { sender.execute(this) }
    }
}

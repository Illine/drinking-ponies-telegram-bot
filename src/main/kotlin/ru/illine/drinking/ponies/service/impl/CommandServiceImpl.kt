package ru.illine.drinking.ponies.service.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.abilitybots.api.sender.MessageSender
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllPrivateChats
import ru.illine.drinking.ponies.config.property.TelegramBotProperties
import ru.illine.drinking.ponies.model.base.TelegramCommandType
import ru.illine.drinking.ponies.service.CommandService
import java.util.*

@Service
class CommandServiceImpl(
    private val telegramBotProperties: TelegramBotProperties,
    private val sender: MessageSender
) : CommandService {

    private val log = LoggerFactory.getLogger("SERVICE")

    override fun register() {
        if (telegramBotProperties.autoUpdateCommands) {
            log.warn("Telegram Commands will be updated!")
            val availabilityCommands = EnumSet.allOf(TelegramCommandType::class.java)
                .asIterable()
                .filter { it.visible }
                .sortedBy { it.order }
                .map { BotCommand(it.command, it.descriptions) }
                .toList()

            log.debug("Created commands: {}", availabilityCommands)

            availabilityCommands.let {
                SetMyCommands()
                    .apply {
                        commands = it
                        scope = BotCommandScopeAllPrivateChats()
                    }
            }.apply { sender.execute(this) }

            log.info("The commands have updated")
        } else {
            log.warn("Telegram Commands will not be updated!")
        }
    }
}
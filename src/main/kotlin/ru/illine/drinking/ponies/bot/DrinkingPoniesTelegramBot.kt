package ru.illine.drinking.ponies.bot

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import org.telegram.abilitybots.api.bot.AbilityBot
import org.telegram.abilitybots.api.bot.BaseAbilityBot
import org.telegram.abilitybots.api.db.MapDBContext.offlineInstance
import org.telegram.abilitybots.api.objects.*
import org.telegram.abilitybots.api.toggle.BareboneToggle
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllPrivateChats
import ru.illine.drinking.ponies.config.property.TelegramBotProperties
import ru.illine.drinking.ponies.model.base.TelegramCommandType
import ru.illine.drinking.ponies.service.NotificationService
import ru.illine.drinking.ponies.service.ReplayButtonFactory
import java.util.*
import java.util.function.BiConsumer


@Component
class DrinkingPoniesTelegramBot(
    private val telegramBotProperties: TelegramBotProperties,
    @Lazy private val notificationService: NotificationService,
    @Lazy private val replayButtonFactory: ReplayButtonFactory
) : AbilityBot(
    telegramBotProperties.token,
    telegramBotProperties.username,
    offlineInstance(telegramBotProperties.username),
    BareboneToggle()
) {

    private val log = LoggerFactory.getLogger("BOT")

    override fun creatorId() = telegramBotProperties.creatorId

    override fun onRegister() {
        super.onRegister()
        registerCommands()
    }

    @Suppress("unused")
    fun startCommand() =
        Ability
            .builder()
            .name(TelegramCommandType.START.command)
            .info(TelegramCommandType.START.info)
            .locality(Locality.USER)
            .privacy(Privacy.PUBLIC)
            .action { notificationService.start(it) }
            .build()

    @Suppress("unused")
    fun resumeCommand() =
        Ability
            .builder()
            .name(TelegramCommandType.RESUME.command)
            .info(TelegramCommandType.RESUME.info)
            .locality(Locality.USER)
            .privacy(Privacy.PUBLIC)
            .action { notificationService.resume(it) }
            .build()

    @Suppress("unused")
    fun stopCommand() =
        Ability
            .builder()
            .name(TelegramCommandType.STOP.command)
            .info(TelegramCommandType.STOP.info)
            .locality(Locality.USER)
            .privacy(Privacy.PUBLIC)
            .action { notificationService.stop(it) }
            .build()

    @Suppress("unused")
    fun settingsCommand() =
        Ability
            .builder()
            .name(TelegramCommandType.SETTINGS.command)
            .info(TelegramCommandType.SETTINGS.info)
            .locality(Locality.USER)
            .privacy(Privacy.PUBLIC)
            .action { notificationService.settings(it) }
            .build()

    @Suppress("unused")
    fun replyInlineButtons(): Reply {
        val action = BiConsumer<BaseAbilityBot, Update> { _, update ->
            replayButtonFactory.getStrategy(update.callbackQuery.data).reply(update.callbackQuery)
        }

        return Reply.of(action, Flag.CALLBACK_QUERY)
    }

    fun registerCommands() {
        if (telegramBotProperties.autoUpdateCommands) {
            log.warn("Telegram Commands will be updated!")
            EnumSet.allOf(TelegramCommandType::class.java)
                .stream()
                .filter { it.visible }
                .map { BotCommand(it.command, it.descriptions) }
                .toList()
                .let {
                    SetMyCommands()
                        .apply {
                            commands = it
                            scope = BotCommandScopeAllPrivateChats()
                        }
                }.apply { execute(this) }
        }
    }
}
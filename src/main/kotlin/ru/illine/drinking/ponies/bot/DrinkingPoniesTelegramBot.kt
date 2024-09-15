package ru.illine.drinking.ponies.bot

import org.telegram.telegrambots.abilitybots.api.bot.AbilityBot
import org.telegram.telegrambots.abilitybots.api.bot.BaseAbilityBot
import org.telegram.telegrambots.abilitybots.api.db.MapDBContext.offlineInstance
import org.telegram.telegrambots.abilitybots.api.objects.*
import org.telegram.telegrambots.abilitybots.api.toggle.BareboneToggle
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.config.property.TelegramBotProperties
import ru.illine.drinking.ponies.model.base.TelegramCommandType
import ru.illine.drinking.ponies.service.CommandService
import ru.illine.drinking.ponies.service.NotificationService
import ru.illine.drinking.ponies.service.button.ReplayButtonFactory
import java.util.function.BiConsumer


class DrinkingPoniesTelegramBot(
    telegramClient: TelegramClient,
    private val telegramBotProperties: TelegramBotProperties,
    private val notificationService: NotificationService,
    private val replayButtonFactory: ReplayButtonFactory,
    private val commandService: CommandService
) : AbilityBot(
    telegramClient,
    telegramBotProperties.username,
    offlineInstance(telegramBotProperties.username),
    BareboneToggle()
) {

    override fun creatorId() = telegramBotProperties.creatorId

    override fun onRegister() {
        super.onRegister()
        commandService.register()
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
    fun pauseCommand() =
        Ability
            .builder()
            .name(TelegramCommandType.PAUSE.command)
            .info(TelegramCommandType.PAUSE.info)
            .locality(Locality.USER)
            .privacy(Privacy.PUBLIC)
            .action { notificationService.pause(it) }
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
    fun versionCommand() =
        Ability
            .builder()
            .name(TelegramCommandType.VERSION.command)
            .info(TelegramCommandType.VERSION.info)
            .locality(Locality.USER)
            .privacy(Privacy.CREATOR)
            .action { silent.send(telegramBotProperties.version, it.chatId()) }
            .build()

    @Suppress("unused")
    fun replyInlineButtons(): Reply {
        val action = BiConsumer<BaseAbilityBot, Update> { _, update ->
            replayButtonFactory.getStrategy(update.callbackQuery.data).reply(update.callbackQuery)
        }
        return Reply.of(action, Flag.CALLBACK_QUERY)
    }
}
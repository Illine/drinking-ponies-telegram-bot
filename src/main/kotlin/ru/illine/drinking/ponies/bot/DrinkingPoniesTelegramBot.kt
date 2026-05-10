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
import ru.illine.drinking.ponies.service.button.ReplyButtonFactory
import ru.illine.drinking.ponies.service.command.CommandService
import ru.illine.drinking.ponies.service.notification.NotificationService
import java.util.function.BiConsumer


class DrinkingPoniesTelegramBot(
    telegramClient: TelegramClient,
    private val telegramBotProperties: TelegramBotProperties,
    private val notificationService: NotificationService,
    private val replyButtonFactory: ReplyButtonFactory,
    private val commandService: CommandService
) : AbilityBot(
    telegramClient,
    telegramBotProperties.username,
    offlineInstance(telegramBotProperties.username),
    BareboneToggle()
) {

    // Sentinel: Privacy.CREATOR is no longer used, but creatorId() is abstract in AbilityBot
    override fun creatorId(): Long = 0L

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
    fun replyInlineButtons(): Reply {
        val action = BiConsumer<BaseAbilityBot, Update> { _, update ->
            replyButtonFactory.getStrategy(update.callbackQuery.data).reply(update.callbackQuery)
        }
        return Reply.of(action, Flag.CALLBACK_QUERY)
    }
}

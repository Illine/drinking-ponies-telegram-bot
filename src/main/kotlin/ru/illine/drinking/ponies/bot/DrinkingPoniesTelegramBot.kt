package ru.illine.drinking.ponies.bot

import org.telegram.telegrambots.abilitybots.api.bot.AbilityBot
import org.telegram.telegrambots.abilitybots.api.bot.BaseAbilityBot
import org.telegram.telegrambots.abilitybots.api.objects.Ability
import org.telegram.telegrambots.abilitybots.api.objects.Flag
import org.telegram.telegrambots.abilitybots.api.objects.Locality
import org.telegram.telegrambots.abilitybots.api.objects.Privacy
import org.telegram.telegrambots.abilitybots.api.objects.Reply
import org.telegram.telegrambots.abilitybots.api.toggle.BareboneToggle
import org.telegram.telegrambots.abilitybots.api.util.AbilityUtils
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.config.property.TelegramBotProperties
import ru.illine.drinking.ponies.dao.access.TelegramUserAccessService
import ru.illine.drinking.ponies.model.base.AppLogger
import ru.illine.drinking.ponies.model.base.TelegramCommandType
import ru.illine.drinking.ponies.service.button.ReplyButtonFactory
import ru.illine.drinking.ponies.service.command.CommandService
import ru.illine.drinking.ponies.service.notification.NotificationService
import java.util.function.BiConsumer

class DrinkingPoniesTelegramBot(
    telegramClient: TelegramClient,
    telegramBotProperties: TelegramBotProperties,
    private val notificationService: NotificationService,
    private val replyButtonFactory: ReplyButtonFactory,
    private val commandService: CommandService,
    private val telegramUserAccessService: TelegramUserAccessService,
) : AbilityBot(
        telegramClient,
        telegramBotProperties.username,
        InMemoryDBContext(),
        BareboneToggle(),
    ) {
    private val logger = AppLogger.BOT.logger

    override fun creatorId(): Long = 0L

    // The single gate every update passes through, commands and callback queries alike.
    public override fun checkGlobalFlags(update: Update): Boolean {
        val externalUserId =
            try {
                AbilityUtils.getUser(update).id
            } catch (e: IllegalStateException) {
                logger.debug("An update without an originating user is left to the filters downstream: {}", e.message)
                return true
            }

        val banned = telegramUserAccessService.resolveAccessFlags(externalUserId).isBanned
        if (banned) {
            logger.warn("Banned externalUserId [{}] gets no answer", externalUserId)
        }

        return !banned
    }

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
        val action =
            BiConsumer<BaseAbilityBot, Update> { _, update ->
                replyButtonFactory.getStrategy(update.callbackQuery.data).reply(update.callbackQuery)
            }
        return Reply.of(action, Flag.CALLBACK_QUERY)
    }
}

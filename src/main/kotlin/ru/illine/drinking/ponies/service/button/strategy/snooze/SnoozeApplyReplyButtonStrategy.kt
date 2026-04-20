package ru.illine.drinking.ponies.service.button.strategy.snooze

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.SnoozeNotificationType
import ru.illine.drinking.ponies.service.button.ReplyButtonStrategy
import ru.illine.drinking.ponies.service.notification.NotificationSettingsService
import ru.illine.drinking.ponies.service.statistic.WaterStatisticService
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.util.TimeHelper
import ru.illine.drinking.ponies.util.telegram.TelegramMessageConstants
import java.time.Clock

@Service
class SnoozeApplyReplyButtonStrategy(
    private val sender: TelegramClient,
    private val notificationSettingsService: NotificationSettingsService,
    private val waterStatisticService: WaterStatisticService,
    private val messageEditorService: MessageEditorService,
    private val clock: Clock
) : ReplyButtonStrategy {

    private val logger = LoggerFactory.getLogger("STRATEGY")

    override fun reply(callbackQuery: CallbackQuery) {
        messageEditorService.deleteReplyMarkup(
            callbackQuery.message.chatId,
            callbackQuery.message.messageId
        )

        val userId = callbackQuery.from.id
        val chatId = callbackQuery.message.chatId
        val queryData = callbackQuery.data

        val snoozeType = SnoozeNotificationType.typeOf(queryData) ?: SnoozeNotificationType.TEN_MINS

        logger.info(
            "A telegram user [{}] for telegram chat [{}] will snooze notification for [{}] minutes",
            userId,
            chatId,
            snoozeType.minutes
        )

        val notificationSetting = notificationSettingsService.getNotificationSettings(userId)
        val nextNotificationTime =
            TimeHelper.nextNotificationTimeByNow(
                clock,
                notificationSetting.notificationInterval.minutes,
                snoozeType.minutes
            )

        notificationSettingsService.resetNotificationTimer(userId, nextNotificationTime)
            .also { setting ->
                runCatching {
                    waterStatisticService.recordEvent(setting.telegramUser, AnswerNotificationType.SNOOZE)
                }.onFailure { e ->
                    logger.error(
                        "Failed to record water statistic for user [{}]",
                        setting.telegramUser.externalUserId,
                        e
                    )
                }
            }

        SendMessage(
            chatId.toString(),
            TelegramMessageConstants.NOTIFICATION_SUSPEND_MESSAGE.format(snoozeType.displayName)
        ).apply { sender.execute(this) }
    }

    override fun isQueryData(queryData: String): Boolean = SnoozeNotificationType.typeOf(queryData) != null

}

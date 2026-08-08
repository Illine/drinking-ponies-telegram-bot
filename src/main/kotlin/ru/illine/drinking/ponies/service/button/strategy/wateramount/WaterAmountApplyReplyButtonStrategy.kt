package ru.illine.drinking.ponies.service.button.strategy.wateramount

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.WaterAmountType
import ru.illine.drinking.ponies.model.dto.internal.NoContext
import ru.illine.drinking.ponies.service.button.ReplyButtonStrategy
import ru.illine.drinking.ponies.service.message.MessageProvider
import ru.illine.drinking.ponies.service.notification.NotificationSettingsService
import ru.illine.drinking.ponies.service.statistic.WaterStatisticService
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.util.message.MessageSpec
import java.time.Clock
import java.time.LocalDateTime

@Service
class WaterAmountApplyReplyButtonStrategy(
    private val sender: TelegramClient,
    private val notificationSettingsService: NotificationSettingsService,
    private val waterStatisticService: WaterStatisticService,
    private val messageEditorService: MessageEditorService,
    private val clock: Clock,
    private val messageProvider: MessageProvider,
) : ReplyButtonStrategy {
    private val logger = LoggerFactory.getLogger("STRATEGY")

    override fun reply(callbackQuery: CallbackQuery) {
        messageEditorService.deleteReplyMarkup(
            callbackQuery.message.chatId,
            callbackQuery.message.messageId,
        )

        val externalUserId = callbackQuery.from.id
        val chatId = callbackQuery.message.chatId
        val queryData = callbackQuery.data

        var waterAmountType = WaterAmountType.typeOf(queryData)
        if (waterAmountType == null) {
            waterAmountType = WaterAmountType.ML_250
            logger.warn("Unknown an queryData: [{}], will be set up default value: [{}]", queryData, waterAmountType)
        }

        logger.info(
            "A telegram user [{}] for telegram chat [{}] drank [{}] ml of water",
            externalUserId,
            chatId,
            waterAmountType.amountMl,
        )

        notificationSettingsService
            .resetNotificationTimer(externalUserId, LocalDateTime.now(clock))
            .also { setting ->
                runCatching {
                    waterStatisticService.recordEvent(
                        setting.telegramUser,
                        AnswerNotificationType.YES,
                        waterAmountType.amountMl,
                    )
                }.onFailure { e ->
                    logger.error(
                        "Failed to record water statistic for user [{}]",
                        setting.telegramUser.externalUserId,
                        e,
                    )
                }
            }

        SendMessage(
            chatId.toString(),
            messageProvider.getMessage(MessageSpec.NotificationAnswerYes, NoContext).text,
        ).apply { sender.execute(this) }
    }

    override fun isQueryData(queryData: String): Boolean = WaterAmountType.typeOf(queryData) != null
}

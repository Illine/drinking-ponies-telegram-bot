package ru.illine.drinking.ponies.service.button.strategy.wateramount

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.WaterAmountType
import ru.illine.drinking.ponies.service.button.ReplyButtonStrategy
import ru.illine.drinking.ponies.service.statistic.WaterStatisticService
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.util.telegram.TelegramMessageConstants
import java.time.Clock
import java.time.LocalDateTime

@Service
class WaterAmountApplyReplyButtonStrategy(
    private val sender: TelegramClient,
    private val notificationAccessService: NotificationAccessService,
    private val waterStatisticService: WaterStatisticService,
    private val messageEditorService: MessageEditorService,
    private val clock: Clock
) : ReplyButtonStrategy {

    private val logger = LoggerFactory.getLogger("REPLY-STRATEGY")

    override fun reply(callbackQuery: CallbackQuery) {
        messageEditorService.deleteReplyMarkup(
            callbackQuery.message.chatId,
            callbackQuery.message.messageId
        )

        val userId = callbackQuery.from.id
        val chatId = callbackQuery.message.chatId
        val queryData = callbackQuery.data

        val waterAmountType = WaterAmountType.typeOf(queryData) ?: WaterAmountType.ML_250

        logger.info(
            "A telegram user [{}] for telegram chat [{}] drank [{}] ml of water",
            userId,
            chatId,
            waterAmountType.amountMl
        )

        val notificationSetting = notificationAccessService.updateTimeOfLastNotification(userId, LocalDateTime.now(clock))

        waterStatisticService.recordEvent(
            notificationSetting.telegramUser,
            AnswerNotificationType.YES,
            waterAmountType.amountMl
        )

        SendMessage(
            chatId.toString(),
            TelegramMessageConstants.NOTIFICATION_ANSWER_YES_MESSAGE
        ).apply { sender.execute(this) }
    }

    override fun isQueryData(queryData: String): Boolean = WaterAmountType.typeOf(queryData) != null

}

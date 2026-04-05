package ru.illine.drinking.ponies.service.button.strategy.notification

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.service.button.strategy.AbstractAnswerNotificationReplyButtonStrategy
import ru.illine.drinking.ponies.service.statistic.WaterStatisticService
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.util.telegram.TelegramMessageConstants
import java.time.Clock
import java.time.LocalDateTime

@Service
class CancelAnswerNotificationReplyButtonStrategy(
    sender: TelegramClient,
    messageEditorService: MessageEditorService,
    private val notificationAccessService: NotificationAccessService,
    private val waterStatisticService: WaterStatisticService,
    private val clock: Clock
) : AbstractAnswerNotificationReplyButtonStrategy<NotificationSettingDto>(sender, messageEditorService) {

    private val logger = LoggerFactory.getLogger("STRATEGY")

    override fun updateLastNotificationTime(callbackQuery: CallbackQuery): () -> NotificationSettingDto = {
        notificationAccessService.updateTimeOfLastNotification(callbackQuery.from.id, LocalDateTime.now(clock))
            .also { setting ->
                runCatching {
                    waterStatisticService.recordEvent(setting.telegramUser, getAnswerType())
                }.onFailure { e ->
                    logger.error(
                        "Failed to record water statistic for user [{}]",
                        setting.telegramUser.externalUserId,
                        e
                    )
                }
            }
    }

    override fun getMessageText(): String = TelegramMessageConstants.NOTIFICATION_ANSWER_CANCEL_MESSAGE

    override fun getAnswerType(): AnswerNotificationType = AnswerNotificationType.CANCEL
}
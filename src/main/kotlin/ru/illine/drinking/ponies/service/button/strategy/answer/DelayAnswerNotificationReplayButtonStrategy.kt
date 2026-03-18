package ru.illine.drinking.ponies.service.button.strategy.answer

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.service.MessageEditorService
import ru.illine.drinking.ponies.service.button.strategy.AbstractAnswerNotificationReplyButtonStrategy
import ru.illine.drinking.ponies.util.TelegramConstants

@Service
class DelayAnswerNotificationReplayButtonStrategy(
    sender: TelegramClient,
    messageEditorService: MessageEditorService,
    private val notificationAccessService: NotificationAccessService
) : AbstractAnswerNotificationReplyButtonStrategy<NotificationSettingDto>(sender, messageEditorService) {

    override fun updateLastNotificationTime(callbackQuery: CallbackQuery): () -> NotificationSettingDto = {
        val userId = callbackQuery.from.id
        val notificationSetting = notificationAccessService.findNotificationSettingByTelegramUserId(userId)
        notificationAccessService.updateTimeOfLastNotification(
            userId,
            notificationSetting.timeOfLastNotification.plusMinutes(10)
        )
    }

    override fun getMessageText(): String = TelegramConstants.NOTIFICATION_ANSWER_DELAY_MESSAGE

    override fun getAnswerType(): AnswerNotificationType = AnswerNotificationType.DELAY
}
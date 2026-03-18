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
import java.time.LocalDateTime

@Service
class YesAnswerNotificationReplayButtonStrategy(
    sender: TelegramClient,
    messageEditorService: MessageEditorService,
    private val notificationAccessService: NotificationAccessService
) : AbstractAnswerNotificationReplyButtonStrategy<NotificationSettingDto>(sender, messageEditorService) {

    override fun updateLastNotificationTime(callbackQuery: CallbackQuery): () -> NotificationSettingDto = {
        notificationAccessService.updateTimeOfLastNotification(callbackQuery.from.id, LocalDateTime.now())
    }

    override fun getMessageText(): String = TelegramConstants.NOTIFICATION_ANSWER_YES_MESSAGE

    override fun getAnswerType(): AnswerNotificationType = AnswerNotificationType.YES
}
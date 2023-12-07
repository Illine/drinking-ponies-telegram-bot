package ru.illine.drinking.ponies.service.impl.strategy

import org.springframework.stereotype.Service
import org.telegram.abilitybots.api.sender.MessageSender
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.dto.NotificationDto
import ru.illine.drinking.ponies.service.AbstractAnswerNotificationReplyButtonStrategy
import ru.illine.drinking.ponies.service.ButtonEditorService
import ru.illine.drinking.ponies.util.MessageHelper
import java.time.OffsetDateTime

@Service
class CancelAnswerNotificationReplayButtonStrategy(
    sender: MessageSender,
    buttonEditorService: ButtonEditorService,
    private val notificationAccessService: NotificationAccessService
) : AbstractAnswerNotificationReplyButtonStrategy<NotificationDto>(sender, buttonEditorService) {

    override fun updateLastNotificationTime(callbackQuery: CallbackQuery): () -> NotificationDto = {
        notificationAccessService.updateTimeOfLastNotification(callbackQuery.from.id, OffsetDateTime.now())
    }

    override fun getMessageText(): String = MessageHelper.NOTIFICATION_ANSWER_CANCEL_MESSAGE

    override fun getAnswerType(): AnswerNotificationType = AnswerNotificationType.CANCEL
}
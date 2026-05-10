package ru.illine.drinking.ponies.service.notification.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.abilitybots.api.objects.MessageContext
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramChatDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserDto
import ru.illine.drinking.ponies.service.notification.NotificationService
import ru.illine.drinking.ponies.util.FunctionHelper.check
import ru.illine.drinking.ponies.util.telegram.TelegramMessageConstants

@Service
class NotificationServiceImpl(
    private val sender: TelegramClient,
    private val notificationAccessService: NotificationAccessService
) : NotificationService {

    private val logger = LoggerFactory.getLogger("SERVICE")

    override fun start(messageContext: MessageContext) {
        SendMessage(
            messageContext.chatId().toString(),
            TelegramMessageConstants.START_GREETING_MESSAGE.format(messageContext.user().userName)
        ).apply { sender.execute(this) }

        val userId = messageContext.user().id
        val chatId = messageContext.chatId()

        val setting = notificationAccessService.existsByTelegramUserId(userId).check(
            ifTrue = {
                notificationAccessService.findNotificationSettingByTelegramUserId(userId)
            },
            ifFalse = {
                createNewUser(userId, chatId)
            }
        )

        SendMessage(
            messageContext.chatId().toString(),
            TelegramMessageConstants.START_DEFAULT_SETTINGS_MESSAGE.format(setting.notificationInterval.displayName)
        ).apply { sender.execute(this) }
    }

    private fun createNewUser(
        userId: Long,
        chatId: Long
    ): NotificationSettingDto {
        val user = TelegramUserDto.create(userId)
        val chat = TelegramChatDto.create(chatId, user)
        val setting = NotificationSettingDto.create(user, chat)

        notificationAccessService.save(user, chat, setting)
        return notificationAccessService.findNotificationSettingByTelegramUserId(userId)
    }
}

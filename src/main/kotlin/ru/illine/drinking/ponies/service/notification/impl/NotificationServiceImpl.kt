package ru.illine.drinking.ponies.service.notification.impl

import org.springframework.stereotype.Service
import org.telegram.telegrambots.abilitybots.api.objects.MessageContext
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.dao.access.TelegramUserAccessService
import ru.illine.drinking.ponies.model.base.AppLogger
import ru.illine.drinking.ponies.model.dto.internal.DefaultSettingsContext
import ru.illine.drinking.ponies.model.dto.internal.GreetingContext
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramChatDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserProfileDto
import ru.illine.drinking.ponies.service.message.MessageProvider
import ru.illine.drinking.ponies.service.notification.NotificationService
import ru.illine.drinking.ponies.util.FunctionHelper.check
import ru.illine.drinking.ponies.util.message.MessageSpec

@Service
class NotificationServiceImpl(
    private val sender: TelegramClient,
    private val notificationAccessService: NotificationAccessService,
    private val telegramUserAccessService: TelegramUserAccessService,
    private val messageProvider: MessageProvider,
) : NotificationService {
    private val logger = AppLogger.SERVICE.logger

    override fun start(messageContext: MessageContext) {
        SendMessage(
            messageContext.chatId().toString(),
            messageProvider.getMessage(MessageSpec.Greeting, GreetingContext(messageContext.user().userName)).text,
        ).apply { sender.execute(this) }

        val externalUserId = messageContext.user().id
        val chatId = messageContext.chatId()
        val profile = with(messageContext.user()) { TelegramUserProfileDto(firstName, lastName, userName) }

        telegramUserAccessService.restoreIfDeleted(externalUserId)

        val setting =
            notificationAccessService.existsByExternalUserId(externalUserId).check(
                ifTrue = {
                    notificationAccessService.updateNotificationsEnabled(externalUserId)
                    notificationAccessService.findNotificationSettingByExternalUserId(externalUserId)
                },
                ifFalse = {
                    createNewUser(externalUserId, chatId, profile)
                },
            )

        SendMessage(
            messageContext.chatId().toString(),
            messageProvider
                .getMessage(
                    MessageSpec.DefaultSettings,
                    DefaultSettingsContext(setting.notificationInterval.displayName),
                ).text,
        ).apply { sender.execute(this) }
    }

    private fun createNewUser(
        externalUserId: Long,
        chatId: Long,
        profile: TelegramUserProfileDto,
    ): NotificationSettingDto {
        val user = TelegramUserDto.create(externalUserId, profile)
        val chat = TelegramChatDto.create(chatId, user)
        val setting = NotificationSettingDto.create(user, chat)

        notificationAccessService.save(user, chat, setting)
        return notificationAccessService.findNotificationSettingByExternalUserId(externalUserId)
    }
}

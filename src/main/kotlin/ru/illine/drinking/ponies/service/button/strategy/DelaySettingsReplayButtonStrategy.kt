package ru.illine.drinking.ponies.service.button.strategy

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.SettingsType
import ru.illine.drinking.ponies.model.base.TimeNotificationType
import ru.illine.drinking.ponies.service.MessageEditorService
import ru.illine.drinking.ponies.service.button.ButtonDataService
import ru.illine.drinking.ponies.service.button.ReplyButtonStrategy
import ru.illine.drinking.ponies.util.TelegramBotKeyboardHelper
import ru.illine.drinking.ponies.util.TelegramMessageConstants
import java.util.*

@Service
class DelaySettingsReplayButtonStrategy(
    private val sender: TelegramClient,
    private val notificationAccessService: NotificationAccessService,
    private val messageEditorService: MessageEditorService,
    private val settingsButtonDataService: ButtonDataService<SettingsType>
) : ReplyButtonStrategy {

    override fun reply(callbackQuery: CallbackQuery) {
        val chatId = callbackQuery.message.chatId
        val messageId = callbackQuery.message.messageId

        messageEditorService.deleteReplyMarkup(chatId, messageId)

        val currentNotificationSetting =
            notificationAccessService.findNotificationSettingByTelegramUserId(callbackQuery.from.id).delayNotification

        SendMessage(
            chatId.toString(),
            TelegramMessageConstants.SETTINGS_DELAY_NOTIFICATION_GREETING_MESSAGE.format(currentNotificationSetting.displayName)
        ).apply {
            enableMarkdown(true)
        }.apply { sender.execute(this) }

        SendMessage(
            chatId.toString(),
            TelegramMessageConstants.SETTINGS_DELAY_NOTIFICATION_BUTTON_MESSAGE
        ).apply {
            replyMarkup = TelegramBotKeyboardHelper.timeOptionButtons(
                TimeNotificationType.entries,
//                exclude = currentNotificationSetting
            )
        }.apply { sender.execute(this) }
    }

    override fun isQueryData(queryData: String): Boolean {
        val delayButtonData = SettingsType.DELAY_NOTIFICATION.getterData.getData(settingsButtonDataService)
        return Objects.equals(delayButtonData, queryData)
    }
}
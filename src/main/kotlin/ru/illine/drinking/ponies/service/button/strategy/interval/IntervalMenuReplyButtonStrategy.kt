package ru.illine.drinking.ponies.service.button.strategy.interval

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.base.SettingsType
import ru.illine.drinking.ponies.service.button.ButtonDataService
import ru.illine.drinking.ponies.service.button.ReplyButtonStrategy
import ru.illine.drinking.ponies.service.telegram.MessageEditorService
import ru.illine.drinking.ponies.util.telegram.TelegramBotKeyboardHelper
import ru.illine.drinking.ponies.util.telegram.TelegramMessageConstants
import java.util.*

@Service
class IntervalMenuReplyButtonStrategy(
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
            notificationAccessService.findNotificationSettingByTelegramUserId(callbackQuery.from.id).notificationInterval

        SendMessage(
            chatId.toString(),
            TelegramMessageConstants.SETTINGS_NOTIFICATION_INTERVAL_GREETING_MESSAGE.format(currentNotificationSetting.displayName)
        ).apply {
            enableMarkdown(true)
        }.apply { sender.execute(this) }

        SendMessage(
            chatId.toString(),
            TelegramMessageConstants.SETTINGS_NOTIFICATION_INTERVAL_BUTTON_MESSAGE
        ).apply {
            replyMarkup = TelegramBotKeyboardHelper.intervalTimeButtons(currentNotificationSetting)
        }.apply { sender.execute(this) }
    }

    override fun isQueryData(queryData: String): Boolean {
        val intervalButtonData = SettingsType.NOTIFICATION_INTERVAL.getterData.getData(settingsButtonDataService)
        return Objects.equals(intervalButtonData, queryData)
    }
}

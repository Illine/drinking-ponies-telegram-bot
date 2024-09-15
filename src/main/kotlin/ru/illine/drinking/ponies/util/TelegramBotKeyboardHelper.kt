package ru.illine.drinking.ponies.util

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.DelayNotificationType
import ru.illine.drinking.ponies.model.base.PauseNotificationType
import ru.illine.drinking.ponies.model.base.SettingsType
import ru.illine.drinking.ponies.service.button.ButtonDataService

object TelegramBotKeyboardHelper {

    fun settingsButtons(service: ButtonDataService<SettingsType>, messageId: Int? = null): InlineKeyboardMarkup {
        val rows = SettingsType.values()
            .filter { it.visible }
            .map {
                val buttonData = service.getData(it)
                if (it.web) {
                    val updatedButtonData = messageId?.let { id -> "$buttonData?messageId=$id" } ?: buttonData
                    InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                            .text(it.displayName).webApp(WebAppInfo(updatedButtonData))
                            .build()
                    )
                } else {
                    InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                            .text(it.displayName).callbackData(buttonData)
                            .build()
                    )
                }
            }

        return InlineKeyboardMarkup.builder()
            .keyboard(rows)
            .build()
    }

    fun delayTimeButtons(delayTime: DelayNotificationType? = null): ReplyKeyboard {
        val rows = DelayNotificationType.values()
            .filter { it != delayTime }
            .map {
                InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                        .text(it.displayName)
                        .callbackData(it.queryData.toString())
                        .build()
                )
            }

        return InlineKeyboardMarkup.builder()
            .keyboard(rows)
            .build()
    }

    fun pauseTimeButtons(currentDelayTime: DelayNotificationType): ReplyKeyboard {
        val rows = PauseNotificationType.values()
            .filter { it == PauseNotificationType.RESET || it.minutes > currentDelayTime.minutes }
            .map {
                InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                        .text(it.displayName)
                        .callbackData(it.queryData.toString())
                        .build()
                )
            }

        return InlineKeyboardMarkup.builder()
            .keyboard(rows)
            .build()
    }

    fun notifyButtons(): ReplyKeyboard {
        val buttons = AnswerNotificationType.values()
            .map {
                InlineKeyboardButton.builder()
                    .text(it.displayName)
                    .callbackData(it.queryData.toString())
                    .build()
            }

        val row = InlineKeyboardRow(buttons)

        return InlineKeyboardMarkup.builder()
            .keyboard(listOf(row))
            .build()
    }

}
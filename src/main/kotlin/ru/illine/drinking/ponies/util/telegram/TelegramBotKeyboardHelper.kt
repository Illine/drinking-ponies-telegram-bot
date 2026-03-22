package ru.illine.drinking.ponies.util.telegram

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo
import ru.illine.drinking.ponies.model.base.*
import ru.illine.drinking.ponies.service.button.ButtonDataService

object TelegramBotKeyboardHelper {

    fun settingsButtons(service: ButtonDataService<SettingsType>, messageId: Int? = null): InlineKeyboardMarkup {
        val rows = SettingsType.entries
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

    fun intervalTimeButtons(intervalTime: IntervalNotificationType? = null): ReplyKeyboard {
        val rows = IntervalNotificationType.entries
            .filter { it != intervalTime }
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

    fun pauseTimeButtons(currentIntervalTime: IntervalNotificationType): ReplyKeyboard {
        val rows = PauseNotificationType.entries
            .filter { it == PauseNotificationType.RESET || it.minutes > currentIntervalTime.minutes }
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

    fun snoozeTimeButtons(): ReplyKeyboard {
        val rows = SnoozeNotificationType.entries
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
        val buttons = AnswerNotificationType.entries
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
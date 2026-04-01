package ru.illine.drinking.ponies.util.telegram

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo
import ru.illine.drinking.ponies.model.base.*
import ru.illine.drinking.ponies.service.button.ButtonDataService
import java.util.*

object TelegramBotKeyboardHelper {

    fun settingsButtons(service: ButtonDataService<SettingsType>, messageId: Int? = null): InlineKeyboardMarkup {
        val rows = SettingsType.entries
            .filter { it.visible }
            .map {
                val buttonData = service.getData(it)
                if (it.web) {
                    val updatedButtonData = if (messageId != null) "$buttonData?messageId=$messageId" else buttonData
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
        return buildInlineKeyboard(
            IntervalNotificationType.entries.filter { it != intervalTime },
            IntervalNotificationType::displayName,
            IntervalNotificationType::queryData
        )
    }

    fun pauseTimeButtons(currentIntervalTime: IntervalNotificationType): ReplyKeyboard {
        return buildInlineKeyboard(
            PauseNotificationType.entries.filter { it == PauseNotificationType.RESET || it.minutes > currentIntervalTime.minutes },
            PauseNotificationType::displayName,
            PauseNotificationType::queryData
        )
    }

    fun snoozeTimeButtons(): ReplyKeyboard {
        return buildInlineKeyboard(
            SnoozeNotificationType.entries,
            SnoozeNotificationType::displayName,
            SnoozeNotificationType::queryData
        )
    }

    fun waterAmountButtons(): ReplyKeyboard {
        return buildInlineKeyboard(
            WaterAmountType.entries,
            WaterAmountType::displayName,
            WaterAmountType::queryData
        )
    }

    fun notifyButtons(): ReplyKeyboard {
        return buildInlineKeyboard(
            AnswerNotificationType.entries,
            AnswerNotificationType::displayName,
            AnswerNotificationType::queryData,
            singleRow = true
        )
    }

    private fun <T> buildInlineKeyboard(
        entries: List<T>,
        displayName: (T) -> String,
        queryData: (T) -> UUID,
        singleRow: Boolean = false
    ): ReplyKeyboard {
        val buttons = entries.map {
            InlineKeyboardButton.builder()
                .text(displayName(it))
                .callbackData(queryData(it).toString())
                .build()
        }

        val keyboard = if (singleRow) {
            listOf(InlineKeyboardRow(buttons))
        } else {
            buttons.map { InlineKeyboardRow(it) }
        }

        return InlineKeyboardMarkup.builder()
            .keyboard(keyboard)
            .build()
    }

}
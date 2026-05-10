package ru.illine.drinking.ponies.util.telegram

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.SnoozeNotificationType
import ru.illine.drinking.ponies.model.base.WaterAmountType
import java.util.*

object TelegramBotKeyboardHelper {

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

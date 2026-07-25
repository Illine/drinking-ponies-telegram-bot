package ru.illine.drinking.ponies.util.telegram

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.SnoozeNotificationType
import ru.illine.drinking.ponies.model.base.WaterAmountType
import java.util.UUID

object TelegramBotKeyboardHelper {
    private const val WATER_AMOUNT_BUTTONS_PER_ROW = 3

    fun snoozeTimeButtons(): ReplyKeyboard =
        buildInlineKeyboard(
            SnoozeNotificationType.entries,
            SnoozeNotificationType::displayName,
            SnoozeNotificationType::queryData,
        )

    fun waterAmountButtons(): ReplyKeyboard =
        buildInlineKeyboard(
            WaterAmountType.entries,
            WaterAmountType::displayName,
            WaterAmountType::queryData,
            buttonsPerRow = WATER_AMOUNT_BUTTONS_PER_ROW,
        )

    fun notifyButtons(): ReplyKeyboard =
        buildInlineKeyboard(
            AnswerNotificationType.entries,
            AnswerNotificationType::displayName,
            AnswerNotificationType::queryData,
            buttonsPerRow = AnswerNotificationType.entries.size,
        )

    private fun <T> buildInlineKeyboard(
        entries: List<T>,
        displayName: (T) -> String,
        queryData: (T) -> UUID,
        buttonsPerRow: Int = 1,
    ): ReplyKeyboard {
        val keyboard =
            entries
                .map {
                    InlineKeyboardButton
                        .builder()
                        .text(displayName(it))
                        .callbackData(queryData(it).toString())
                        .build()
                }.chunked(buttonsPerRow)
                .map { InlineKeyboardRow(it) }

        return InlineKeyboardMarkup
            .builder()
            .keyboard(keyboard)
            .build()
    }
}

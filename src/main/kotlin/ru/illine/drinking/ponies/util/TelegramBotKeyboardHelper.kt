package ru.illine.drinking.ponies.util

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.DelayNotificationType
import java.util.*

class TelegramBotKeyboardHelper {

    companion object {
        fun delayTimeButtons(delayTime: DelayNotificationType? = null): ReplyKeyboard {
            val rows = EnumSet.allOf(DelayNotificationType::class.java).stream().filter { it != delayTime }.map {
                InlineKeyboardButton().apply {
                    text = it.displayName
                    callbackData = it.name
                }.let { listOf(it) }
            }.toList()

            return InlineKeyboardMarkup().apply { keyboard = rows }
        }

        fun notifyButtons(): ReplyKeyboard {
            val rows = EnumSet.allOf(AnswerNotificationType::class.java).stream().map {
                InlineKeyboardButton().apply {
                    text = it.displayName
                    callbackData = it.name
                }
            }.toList()
            return InlineKeyboardMarkup()
                .apply {
                    keyboard = listOf(rows)
                }
        }

    }
}
package ru.illine.drinking.ponies.util

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.DelayNotificationType
import ru.illine.drinking.ponies.model.base.SettingsType
import ru.illine.drinking.ponies.model.base.SnoozeNotificationType
import java.util.*

class TelegramBotKeyboardHelper {

    companion object {

        fun settingsButtons(): ReplyKeyboard {
            val rows =
                EnumSet.allOf(SettingsType::class.java)
                    .stream()
                    .filter { it.visible }
                    .map {
                        InlineKeyboardButton().apply {
                            text = it.displayName
                            callbackData = it.queryData.toString()
                        }.let { listOf(it) }
                    }.toList()

            return InlineKeyboardMarkup().apply { keyboard = rows }
        }

        fun delayTimeButtons(delayTime: DelayNotificationType? = null): ReplyKeyboard {
            val rows =
                EnumSet.allOf(DelayNotificationType::class.java)
                    .stream()
                    .filter { it != delayTime }
                    .map {
                        InlineKeyboardButton().apply {
                            text = it.displayName
                            callbackData = it.queryData.toString()
                        }.let { listOf(it) }
                    }.toList()

            return InlineKeyboardMarkup().apply { keyboard = rows }
        }

        fun snoozeTimeButtons(currentDelayTime: DelayNotificationType): ReplyKeyboard {
            val rows =
                EnumSet.allOf(SnoozeNotificationType::class.java)
                    .stream()
                    .filter { it.minutes > currentDelayTime.minutes }
                    .map {
                        InlineKeyboardButton().apply {
                            text = it.displayName
                            callbackData = it.queryData.toString()
                        }.let { listOf(it) }
                    }.toList()

            return InlineKeyboardMarkup().apply { keyboard = rows }
        }

        fun notifyButtons(): ReplyKeyboard {
            val rows =
                EnumSet.allOf(AnswerNotificationType::class.java)
                    .stream()
                    .map {
                        InlineKeyboardButton().apply {
                            text = it.displayName
                            callbackData = it.queryData.toString()
                        }
                    }.toList()
            return InlineKeyboardMarkup()
                .apply {
                    keyboard = listOf(rows)
                }
        }

    }
}
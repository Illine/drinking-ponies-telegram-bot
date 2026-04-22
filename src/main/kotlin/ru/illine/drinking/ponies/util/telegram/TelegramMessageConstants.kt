package ru.illine.drinking.ponies.util.telegram

object TelegramMessageConstants {

    val START_GREETING_MESSAGE =
        """
                Здравствуй, %s! 
                Я бот Пьющие Поняшки. Я полностью понимающий и знаю, что всем нужно пить!
            """.trimIndent()

    val START_DEFAULT_SETTINGS_MESSAGE =
        """
               Установлены настройки по-умолчанию:
               Периодичность уведомлений: %s
               Часовой пояс: Москва
               Время тихого режима: с 23:00 до 11:00
            """.trimIndent()

    val RESUME_GREETING_MESSAGE =
        """
                Поняшки снова в деле!
                Котик, тебе снова будут приходить уведомления.
            """.trimIndent()

    val PAUSE_GREETING_MESSAGE =
        """
                Выбери, пожалуйста, сколько времени тебя не будут беспокоить поняшки.
            """.trimIndent()

    val STOP_GREETING_MESSAGE = "Поняшки больше не будут напоминать тебе попить :("

    val PAUSE_BUTTON_RESULT_MESSAGE = "Поняшки не будут тебя беспокоить следующие %s!"

    val PAUSE_RESET_BUTTON_RESULT_MESSAGE =
        """
                Теперь тебе снова будут приходить напоминая, что нужно водицы напиться раз в %s!
                Ориентировочно следующий раз поняшка с уведомлением вернётся в %s.
            """.trimIndent()

    val NOTIFICATION_QUESTION_MESSAGE = "Водица выпита?"

    val NOTIFICATION_QUESTION_EDITED_MESSAGE_PATTERN = "$NOTIFICATION_QUESTION_MESSAGE\nБыло выбрано: *%s*"

    val NOTIFICATION_SUSPEND_MESSAGE =
        """
                Котик, твое уведомление отложено! 
                Через %s я тебя снова побеспокою, жди!
            """.trimIndent()

    val NOTIFICATION_ANSWER_YES_MESSAGE = "Ты - солнышко! Так держать!"

    val NOTIFICATION_ANSWER_CANCEL_MESSAGE = "Милый зайчик, пожалуйста, напейся в следующий раз!"

    val NOTIFICATION_NOT_ACTIVE_MESSAGE =
        """
                К сожалению, уведомления тебе отключены. Чтобы использовать эту функцию тебе нужно включить их обратно.
            """.trimIndent()

    val PAUSE_RESET_TO_DEFAULT_MESSAGE = "Вернуть как было!"

    val NOTIFICATION_SNOOZE_MENU_MESSAGE = "Выбери, на сколько хочешь отложить уведомление"

    val NOTIFICATION_WATER_AMOUNT_MENU_MESSAGE = "Сколько водицы ты выпил(а)?"

}
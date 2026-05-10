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

    val NOTIFICATION_QUESTION_MESSAGE = "Водица выпита?"

    val NOTIFICATION_QUESTION_EDITED_MESSAGE_PATTERN = "$NOTIFICATION_QUESTION_MESSAGE\nБыло выбрано: *%s*"

    val NOTIFICATION_SUSPEND_MESSAGE =
        """
                Котик, твое уведомление отложено! 
                Через %s я тебя снова побеспокою, жди!
            """.trimIndent()

    val NOTIFICATION_ANSWER_YES_MESSAGE = "Ты - солнышко! Так держать!"

    val NOTIFICATION_ANSWER_CANCEL_MESSAGE = "Милый зайчик, пожалуйста, напейся в следующий раз!"

    val NOTIFICATION_SNOOZE_MENU_MESSAGE = "Выбери, на сколько хочешь отложить уведомление"

    val NOTIFICATION_WATER_AMOUNT_MENU_MESSAGE = "Сколько водицы выпито?"

}

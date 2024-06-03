package ru.illine.drinking.ponies.util

class MessageHelper {

    companion object {

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
               
               Если хочешь изменить настройки, то нажми: /settings
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

        val SETTINGS_GREETING_MESSAGE = "Выбери, какие настройки ты хочешь изменить"

        val SETTINGS_DELAY_NOTIFICATION_GREETING_MESSAGE = "Сейчас периодичность отправки уведомлений: *%s*"
        val SETTINGS_DELAY_NOTIFICATION_BUTTON_MESSAGE =
            """
                Помните, что пить - это очень важно и если ты забываешь, то я всегда напомню! 
                Теперь выбери периодичность отправки напоминаний.
            """.trimIndent()

        val TIME_BUTTON_RESULT_MESSAGE = "Тебе будут приходить напоминая, что нужно водицы напиться раз в %s!"

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
                Надеюсь в следующий раз (который будет через %s) я снова тебе напомню, не волнуйся!
            """.trimIndent()

        val NOTIFICATION_ANSWER_YES_MESSAGE = "Ты - солнышко! Так держать!"

        val NOTIFICATION_ANSWER_DELAY_MESSAGE =
            """
                Это грустно, что ты так мало пьёшь! 
                Наверное, котик был занят? Ничего страшного! Я напомню через несколько минут!
            """.trimIndent()

        val NOTIFICATION_ANSWER_CANCEL_MESSAGE = "Милый зайчик, пожалуйста, напейся в следующий раз!"

        val NOTIFICATION_NOT_ACTIVE_MESSAGE =
            """
                К сожалению, уведомления тебе отключены. Чтобы использовать эту функцию тебе нужно включить их обратно.
            """.trimIndent()

        val PAUSE_RESET_TO_DEFAULT_MESSAGE = "Вернуть как было!"

        val HALF_HOUR = "30 минут"
        val HOUR = "1 час"
        val HOUR_AND_HALF = "1 час 30 минут"
        val TWO_HOURS = "2 часа"
        val THREE_HOURS = "3 часа"
        val FOUR_HOURS = "4 часа"
        val FIVE_HOURS = "5 часов"

    }

}
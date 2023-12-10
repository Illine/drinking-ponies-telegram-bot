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
            Поняшки снова в деле: Котик, тебе снова будут приходить уведомления!
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
    }

}
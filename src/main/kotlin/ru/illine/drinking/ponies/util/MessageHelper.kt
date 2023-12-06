package ru.illine.drinking.ponies.util

class MessageHelper {

    companion object {

        val START_GREETING_MESSAGE =
            """
                Здравствуй, %s! 
                Я бот Пьющие Поняшки. Я полностью понимающий и знаю, что всем нужно пить!
            """.trimIndent()
        val START_BUTTON_MESSAGE = "Пожалуйста, не стесняйся своего желания пить и установи периодичность с которой я должен тебе напоминать об этом."

        val RESUME_GREETING_MESSAGE =
        """
            Поняшки снова в деле: Тебе, Котик, снова будут приходить уведомления!
        """.trimIndent()

        val STOP_GREETING_MESSAGE = "Поняшки больше не будут напоминать тебе попить :("

        val SETTINGS_GREETING_MESSAGE = "Сейчас периодичность отправки уведомлений: **%s**"
        val SETTINGS_BUTTON_MESSAGE =
        """
            Помните, что пить - это очень важно и если ты забываешь, то я всегда напомню! 
            Теперь выбери периодичность отправки напоминаний.
        """.trimIndent()

        val TIME_BUTTON_RESULT_MESSAGE = "Теперь тебе будут приходить напоминая, что нужно водицы напиться раз в %s!"

        val NOTIFICATION_MESSAGE = "Водица выпита?"

        val NOTIFICATION_SUSPEND_MESSAGE =
            """
                Котик, твое уведомление отложено! 
                Надеюсь в следующий раз (который будет через %s) я снова тебе напомню, не волнуйся!
            """.trimIndent()

        val NOTIFICATION_ANSWER_YES_MESSAGE = "ты - солнышко! Так держать!"

        val NOTIFICATION_ANSWER_NO_MESSAGE =
            """
                Это грустно! 
                Наверное, котик был занят? Ничего страшного! Я напомню через несколько минут!
            """.trimIndent()

        val NOTIFICATION_ANSWER_CANCEL_MESSAGE = "Милый зайчик, пожалуйста, напейся в следующий раз!"
    }

}
package ru.illine.drinking.ponies.util

class MessageHelper {

    companion object {

        val START_GREETING_MESSAGE = "Здравствуйте, %s! Это Пьющие Поняшки. Я полностью понимающий и знаю, что всем нужно пить!"
        val START_BUTTON_MESSAGE = "Пожалуйста, не стесняйтесь своего желания пить и установите периодичность с которой я должен Вам напоминать об этом."

        val SETTINGS_GREETING_MESSAGE = "Сейчас периодичность отправки уведомлений: **%s**"
        val SETTINGS_BUTTON_MESSAGE =
        """
            Помните, что пить - это очень важно и если Вы забываете, то я всегда Вам напомню. 
            Теперь выберите новую периодичность отправки напоминаний.
        """.trimIndent()

        val TIME_BUTTON_RESULT_MESSAGE = "Теперь вам будут приходить напоминая, что нужно водицы напиться раз в %s!"

        val NOTIFICATION_MESSAGE = "Вы выпили воду?"

        val NOTIFICATION_SUSPEND_MESSAGE =
            """
                Котик, Ваше уведомление отложено! 
                Надеюсь в следующий раз (который будет через %s) я снова тебе напомню, не волнуйся!
            """.trimIndent()

        val NOTIFICATION_ANSWER_YES_MESSAGE = "Вы солнышко! Так держать!"

        val NOTIFICATION_ANSWER_NO_MESSAGE =
            """
                Это грустно! 
                Наверное, Вы были заняты? Ничего страшного! Я напомню Вам через несколько минут!
            """.trimIndent()

        val NOTIFICATION_ANSWER_CANCEL_MESSAGE = "Милый зайчик, пожалуйста, напейтесь в следующий раз!"
    }

}
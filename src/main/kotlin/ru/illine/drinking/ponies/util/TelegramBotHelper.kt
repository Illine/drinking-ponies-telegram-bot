package ru.illine.drinking.ponies.util

import org.apache.http.client.HttpClient
import org.springframework.util.ReflectionUtils
import org.telegram.telegrambots.meta.generics.BotSession
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.DelayNotificationType
import ru.illine.drinking.ponies.model.base.ReplayType

class TelegramBotHelper {

    companion object {
        private val READER_THREAD_FIELD_NAME = "readerThread"
        private val HTTP_CLIENT_FIELD_NAME = "httpclient"

        fun replaceBotSessionHttpClient(
            session: BotSession,
            httpClient: HttpClient
        ) {
            val readerThread = ReflectionUtils.findField(session.javaClass, READER_THREAD_FIELD_NAME)!!
                .apply { ReflectionUtils.makeAccessible(this) }
                .let { ReflectionUtils.getField(it, session) }!!

            ReflectionUtils.findField(readerThread.javaClass, HTTP_CLIENT_FIELD_NAME)!!
                .apply { ReflectionUtils.makeAccessible(this) }
                .apply { ReflectionUtils.setField(this, readerThread, httpClient) }
        }

        // Переписать этот ужас
        fun getReplayType(callbackData: String): ReplayType {
            val answerNotificationButtonIds = AnswerNotificationType.BUTTON_IDS
            val delayNotificationButtonIds = DelayNotificationType.BUTTON_IDS

            if (answerNotificationButtonIds.contains(callbackData)) {
                return ReplayType.ANSWER_NOTIFICATION_BUTTON
            } else if (delayNotificationButtonIds.contains(callbackData)) {
                return ReplayType.TIME_BUTTON
            }

            throw IllegalArgumentException("Not found a button id!")
        }
    }
}

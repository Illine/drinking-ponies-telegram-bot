package ru.illine.drinking.ponies.util

import org.apache.http.client.HttpClient
import org.springframework.util.ReflectionUtils
import org.telegram.telegrambots.meta.generics.BotSession

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
    }
}

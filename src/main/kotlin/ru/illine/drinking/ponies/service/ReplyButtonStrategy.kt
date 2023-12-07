package ru.illine.drinking.ponies.service

import org.telegram.telegrambots.meta.api.objects.CallbackQuery

interface ReplyButtonStrategy {

    fun reply(callbackQuery: CallbackQuery)

    fun isQueryData(queryData: String): Boolean
}
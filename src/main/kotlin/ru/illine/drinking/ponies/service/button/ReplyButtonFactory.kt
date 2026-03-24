package ru.illine.drinking.ponies.service.button

fun interface ReplyButtonFactory {

    fun getStrategy(queryData: String): ReplyButtonStrategy

}
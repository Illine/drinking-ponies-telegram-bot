package ru.illine.drinking.ponies.service.button

fun interface ReplayButtonFactory {

    fun getStrategy(queryData: String): ReplyButtonStrategy

}
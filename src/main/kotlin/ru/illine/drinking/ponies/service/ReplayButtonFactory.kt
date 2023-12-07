package ru.illine.drinking.ponies.service

fun interface ReplayButtonFactory {

    fun getStrategy(queryData: String): ReplyButtonStrategy

}
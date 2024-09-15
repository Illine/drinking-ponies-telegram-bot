package ru.illine.drinking.ponies.service.button

fun interface ButtonDataService<T : Enum<T>> {
    fun getData(enumValue: T): String
}
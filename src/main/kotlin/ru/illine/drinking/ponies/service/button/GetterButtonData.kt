package ru.illine.drinking.ponies.service.button

fun interface GetterButtonData<T : Enum<T>> {
    fun getData(service: ButtonDataService<T>): String
}
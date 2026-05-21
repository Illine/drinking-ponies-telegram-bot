package ru.illine.drinking.ponies.util

object PluralizationHelper {

    fun pluralizeDays(n: Int): String {
        val abs = if (n < 0) -n else n
        val mod100 = abs % 100
        if (mod100 in 11..14) return "дней"
        return when (abs % 10) {
            1 -> "день"
            2, 3, 4 -> "дня"
            else -> "дней"
        }
    }
}

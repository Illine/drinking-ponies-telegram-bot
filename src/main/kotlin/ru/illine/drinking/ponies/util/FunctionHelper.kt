package ru.illine.drinking.ponies.util

object FunctionHelper {

    inline fun <T> Boolean.check(
        ifTrue: Boolean.() -> T,
        ifFalse: Boolean.() -> T
    ) = when (this) {
        true -> ifTrue()
        false -> ifFalse()
    }

    inline fun catchAny(
        action: () -> Unit,
        ifException: (Exception) -> Unit? = {},
        errorLogging: (Exception) -> Unit?
    ) {
        try {
            action()
        } catch (e: Exception) {
            errorLogging(e)
            ifException(e)
        }
    }

    inline fun <T> catchAnyWithReturn(
        action: () -> T,
        noinline ifException: ((Exception) -> T?)? = null,
        errorLogging: (Exception) -> Unit?
    ): T? {
        return try {
            action()
        } catch (e: Exception) {
            errorLogging(e)
            ifException?.invoke(e)
        }
    }
}

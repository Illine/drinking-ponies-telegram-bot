package ru.illine.drinking.ponies.model.dto.internal

data class TelegramChatDto(
    var id: Long? = null,

    val telegramUser: TelegramUserDto,

    var externalChatId: Long,

    var previousNotificationMessageId: Int? = null
) {
    companion object {
        fun create(externalChatId: Long, telegramUser: TelegramUserDto): TelegramChatDto =
            TelegramChatDto(
                telegramUser = telegramUser,
                externalChatId = externalChatId,
            )
    }
}

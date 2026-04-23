package ru.illine.drinking.ponies.service.notification

import org.telegram.telegrambots.abilitybots.api.objects.MessageContext

interface NotificationService {

    fun start(messageContext: MessageContext)

    fun resume(messageContext: MessageContext)

    fun pause(messageContext: MessageContext)

}

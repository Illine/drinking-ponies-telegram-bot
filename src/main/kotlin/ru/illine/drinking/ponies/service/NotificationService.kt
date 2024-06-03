package ru.illine.drinking.ponies.service

import org.telegram.abilitybots.api.objects.MessageContext
import ru.illine.drinking.ponies.model.dto.NotificationDto

interface NotificationService {

    fun start(messageContext: MessageContext)

    fun stop(messageContext: MessageContext)

    fun resume(messageContext: MessageContext)

    fun pause(messageContext: MessageContext)

    fun settings(messageContext: MessageContext)

    fun sendNotifications(notifications: Collection<NotificationDto>)

    fun suspendNotifications(notifications: Collection<NotificationDto>)
}
package ru.illine.drinking.ponies.dao.access

interface TelegramUserAccessService {

    fun findIsAdminByExternalUserId(externalUserId: Long): Boolean
}

package ru.illine.drinking.ponies.dao.access

import org.springframework.data.domain.Pageable
import ru.illine.drinking.ponies.model.base.AdminUserStatusFilter
import ru.illine.drinking.ponies.model.dto.internal.AdminUserDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserProfileDto
import ru.illine.drinking.ponies.model.dto.internal.UserAccessDto
import ru.illine.drinking.ponies.model.dto.internal.UserCountsDto
import ru.illine.drinking.ponies.model.dto.internal.UserStateChangeDto

interface TelegramUserAccessService {
    fun resolveAccessFlags(externalUserId: Long): UserAccessDto

    /** Returns null when the caller carries no profile, true when the stored one was refreshed. */
    fun syncProfile(
        externalUserId: Long,
        profile: TelegramUserProfileDto,
    ): Boolean?

    fun findAllForAdmin(
        search: String?,
        status: AdminUserStatusFilter,
        pageable: Pageable,
    ): List<AdminUserDto>

    fun findByIdForAdmin(id: Long): AdminUserDto?

    fun countForAdmin(search: String?): UserCountsDto

    fun updateState(
        id: Long,
        actorId: Long,
        change: UserStateChangeDto,
    ): UserAccessDto

    fun restoreIfDeleted(externalUserId: Long): Boolean
}

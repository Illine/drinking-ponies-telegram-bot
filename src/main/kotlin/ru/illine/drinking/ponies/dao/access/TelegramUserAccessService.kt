package ru.illine.drinking.ponies.dao.access

import org.springframework.data.domain.Pageable
import ru.illine.drinking.ponies.model.base.AdminUserStatusFilter
import ru.illine.drinking.ponies.model.dto.internal.AdminUserDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserProfile
import ru.illine.drinking.ponies.model.dto.internal.UserAccessDto
import ru.illine.drinking.ponies.model.dto.internal.UserCountsDto

interface TelegramUserAccessService {
    fun resolveAccessFlags(
        externalUserId: Long,
        profile: TelegramUserProfile,
    ): UserAccessDto

    fun findAllForAdmin(
        search: String?,
        status: AdminUserStatusFilter,
        pageable: Pageable,
    ): List<AdminUserDto>

    fun findByIdForAdmin(id: Long): AdminUserDto?

    fun countForAdmin(search: String?): UserCountsDto

    fun updateState(
        id: Long,
        externalUserId: Long,
        deleted: Boolean?,
    )

    fun restoreIfDeleted(externalUserId: Long): Boolean
}

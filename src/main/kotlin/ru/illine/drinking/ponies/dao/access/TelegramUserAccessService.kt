package ru.illine.drinking.ponies.dao.access

import org.springframework.data.domain.Pageable
import ru.illine.drinking.ponies.dao.repository.AdminUserProjection
import ru.illine.drinking.ponies.dao.repository.UserCountsProjection
import ru.illine.drinking.ponies.model.base.AdminUserStatusFilter
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserProfile
import ru.illine.drinking.ponies.model.dto.internal.UserAccessDto

interface TelegramUserAccessService {
    fun resolveAccessFlags(
        externalUserId: Long,
        profile: TelegramUserProfile,
    ): UserAccessDto

    fun findAllForAdmin(
        search: String?,
        status: AdminUserStatusFilter,
        pageable: Pageable,
    ): List<AdminUserProjection>

    fun findByIdForAdmin(id: Long): AdminUserProjection?

    fun countForAdmin(search: String?): UserCountsProjection

    fun updateDeleted(
        id: Long,
        externalUserId: Long,
        deleted: Boolean,
    )

    // Returns true when the user was soft-deleted and has just been brought back.
    fun restoreIfDeleted(externalUserId: Long): Boolean
}

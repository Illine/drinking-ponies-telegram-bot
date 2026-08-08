package ru.illine.drinking.ponies.service.user

import ru.illine.drinking.ponies.model.base.AdminUserStatusFilter
import ru.illine.drinking.ponies.model.dto.internal.AdminUserDto
import ru.illine.drinking.ponies.model.dto.internal.AdminUserPageDto
import ru.illine.drinking.ponies.model.dto.internal.UserStateDto

interface UserAdminService {
    fun getUsers(
        search: String?,
        status: AdminUserStatusFilter,
        page: Int,
        size: Int,
    ): AdminUserPageDto

    fun getUser(id: Long): AdminUserDto

    fun updateState(
        id: Long,
        state: UserStateDto,
    ): AdminUserDto
}

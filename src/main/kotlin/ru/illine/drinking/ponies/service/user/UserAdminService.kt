package ru.illine.drinking.ponies.service.user

import ru.illine.drinking.ponies.model.base.AdminUserStatusFilter
import ru.illine.drinking.ponies.model.dto.response.UserDetailsResponse
import ru.illine.drinking.ponies.model.dto.response.UsersResponse

interface UserAdminService {
    fun getUsers(
        search: String?,
        status: AdminUserStatusFilter,
        page: Int,
        size: Int,
    ): UsersResponse

    fun getUser(id: Long): UserDetailsResponse

    fun updateState(
        id: Long,
        isActive: Boolean?,
    ): UserDetailsResponse
}

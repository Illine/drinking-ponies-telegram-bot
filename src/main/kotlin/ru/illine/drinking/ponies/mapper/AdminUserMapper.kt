package ru.illine.drinking.ponies.mapper

import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Mapping
import ru.illine.drinking.ponies.dao.repository.projection.AdminUserProjection
import ru.illine.drinking.ponies.dao.repository.projection.UserCountsProjection
import ru.illine.drinking.ponies.model.dto.internal.AdminUserDto
import ru.illine.drinking.ponies.model.dto.internal.UserCountsDto

@Konverter
interface AdminUserMapper {
    @Konvert(
        mappings = [
            Mapping(source = "admin", target = "isAdmin"),
            Mapping(source = "banned", target = "isBanned"),
        ],
    )
    fun toDto(projection: AdminUserProjection): AdminUserDto

    fun toCounts(projection: UserCountsProjection): UserCountsDto

    companion object : AdminUserMapper by Konverter.get<AdminUserMapper>()
}

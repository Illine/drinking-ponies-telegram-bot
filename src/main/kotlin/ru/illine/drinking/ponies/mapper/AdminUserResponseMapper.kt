package ru.illine.drinking.ponies.mapper

import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Mapping
import ru.illine.drinking.ponies.model.dto.internal.AdminUserDto
import ru.illine.drinking.ponies.model.dto.internal.AdminUserPageDto
import ru.illine.drinking.ponies.model.dto.internal.UserCountsDto
import ru.illine.drinking.ponies.model.dto.response.ShortUserInfo
import ru.illine.drinking.ponies.model.dto.response.UserCounts
import ru.illine.drinking.ponies.model.dto.response.UserDetailsResponse
import ru.illine.drinking.ponies.model.dto.response.UsersResponse

@Konverter
interface AdminUserResponseMapper {
    fun toResponse(dto: AdminUserPageDto): UsersResponse

    fun toCounts(dto: UserCountsDto): UserCounts

    @Konvert(
        mappings = [
            Mapping(target = "isActive", expression = "!it.deleted"),
            Mapping(target = "lastActivity", expression = "it.lastActivity?.toInstant(java.time.ZoneOffset.UTC)"),
        ],
    )
    fun toShortUserInfo(dto: AdminUserDto): ShortUserInfo

    @Konvert(
        mappings = [
            Mapping(target = "isActive", expression = "!it.deleted"),
            Mapping(target = "lastActivity", expression = "it.lastActivity?.toInstant(java.time.ZoneOffset.UTC)"),
            Mapping(target = "registeredAt", expression = "it.created.toInstant(java.time.ZoneOffset.UTC)"),
        ],
    )
    fun toDetails(dto: AdminUserDto): UserDetailsResponse

    companion object : AdminUserResponseMapper by Konverter.get<AdminUserResponseMapper>()
}

package ru.illine.drinking.ponies.service.user.impl

import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.query.EscapeCharacter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.illine.drinking.ponies.dao.access.TelegramUserAccessService
import ru.illine.drinking.ponies.exception.TelegramUserNotFoundException
import ru.illine.drinking.ponies.model.base.AdminUserStatusFilter
import ru.illine.drinking.ponies.model.dto.internal.AdminUserDto
import ru.illine.drinking.ponies.model.dto.internal.AdminUserPageDto
import ru.illine.drinking.ponies.model.dto.internal.UserCountsDto
import ru.illine.drinking.ponies.model.dto.internal.UserStateDto
import ru.illine.drinking.ponies.service.user.UserAdminService

@Service
class UserAdminServiceImpl(
    private val telegramUserAccessService: TelegramUserAccessService,
) : UserAdminService {
    private val logger = LoggerFactory.getLogger("SERVICE")

    override fun getUsers(
        search: String?,
        status: AdminUserStatusFilter,
        page: Int,
        size: Int,
    ): AdminUserPageDto {
        val query = search?.takeIf { it.isNotBlank() }?.toSearchPattern()
        val users = telegramUserAccessService.findAllForAdmin(query, status, PageRequest.of(page, size))

        val counts = telegramUserAccessService.countForAdmin(query)

        return AdminUserPageDto(
            users = users,
            page = page,
            size = size,
            total = counts.of(status),
            counts = counts,
        )
    }

    override fun getUser(id: Long): AdminUserDto = requireUser(id)

    @Transactional
    override fun updateState(
        id: Long,
        state: UserStateDto,
    ): AdminUserDto {
        val isActive = requireNotNull(state.isActive) { "At least one state field is required, got an empty payload" }

        val user = requireUser(id)

        logger.info("Setting isActive={} for user [{}]", isActive, id)
        telegramUserAccessService.updateState(id, user.externalUserId, deleted = !isActive)

        return user.copy(deleted = !isActive)
    }

    private fun String.toSearchPattern(): String = "%${EscapeCharacter.DEFAULT.escape(this)}%"

    private fun requireUser(id: Long): AdminUserDto =
        telegramUserAccessService.findByIdForAdmin(id)
            ?: throw TelegramUserNotFoundException("No user with id: [$id]")

    private fun UserCountsDto.of(status: AdminUserStatusFilter): Long =
        when (status) {
            AdminUserStatusFilter.ALL -> all
            AdminUserStatusFilter.ACTIVE -> active
            AdminUserStatusFilter.INACTIVE -> inactive
            AdminUserStatusFilter.BANNED -> banned
        }
}

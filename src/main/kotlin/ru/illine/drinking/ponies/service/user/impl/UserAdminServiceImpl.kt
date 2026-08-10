package ru.illine.drinking.ponies.service.user.impl

import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.query.EscapeCharacter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.illine.drinking.ponies.dao.access.TelegramUserAccessService
import ru.illine.drinking.ponies.exception.InactiveUserPromotionException
import ru.illine.drinking.ponies.exception.LastAdminException
import ru.illine.drinking.ponies.exception.SelfStateChangeException
import ru.illine.drinking.ponies.exception.TelegramUserNotFoundException
import ru.illine.drinking.ponies.model.base.AdminUserStatusFilter
import ru.illine.drinking.ponies.model.dto.internal.AdminUserDto
import ru.illine.drinking.ponies.model.dto.internal.AdminUserPageDto
import ru.illine.drinking.ponies.model.dto.internal.UserCountsDto
import ru.illine.drinking.ponies.model.dto.internal.UserStateChangeDto
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
        actorId: Long,
        state: UserStateDto,
    ): AdminUserDto {
        require(state.isActive != null || state.isBanned != null || state.isAdmin != null) {
            "At least one state field is required, got an empty payload"
        }
        if (id == actorId) {
            throw SelfStateChangeException("Admin [$actorId] tried to change their own state")
        }
        val user = requireUser(id)
        when {
            state.isAdmin == true -> requireSignInPossible(id, state, user)
            state.isAdmin == false && user.isAdmin -> requireAnotherAdminLeft(id)
        }

        logger.info("Setting {} for user [{}]", state, id)
        val applied =
            telegramUserAccessService.updateState(
                id = id,
                actorId = actorId,
                change =
                    UserStateChangeDto(
                        deleted = state.isActive?.not(),
                        banned = state.isBanned,
                        admin = state.isAdmin,
                    ),
            )

        return user.copy(deleted = applied.isDeleted, isBanned = applied.isBanned, isAdmin = applied.isAdmin)
    }

    private fun requireSignInPossible(
        id: Long,
        state: UserStateDto,
        user: AdminUserDto,
    ) {
        val banned = state.isBanned ?: user.isBanned
        val deleted = state.isActive?.not() ?: user.deleted

        if (banned || deleted) {
            throw InactiveUserPromotionException("User [$id] would stay locked out, so there is nothing to promote")
        }
    }

    private fun requireAnotherAdminLeft(id: Long) {
        if (telegramUserAccessService.findActiveAdminIdsForUpdate().singleOrNull() == id) {
            throw LastAdminException("Demoting user [$id] would leave the system without an admin")
        }
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

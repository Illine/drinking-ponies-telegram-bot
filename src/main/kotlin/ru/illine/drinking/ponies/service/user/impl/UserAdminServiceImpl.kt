package ru.illine.drinking.ponies.service.user.impl

import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.query.EscapeCharacter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.illine.drinking.ponies.dao.access.TelegramUserAccessService
import ru.illine.drinking.ponies.dao.repository.AdminUserProjection
import ru.illine.drinking.ponies.exception.TelegramUserNotFoundException
import ru.illine.drinking.ponies.model.base.AdminUserStatusFilter
import ru.illine.drinking.ponies.model.dto.response.ShortUserInfo
import ru.illine.drinking.ponies.model.dto.response.UserCounts
import ru.illine.drinking.ponies.model.dto.response.UserDetailsResponse
import ru.illine.drinking.ponies.model.dto.response.UsersResponse
import ru.illine.drinking.ponies.service.user.UserAdminService
import ru.illine.drinking.ponies.util.statistics.toUtcInstant

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
    ): UsersResponse {
        val query = search?.takeIf { it.isNotBlank() }?.toSearchPattern()
        val users = telegramUserAccessService.findAllForAdmin(query, status, PageRequest.of(page, size))

        // Counted without the status filter on purpose: the admin page shows every chip at once.
        // The total of the requested status is one of those counters, so no extra count query.
        val counts =
            telegramUserAccessService.countForAdmin(query).let {
                UserCounts(it.all, it.active, it.inactive, it.banned)
            }

        return UsersResponse(
            users = users.map { it.toShortUserInfo() },
            page = page,
            size = size,
            total = counts.of(status),
            counts = counts,
        )
    }

    override fun getUser(id: Long): UserDetailsResponse = requireUser(id).toDetails()

    @Transactional
    override fun updateState(
        id: Long,
        isActive: Boolean?,
    ): UserDetailsResponse {
        require(isActive != null) { "At least one state field is required, got an empty payload" }

        val user = requireUser(id)

        logger.info("Setting isActive={} for user [{}]", isActive, id)
        telegramUserAccessService.updateDeleted(id, user.externalUserId, deleted = !isActive)

        return user.toDetails(isActive)
    }

    // Telegram usernames legitimately contain "_", which LIKE reads as "any character" - without
    // escaping, a search for alice_p would also match aliceXp. EscapeCharacter.DEFAULT escapes with
    // a backslash, which is what the query declares in its "escape" clause.
    private fun String.toSearchPattern(): String = "%${EscapeCharacter.DEFAULT.escape(this)}%"

    private fun requireUser(id: Long): AdminUserProjection =
        telegramUserAccessService.findByIdForAdmin(id)
            ?: throw TelegramUserNotFoundException("No user with id: [$id]")

    private fun UserCounts.of(status: AdminUserStatusFilter): Long =
        when (status) {
            AdminUserStatusFilter.ALL -> all
            AdminUserStatusFilter.ACTIVE -> active
            AdminUserStatusFilter.INACTIVE -> inactive
            AdminUserStatusFilter.BANNED -> banned
        }

    private fun AdminUserProjection.toShortUserInfo(): ShortUserInfo =
        ShortUserInfo(
            id = id,
            externalUserId = externalUserId,
            firstName = firstName,
            lastName = lastName,
            username = username,
            isAdmin = admin,
            isBanned = banned,
            isActive = !deleted,
            lastActivity = lastActivity?.toUtcInstant(),
        )

    // isActive is passed in when the caller has just changed it, so the freshly written value
    // is returned without reading the row again.
    private fun AdminUserProjection.toDetails(isActive: Boolean = !deleted): UserDetailsResponse =
        UserDetailsResponse(
            id = id,
            externalUserId = externalUserId,
            firstName = firstName,
            lastName = lastName,
            username = username,
            isAdmin = admin,
            isBanned = banned,
            isActive = isActive,
            lastActivity = lastActivity?.toUtcInstant(),
            timeZone = timeZone,
            registeredAt = created.toUtcInstant(),
        )
}

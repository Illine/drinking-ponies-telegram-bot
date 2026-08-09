package ru.illine.drinking.ponies.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.illine.drinking.ponies.config.web.security.AdminOnly
import ru.illine.drinking.ponies.mapper.AdminUserResponseMapper
import ru.illine.drinking.ponies.model.base.AdminUserStatusFilter
import ru.illine.drinking.ponies.model.dto.internal.TelegramAuthUserDto
import ru.illine.drinking.ponies.model.dto.internal.UserStateDto
import ru.illine.drinking.ponies.model.dto.request.UserStateRequest
import ru.illine.drinking.ponies.model.dto.response.UserDetailsResponse
import ru.illine.drinking.ponies.model.dto.response.UsersResponse
import ru.illine.drinking.ponies.service.user.UserAdminService
import ru.illine.drinking.ponies.util.telegram.TelegramGeneralConstants

@RestController
@RequestMapping("/users")
@Validated
@AdminOnly
@Tag(name = "User administration", description = "Operations over other users' accounts")
class UserAdminController(
    private val userAdminService: UserAdminService,
) {
    @GetMapping
    @Operation(summary = "List users, soft-deleted ones included")
    fun getUsers(
        @Parameter(description = "Free text over name, username and both ids", example = "alice")
        @RequestParam(name = "search", required = false) search: String?,
        @Parameter(description = "Status chip to filter by")
        @RequestParam(name = "status", required = false, defaultValue = "ALL") status: AdminUserStatusFilter,
        @Parameter(description = "Zero-based page number", example = "0")
        @RequestParam(name = "page", required = false, defaultValue = "0")
        @Min(0) page: Int,
        @Parameter(description = "Page size", example = "20")
        @RequestParam(name = "size", required = false, defaultValue = "20")
        @Min(1)
        @Max(100)
        size: Int,
    ): UsersResponse = AdminUserResponseMapper.toResponse(userAdminService.getUsers(search, status, page, size))

    @GetMapping("/{id}")
    @Operation(summary = "Get a single user card")
    fun getUser(
        @Parameter(description = "Internal user id", example = "1042")
        @PathVariable(name = "id") id: Long,
    ): UserDetailsResponse = AdminUserResponseMapper.toDetails(userAdminService.getUser(id))

    @PatchMapping("/{id}")
    @Operation(summary = "Update user state: soft delete, restore, ban or unban")
    fun updateUserState(
        @Parameter(description = "Internal user id", example = "1042")
        @PathVariable(name = "id") id: Long,
        @Parameter(hidden = true)
        @RequestAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE) actor: TelegramAuthUserDto,
        @Valid @RequestBody request: UserStateRequest,
    ): UserDetailsResponse {
        val state = UserStateDto(isActive = request.isActive, isBanned = request.isBanned)
        val actorId = requireNotNull(actor.id) { "An admin without a stored account cannot reach this endpoint" }

        return AdminUserResponseMapper.toDetails(userAdminService.updateState(id, actorId, state))
    }
}

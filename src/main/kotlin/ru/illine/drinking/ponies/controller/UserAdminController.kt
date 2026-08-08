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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.illine.drinking.ponies.config.web.security.AdminOnly
import ru.illine.drinking.ponies.model.base.AdminUserStatusFilter
import ru.illine.drinking.ponies.model.dto.request.UserStateRequest
import ru.illine.drinking.ponies.model.dto.response.UserDetailsResponse
import ru.illine.drinking.ponies.model.dto.response.UsersResponse
import ru.illine.drinking.ponies.service.user.UserAdminService

// Shares the /users path with UserController on purpose: same resource, different audience.
// @AdminOnly sits on the class so a new endpoint here is closed by default.
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
        @Max(MAX_PAGE_SIZE)
        size: Int,
    ): UsersResponse = userAdminService.getUsers(search, status, page, size)

    @GetMapping("/{id}")
    @Operation(summary = "Get a single user card")
    fun getUser(
        @Parameter(description = "Internal user id", example = "1042")
        @PathVariable(name = "id") id: Long,
    ): UserDetailsResponse = userAdminService.getUser(id)

    @PatchMapping("/{id}")
    @Operation(summary = "Update user state: soft delete or restore")
    fun updateUserState(
        @Parameter(description = "Internal user id", example = "1042")
        @PathVariable(name = "id") id: Long,
        @Valid @RequestBody request: UserStateRequest,
    ): UserDetailsResponse = userAdminService.updateState(id, request.isActive)

    companion object {
        private const val MAX_PAGE_SIZE = 100L
    }
}

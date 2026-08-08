package ru.illine.drinking.ponies.model.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A page of users for the admin page")
data class UsersResponse(
    @Schema(description = "Users of the requested page, most recently active first")
    val users: List<ShortUserInfo>,
    @Schema(description = "Zero-based page number", example = "0")
    val page: Int,
    @Schema(description = "Page size", example = "20")
    val size: Int,
    @Schema(description = "How many users match the request, all pages included", example = "8")
    val total: Long,
    @Schema(description = "Counters for the status chips")
    val counts: UserCounts,
)

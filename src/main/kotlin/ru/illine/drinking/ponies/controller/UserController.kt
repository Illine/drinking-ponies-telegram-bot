package ru.illine.drinking.ponies.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.illine.drinking.ponies.model.dto.TelegramUserDto
import ru.illine.drinking.ponies.model.dto.response.MeResponse
import ru.illine.drinking.ponies.util.telegram.TelegramGeneralConstants

@RestController
@RequestMapping("/users")
@Tag(name = "User", description = "Current user identity and profile")
class UserController {

    @GetMapping("/me")
    @Operation(summary = "Get current user identity")
    fun getMe(
        @Parameter(hidden = true)
        @RequestAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE) telegramUser: TelegramUserDto,
    ): MeResponse = MeResponse(
        telegramUserId = telegramUser.telegramId,
        isAdmin = telegramUser.isAdmin,
    )
}

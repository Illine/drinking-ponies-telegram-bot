package ru.illine.drinking.ponies.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.illine.drinking.ponies.mapper.NotificationHistoryResponseMapper
import ru.illine.drinking.ponies.mapper.PauseStateResponseMapper
import ru.illine.drinking.ponies.model.dto.internal.TelegramAuthUserDto
import ru.illine.drinking.ponies.model.dto.request.NotificationHistoryEntryRequest
import ru.illine.drinking.ponies.model.dto.response.NotificationHistoryEvent
import ru.illine.drinking.ponies.model.dto.response.NotificationHistoryResponse
import ru.illine.drinking.ponies.model.dto.response.NotificationNextResponse
import ru.illine.drinking.ponies.model.dto.response.PauseStateResponse
import ru.illine.drinking.ponies.service.notification.NotificationHistoryService
import ru.illine.drinking.ponies.service.notification.NotificationSettingsService
import ru.illine.drinking.ponies.util.telegram.TelegramGeneralConstants
import java.time.LocalDate

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "Notification management")
@Validated
class NotificationController(
    private val notificationSettingsService: NotificationSettingsService,
    private val notificationHistoryService: NotificationHistoryService,
) {
    @GetMapping("/next")
    @Operation(summary = "Get next notification time")
    fun getNextNotification(
        @Parameter(hidden = true)
        @RequestAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE) telegramUser: TelegramAuthUserDto,
    ): NotificationNextResponse {
        val nextAt = notificationSettingsService.getNextNotificationAt(telegramUser.externalUserId)
        return NotificationNextResponse(nextNotificationAt = nextAt)
    }

    @GetMapping("/pause")
    @Operation(summary = "Get notification pause state")
    fun getPauseState(
        @Parameter(hidden = true)
        @RequestAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE) telegramUser: TelegramAuthUserDto,
    ): PauseStateResponse =
        PauseStateResponseMapper.toResponse(notificationSettingsService.getPauseState(telegramUser.externalUserId))

    @PutMapping("/pause")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Pause or cancel pause for notifications")
    fun changePause(
        @Parameter(hidden = true)
        @RequestAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE) telegramUser: TelegramAuthUserDto,
        @Parameter(description = "Pause duration in minutes (0 = cancel pause, max 300 = 5 hours)", example = "60")
        @RequestParam(name = "minutes", required = true)
        @Min(0)
        @Max(300) minutes: Long,
    ) {
        if (minutes == 0L) {
            notificationSettingsService.cancelPause(telegramUser.externalUserId)
        } else {
            notificationSettingsService.pauseNotifications(telegramUser.externalUserId, minutes)
        }
    }

    @GetMapping("/history")
    @Operation(summary = "Get the notification journal for the requested period")
    fun getHistory(
        @Parameter(hidden = true)
        @RequestAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE) telegramUser: TelegramAuthUserDto,
        @Parameter(
            description = "Range start (inclusive) in yyyy-MM-dd format, in the user's timezone",
            example = "2026-05-01",
            schema = Schema(type = "string", format = "date"),
        )
        @RequestParam(name = "from", required = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @Parameter(
            description = "Range end (inclusive) in yyyy-MM-dd format, in the user's timezone",
            example = "2026-05-31",
            schema = Schema(type = "string", format = "date"),
        )
        @RequestParam(name = "to", required = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): NotificationHistoryResponse =
        NotificationHistoryResponseMapper.toResponse(
            notificationHistoryService.getHistory(telegramUser.externalUserId, from, to),
        )

    @PatchMapping("/history/{id}")
    @Operation(summary = "Update a notification journal entry")
    fun updateHistoryEntry(
        @Parameter(hidden = true)
        @RequestAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE) telegramUser: TelegramAuthUserDto,
        @Parameter(description = "Journal entry identifier", example = "1042")
        @PathVariable(name = "id") id: Long,
        @Valid @RequestBody request: NotificationHistoryEntryRequest,
    ): NotificationHistoryEvent =
        NotificationHistoryResponseMapper.toEvent(
            notificationHistoryService.updateEntry(
                externalUserId = telegramUser.externalUserId,
                entryId = id,
                status = request.status,
                amountMl = request.amountMl,
            ),
        )
}

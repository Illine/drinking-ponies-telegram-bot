package ru.illine.drinking.ponies.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import ru.illine.drinking.ponies.builder.StatisticsBuilder
import ru.illine.drinking.ponies.builder.WaterStatisticBuilder
import ru.illine.drinking.ponies.model.dto.TelegramUserDto
import ru.illine.drinking.ponies.model.dto.request.WaterEntryRequest
import ru.illine.drinking.ponies.model.dto.response.StatisticsResponse
import ru.illine.drinking.ponies.model.dto.response.StatisticsTodayResponse
import ru.illine.drinking.ponies.service.statistic.StatisticsService
import ru.illine.drinking.ponies.service.statistic.WaterStatisticService
import ru.illine.drinking.ponies.util.telegram.TelegramGeneralConstants
import java.time.LocalDate

@RestController
@RequestMapping("/statistics")
@Validated
@Tag(name = "Statistics", description = "Water intake statistics")
class StatisticsController(
    private val statisticsService: StatisticsService,
    private val waterStatisticService: WaterStatisticService,
) {

    @GetMapping("/today")
    @Operation(summary = "Get today's water intake events")
    fun getToday(
        @Parameter(hidden = true)
        @RequestAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE) telegramUser: TelegramUserDto,
    ): StatisticsTodayResponse {
        val entries = statisticsService.getToday(telegramUser.telegramId)
        return StatisticsTodayResponse(
            entries = entries.map(WaterStatisticBuilder::toWaterEntry),
        )
    }

    @GetMapping
    @Operation(summary = "Get aggregated statistics for the requested period")
    fun getStatistics(
        @Parameter(hidden = true)
        @RequestAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE) telegramUser: TelegramUserDto,
        @Parameter(
            description = "Range start (inclusive) in yyyy-MM-dd format",
            example = "2026-05-01",
            schema = Schema(type = "string", format = "date")
        )
        @RequestParam(name = "from", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @Parameter(
            description = "Range end (inclusive) in yyyy-MM-dd format",
            example = "2026-05-20",
            schema = Schema(type = "string", format = "date")
        )
        @RequestParam(name = "to", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): StatisticsResponse =
        StatisticsBuilder.toResponse(statisticsService.getStatistics(telegramUser.telegramId, from, to))

    @PostMapping("/water")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a manual water intake entry at an arbitrary time")
    fun recordWaterEntry(
        @Parameter(hidden = true)
        @RequestAttribute(TelegramGeneralConstants.TELEGRAM_USER_ATTRIBUTE) telegramUser: TelegramUserDto,
        @Valid @RequestBody request: WaterEntryRequest,
    ) {
        waterStatisticService.manualRecordEvent(
            externalUserId = telegramUser.telegramId,
            consumedAt = request.consumedAt,
            amountMl = request.amountMl,
        )
    }

}

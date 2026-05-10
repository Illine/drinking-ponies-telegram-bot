package ru.illine.drinking.ponies.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.illine.drinking.ponies.builder.WaterStatisticBuilder
import ru.illine.drinking.ponies.model.dto.TelegramUserDto
import ru.illine.drinking.ponies.model.dto.response.StatisticsTodayResponse
import ru.illine.drinking.ponies.service.statistic.StatisticsService
import ru.illine.drinking.ponies.util.telegram.TelegramGeneralConstants

@RestController
@RequestMapping("/statistics")
@Tag(name = "Statistics", description = "Water intake statistics")
class StatisticsController(
    private val statisticsService: StatisticsService,
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
}

package ru.illine.drinking.ponies.test.generator

import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.IntervalNotificationType
import ru.illine.drinking.ponies.model.base.WaterEntrySourceType
import ru.illine.drinking.ponies.model.dto.BestDayDto
import ru.illine.drinking.ponies.model.dto.SettingDto
import ru.illine.drinking.ponies.model.dto.StatisticsDto
import ru.illine.drinking.ponies.model.dto.StatisticsPointDto
import ru.illine.drinking.ponies.model.dto.TelegramUserDto
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramChatDto
import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto
import ru.illine.drinking.ponies.model.dto.message.InsightStatsContext
import ru.illine.drinking.ponies.model.dto.request.WaterEntryRequest
import ru.illine.drinking.ponies.model.dto.response.PauseStateResponse
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.random.Random
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserDto as InternalTelegramUserDto

class DtoGenerator {
    companion object {
        fun generateNotificationDto(
            externalUserId: Long = Random.nextLong(),
            externalChatId: Long = Random.nextLong(),
            notificationInterval: IntervalNotificationType = IntervalNotificationType.HOUR,
            timeOfLastNotification: LocalDateTime = LocalDateTime.now(),
            notificationAttempts: Int = 0,
            userTimeZone: String = "Europe/Moscow",
            previousNotificationMessageId: Int? = null,
            quietModeStart: LocalTime? = null,
            quietModeEnd: LocalTime? = null,
            pauseUntil: LocalDateTime? = null,
            dailyGoalMl: Int = 2000,
        ): NotificationSettingDto {
            val user =
                InternalTelegramUserDto(
                    externalUserId = externalUserId,
                    userTimeZone = userTimeZone,
                )
            val chat =
                TelegramChatDto(
                    telegramUser = user,
                    externalChatId = externalChatId,
                    previousNotificationMessageId = previousNotificationMessageId,
                )
            return NotificationSettingDto(
                telegramUser = user,
                telegramChat = chat,
                notificationInterval = notificationInterval,
                timeOfLastNotification = timeOfLastNotification,
                notificationAttempts = notificationAttempts,
                quietModeStart = quietModeStart,
                quietModeEnd = quietModeEnd,
                pauseUntil = pauseUntil,
                dailyGoalMl = dailyGoalMl,
            )
        }

        fun generateWaterStatisticDto(
            externalUserId: Long = Random.nextLong(),
            eventTime: LocalDateTime = LocalDateTime.now(),
            eventType: AnswerNotificationType = AnswerNotificationType.YES,
            waterAmountMl: Int = 250,
            userTimeZone: String = "Europe/Moscow",
            source: WaterEntrySourceType = WaterEntrySourceType.NOTIFICATION,
        ): WaterStatisticDto {
            val user =
                InternalTelegramUserDto(
                    externalUserId = externalUserId,
                    userTimeZone = userTimeZone,
                )
            return WaterStatisticDto(
                telegramUser = user,
                eventTime = eventTime,
                eventType = eventType,
                waterAmountMl = waterAmountMl,
                source = source,
            )
        }

        fun generateWaterEvent(
            eventTime: LocalDateTime = LocalDateTime.now(),
            waterAmountMl: Int = 250,
        ): WaterStatisticDto =
            generateWaterStatisticDto(
                eventTime = eventTime,
                waterAmountMl = waterAmountMl,
            )

        fun generateInsightStatsContext(
            currentStreakDays: Int = 0,
            avgMlPerDay: Int = 0,
            dailyGoalMl: Int = 2000,
            bestDay: BestDayDto? = null,
        ): InsightStatsContext =
            InsightStatsContext(
                avgMlPerDay = avgMlPerDay,
                bestDay = bestDay,
                currentStreakDays = currentStreakDays,
                dailyGoalMl = dailyGoalMl,
            )

        fun generateBestDayDto(
            date: LocalDate = LocalDate.of(2026, 5, 6),
            valueMl: Int = 2400,
            weekday: DayOfWeek = DayOfWeek.WEDNESDAY,
        ): BestDayDto =
            BestDayDto(
                date = date,
                valueMl = valueMl,
                weekday = weekday,
            )

        fun generateWaterEntryRequest(
            consumedAt: Instant? = Instant.now().minusSeconds(300),
            amountMl: Int = 250,
        ): WaterEntryRequest =
            WaterEntryRequest(
                consumedAt = consumedAt,
                amountMl = amountMl,
            )

        fun generateTelegramUserDto(
            externalUserId: Long = 1L,
            firstName: String? = "First Name",
            lastName: String? = null,
            username: String? = "username",
        ): TelegramUserDto =
            TelegramUserDto(
                externalUserId = externalUserId,
                firstName = firstName,
                lastName = lastName,
                username = username,
            )

        fun generateSettingDto(
            interval: String? = IntervalNotificationType.HOUR_AND_HALF.name,
            intervalDisplayName: String? = IntervalNotificationType.HOUR_AND_HALF.displayName,
            intervalMinutes: Long? = IntervalNotificationType.HOUR_AND_HALF.minutes,
            quietModeStart: String? = "23:00",
            quietModeEnd: String? = "08:00",
            timezone: String? = "America/New_York",
            dailyGoalMl: Int? = 2500,
            notificationActive: Boolean = true,
        ): SettingDto =
            SettingDto(
                interval = interval,
                intervalDisplayName = intervalDisplayName,
                intervalMinutes = intervalMinutes,
                quietModeStart = quietModeStart,
                quietModeEnd = quietModeEnd,
                timezone = timezone,
                dailyGoalMl = dailyGoalMl,
                notificationActive = notificationActive,
            )

        fun generateStatisticsDto(
            points: List<StatisticsPointDto> =
                listOf(
                    StatisticsPointDto("2026-05-04", 1800),
                    StatisticsPointDto("2026-05-05", 2100),
                    StatisticsPointDto("2026-05-06", 2400),
                    StatisticsPointDto("2026-05-07", 1500),
                    StatisticsPointDto("2026-05-08", 0),
                    StatisticsPointDto("2026-05-09", 0),
                    StatisticsPointDto("2026-05-10", 0),
                ),
            dailyGoalMl: Int = 2000,
            averageMlPerDay: Int = 1114,
            bestDay: BestDayDto? = generateBestDayDto(),
            currentStreakDays: Int = 3,
            insightText: String = "insight",
            firstEntryAt: Instant? = Instant.parse("2026-04-15T10:30:00Z"),
        ): StatisticsDto =
            StatisticsDto(
                points = points,
                dailyGoalMl = dailyGoalMl,
                averageMlPerDay = averageMlPerDay,
                bestDay = bestDay,
                currentStreakDays = currentStreakDays,
                insightText = insightText,
                firstEntryAt = firstEntryAt,
            )

        fun generatePauseStateResponse(
            paused: Boolean = true,
            pauseUntil: Instant? = Instant.parse("2025-01-01T18:00:00Z"),
        ): PauseStateResponse =
            PauseStateResponse(
                paused = paused,
                pauseUntil = pauseUntil,
            )
    }
}

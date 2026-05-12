package ru.illine.drinking.ponies.builder

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.illine.drinking.ponies.model.base.StatisticsPeriodType
import ru.illine.drinking.ponies.model.dto.BestDayDto
import ru.illine.drinking.ponies.model.dto.StatisticsDto
import ru.illine.drinking.ponies.model.dto.StatisticsPointDto
import ru.illine.drinking.ponies.test.tag.UnitTest
import java.time.DayOfWeek
import java.time.LocalDate

@UnitTest
@DisplayName("StatisticsBuilder Unit Test")
class StatisticsBuilderTest {

    @Test
    @DisplayName("toResponse(): maps every field from StatisticsDto to StatisticsResponse")
    fun `toResponse maps every field`() {
        val dto = StatisticsDto(
            period = StatisticsPeriodType.WEEK,
            points = listOf(
                StatisticsPointDto("2026-05-04", 1800),
                StatisticsPointDto("2026-05-05", 2100),
            ),
            dailyGoalMl = 2000,
            averageMlPerDay = 1950,
            bestDay = BestDayDto(LocalDate.of(2026, 5, 5), 2100, DayOfWeek.TUESDAY),
            currentStreakDays = 3,
            goalProgress = 0.43,
            insightText = "Котик, ты пьёшь водицу 3 дней подряд - так держать!",
        )

        val response = StatisticsBuilder.toResponse(dto)

        assertEquals(StatisticsPeriodType.WEEK, response.period)
        assertEquals(2, response.points.size)
        assertEquals("2026-05-04", response.points[0].label)
        assertEquals(1800, response.points[0].valueMl)
        assertEquals("2026-05-05", response.points[1].label)
        assertEquals(2100, response.points[1].valueMl)
        assertEquals(2000, response.dailyGoalMl)
        assertEquals(1950, response.averageMlPerDay)
        assertNotNull(response.bestDay)
        assertEquals(LocalDate.of(2026, 5, 5), response.bestDay!!.date)
        assertEquals(2100, response.bestDay!!.valueMl)
        assertEquals(DayOfWeek.TUESDAY, response.bestDay!!.weekday)
        assertEquals(3, response.currentStreakDays)
        assertEquals(0.43, response.goalProgress)
        assertEquals("Котик, ты пьёшь водицу 3 дней подряд - так держать!", response.insight.text)
    }

    @Test
    @DisplayName("toResponse(): null bestDay and null goalProgress map through as null")
    fun `toResponse preserves null nullable fields`() {
        val dto = StatisticsDto(
            period = StatisticsPeriodType.DAY,
            points = emptyList(),
            dailyGoalMl = 2000,
            averageMlPerDay = 0,
            bestDay = null,
            currentStreakDays = 0,
            goalProgress = null,
            insightText = "Здесь пока пусто, котик. Сделай первый глоток.",
        )

        val response = StatisticsBuilder.toResponse(dto)

        assertNull(response.bestDay)
        assertNull(response.goalProgress)
        assertEquals(0, response.points.size)
    }
}

package ru.illine.drinking.ponies.builder

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.illine.drinking.ponies.model.dto.BestDayDto
import ru.illine.drinking.ponies.model.dto.StatisticsDto
import ru.illine.drinking.ponies.model.dto.StatisticsPointDto
import ru.illine.drinking.ponies.model.dto.response.StatisticsResponse
import ru.illine.drinking.ponies.test.tag.UnitTest
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.reflect.full.memberProperties

@UnitTest
@DisplayName("StatisticsBuilder Unit Test")
class StatisticsBuilderTest {

    @Test
    @DisplayName("toResponse(): maps every field from StatisticsDto to StatisticsResponse")
    fun `toResponse maps every field`() {
        val dto = StatisticsDto(
            points = listOf(
                StatisticsPointDto("2026-05-04", 1800),
                StatisticsPointDto("2026-05-05", 2100),
            ),
            dailyGoalMl = 2000,
            averageMlPerDay = 1950,
            bestDay = BestDayDto(LocalDate.of(2026, 5, 5), 2100, DayOfWeek.TUESDAY),
            currentStreakDays = 3,
            insightText = "Котик, ты пьёшь водицу 3 дней подряд - так держать!",
        )

        val response = StatisticsBuilder.toResponse(dto)

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
        assertEquals("Котик, ты пьёшь водицу 3 дней подряд - так держать!", response.insight.text)
    }

    @Test
    @DisplayName("DPTB-127 regression: StatisticsResponse and StatisticsDto no longer expose period/goalProgress")
    fun `removed fields are absent from response and dto`() {
        // The MiniApp does not consume these fields - they were removed from the public contract.
        // Restoring them silently (e.g. via a copy-paste from an older branch) would leak internal
        // state into the API. This contract guard fails fast if either field returns.
        val responseFields = StatisticsResponse::class.memberProperties.map { it.name }.toSet()
        val dtoFields = StatisticsDto::class.memberProperties.map { it.name }.toSet()

        assertFalse(responseFields.contains("period"), "StatisticsResponse must not expose 'period'")
        assertFalse(responseFields.contains("goalProgress"), "StatisticsResponse must not expose 'goalProgress'")
        assertFalse(dtoFields.contains("period"), "StatisticsDto must not expose 'period'")
        assertFalse(dtoFields.contains("goalProgress"), "StatisticsDto must not expose 'goalProgress'")
    }

    @Test
    @DisplayName("toResponse(): null bestDay maps through as null")
    fun `toResponse preserves null bestDay`() {
        val dto = StatisticsDto(
            points = emptyList(),
            dailyGoalMl = 2000,
            averageMlPerDay = 0,
            bestDay = null,
            currentStreakDays = 0,
            insightText = "Здесь пока пусто, котик. Сделай первый глоток.",
        )

        val response = StatisticsBuilder.toResponse(dto)

        assertNull(response.bestDay)
        assertEquals(0, response.points.size)
    }
}

package ru.illine.drinking.ponies.service.message.impl

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.illine.drinking.ponies.model.dto.BestDayDto
import ru.illine.drinking.ponies.model.dto.message.InsightStatsContext
import ru.illine.drinking.ponies.service.message.MessageProvider
import ru.illine.drinking.ponies.test.tag.UnitTest
import ru.illine.drinking.ponies.util.message.MessageSpec
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random

@UnitTest
@DisplayName("LocalMessageProvider Unit Test")
class LocalMessageProviderTest {

    private lateinit var provider: MessageProvider

    @BeforeEach
    fun setUp() {
        // Fixed seed (the same one TestTimeConfig uses) keeps template choice deterministic.
        provider = LocalMessageProvider(Random(42))
    }

    @Test
    @DisplayName("INSIGHT_STATS: empty context -> EMPTY_PERIOD group text")
    fun `empty context picks empty period group`() {
        val ctx = InsightStatsContext(
            avgMlPerDay = 0,
            bestDay = null,
            currentStreakDays = 0,
            dailyGoalMl = 2000
        )

        val result = provider.getMessage(MessageSpec.InsightStats, ctx)

        // Both templates mention an opening sip / first drink and avoid mid-period vocabulary.
        assertTrue(
            result.text.contains("первый глоток") || result.text.contains("в первый раз"),
            "expected empty-period phrasing, got: ${result.text}"
        )
    }

    @Test
    @DisplayName("INSIGHT_STATS: streak >= 3 -> STREAK_HIGH group (beats BEST_DAY when both match)")
    fun `streak high beats best day priority`() {
        // Both STREAK_HIGH and BEST_DAY conditions satisfied; STREAK_HIGH must win on priority.
        val ctx = InsightStatsContext(
            avgMlPerDay = 1800,
            bestDay = BestDayDto(LocalDate.of(2026, 5, 6), 2400, DayOfWeek.WEDNESDAY),
            currentStreakDays = 5,
            dailyGoalMl = 2000
        )

        val result = provider.getMessage(MessageSpec.InsightStats, ctx)

        // STREAK_HIGH templates always contain "подряд" or "по цели" with the streak number.
        assertTrue(
            result.text.contains("подряд") || result.text.contains("по цели"),
            "expected STREAK_HIGH phrasing, got: ${result.text}"
        )
        assertTrue(result.text.contains("5"), "expected streak count in text, got: ${result.text}")
        // BEST_DAY templates inject "среда" - must NOT be present (priority test).
        assertFalse(result.text.contains("среда"), "STREAK_HIGH must win over BEST_DAY: ${result.text}")
        // BEST_DAY templates would contain "2400" - must NOT leak through.
        assertFalse(result.text.contains("2400"), "STREAK_HIGH must not contain bestDay value: ${result.text}")
    }

    @Test
    @DisplayName("INSIGHT_STATS: bestDay set, low streak -> BEST_DAY group with localized weekday")
    fun `best day localizes weekday`() {
        // Wednesday 2026-05-06
        val ctx = InsightStatsContext(
            avgMlPerDay = 1800,
            bestDay = BestDayDto(LocalDate.of(2026, 5, 6), 2400, DayOfWeek.WEDNESDAY),
            currentStreakDays = 0,
            dailyGoalMl = 2000
        )

        val result = provider.getMessage(MessageSpec.InsightStats, ctx)

        assertTrue(result.text.contains("среда"), "expected 'среда' in text, got: ${result.text}")
        assertTrue(result.text.contains("2400"), "expected 2400 in text, got: ${result.text}")
    }

    @Test
    @DisplayName("INSIGHT_STATS: avg >= goal (no streak, no bestDay) -> AVG_GOOD group")
    fun `avg good group`() {
        val ctx = InsightStatsContext(
            avgMlPerDay = 2200,
            bestDay = null,
            currentStreakDays = 0,
            dailyGoalMl = 2000
        )

        val result = provider.getMessage(MessageSpec.InsightStats, ctx)

        assertTrue(
            result.text.contains("2200"),
            "expected avg value in text, got: ${result.text}"
        )
        assertTrue(
            result.text.contains("выше цели") || result.text.contains("так держать"),
            "expected AVG_GOOD phrasing, got: ${result.text}"
        )
    }

    @Test
    @DisplayName("INSIGHT_STATS: avg below goal (no streak, no bestDay) -> AVG_LOW group")
    fun `avg low group`() {
        val ctx = InsightStatsContext(
            avgMlPerDay = 1200,
            bestDay = null,
            currentStreakDays = 0,
            dailyGoalMl = 2000
        )

        val result = provider.getMessage(MessageSpec.InsightStats, ctx)

        assertTrue(result.text.contains("1200"), "expected avg value, got: ${result.text}")
    }

    @Test
    @DisplayName("INSIGHT_STATS: bestDay with valueMl=0 -> falls through to FALLBACK")
    fun `best day zero falls through to fallback`() {
        // best with zero value should not trigger BEST_DAY group; avg=0 also kills AVG groups.
        // Empty-period guard (avg==0 && streak==0 && bestDay==null) is false (bestDay != null),
        // so the chain falls to FALLBACK.
        val ctx = InsightStatsContext(
            avgMlPerDay = 0,
            bestDay = BestDayDto(LocalDate.of(2026, 5, 6), 0, DayOfWeek.WEDNESDAY),
            currentStreakDays = 0,
            dailyGoalMl = 2000
        )

        val result = provider.getMessage(MessageSpec.InsightStats, ctx)

        // FALLBACK template
        assertTrue(
            result.text.contains("ещё можно попить"),
            "expected FALLBACK phrasing, got: ${result.text}"
        )
    }

    @Test
    @DisplayName("INSIGHT_STATS: rendered text never contains unfilled {placeholder} markers")
    fun `no unfilled placeholders in any group`() {
        val unfilledRegex = Regex("\\{[^}]+}")
        val contexts = listOf(
            InsightStatsContext(0, null, 0, 2000),
            InsightStatsContext(1800, BestDayDto(LocalDate.of(2026, 5, 6), 2400, DayOfWeek.WEDNESDAY), 5, 2000),
            InsightStatsContext(1800, BestDayDto(LocalDate.of(2026, 5, 6), 2400, DayOfWeek.WEDNESDAY), 0, 2000),
            InsightStatsContext(2200, null, 0, 2000),
            InsightStatsContext(1200, null, 0, 2000),
            InsightStatsContext(0, BestDayDto(LocalDate.of(2026, 5, 6), 0, DayOfWeek.WEDNESDAY), 0, 2000),
        )

        contexts.forEach { ctx ->
            val result = provider.getMessage(MessageSpec.InsightStats, ctx)

            assertFalse(
                unfilledRegex.containsMatchIn(result.text),
                "unfilled placeholder found in: ${result.text}"
            )
            assertNotNull(result.text)
            assertTrue(result.text.isNotBlank())
        }
    }

    @Test
    @DisplayName("INSIGHT_STATS: stable output with fixed seed across calls (same group, deterministic random)")
    fun `fixed seed yields stable choice within group`() {
        // Two providers with the same seed must return the same template for the same context.
        val p1 = LocalMessageProvider(Random(42))
        val p2 = LocalMessageProvider(Random(42))
        val ctx = InsightStatsContext(
            avgMlPerDay = 1800,
            bestDay = BestDayDto(LocalDate.of(2026, 5, 6), 2400, DayOfWeek.WEDNESDAY),
            currentStreakDays = 0,
            dailyGoalMl = 2000
        )

        val a = p1.getMessage(MessageSpec.InsightStats, ctx).text
        val b = p2.getMessage(MessageSpec.InsightStats, ctx).text

        assertEquals(a, b)
    }

    @Test
    @DisplayName("INSIGHT_STATS: streak boundary (streak=3) triggers STREAK_HIGH group")
    fun `streak boundary three triggers high group`() {
        val ctx = InsightStatsContext(
            avgMlPerDay = 1800,
            bestDay = null,
            currentStreakDays = 3,
            dailyGoalMl = 2000
        )

        val result = provider.getMessage(MessageSpec.InsightStats, ctx)

        assertTrue(result.text.contains("3"), "expected streak number in text, got: ${result.text}")
    }

    @Test
    @DisplayName("INSIGHT_STATS: streak=2 does NOT trigger STREAK_HIGH (falls to AVG_LOW for avg<goal)")
    fun `streak two below threshold`() {
        val ctx = InsightStatsContext(
            avgMlPerDay = 1200,
            bestDay = null,
            currentStreakDays = 2,
            dailyGoalMl = 2000
        )

        val result = provider.getMessage(MessageSpec.InsightStats, ctx)

        // STREAK_HIGH templates contain "подряд"; AVG_LOW does not.
        assertFalse(result.text.contains("подряд"), "streak=2 must not trigger STREAK_HIGH: ${result.text}")
        assertTrue(result.text.contains("1200"), "expected AVG_LOW with avg=1200, got: ${result.text}")
    }

    @Test
    @DisplayName("INSIGHT_STATS: all group-triggering contexts produce non-empty text without crashing")
    fun `all groups render without error`() {
        val rng = Random(1)
        val p = LocalMessageProvider(rng)
        val unfilledRegex = Regex("\\{[^}]+}")

        listOf(
            InsightStatsContext(0, null, 0, 2000),                                                              // EMPTY
            InsightStatsContext(2200, null, 5, 2000),                                                           // STREAK_HIGH
            InsightStatsContext(1800, BestDayDto(LocalDate.of(2026, 5, 6), 2400, DayOfWeek.WEDNESDAY), 0, 2000), // BEST_DAY
            InsightStatsContext(2200, null, 0, 2000),                                                           // AVG_GOOD
            InsightStatsContext(1200, null, 0, 2000),                                                           // AVG_LOW
            InsightStatsContext(0, BestDayDto(LocalDate.of(2026, 5, 6), 0, DayOfWeek.WEDNESDAY), 0, 2000),       // FALLBACK
        ).forEach { ctx ->
            val text = p.getMessage(MessageSpec.InsightStats, ctx).text

            assertTrue(text.isNotBlank(), "blank text for ctx=$ctx")
            assertFalse(unfilledRegex.containsMatchIn(text), "unfilled placeholder for ctx=$ctx: $text")
        }
    }
}

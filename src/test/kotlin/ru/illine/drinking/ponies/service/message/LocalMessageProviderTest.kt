package ru.illine.drinking.ponies.service.message

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.illine.drinking.ponies.model.base.StatisticsPeriodType
import ru.illine.drinking.ponies.model.dto.BestDayDto
import ru.illine.drinking.ponies.model.dto.message.InsightStatsContext
import ru.illine.drinking.ponies.service.message.impl.LocalMessageProvider
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
    @DisplayName("DAY+empty: phrasing references today, not the week")
    fun `day empty references today`() {
        val ctx = InsightStatsContext(
            period = StatisticsPeriodType.DAY,
            avgMlPerDay = 0,
            bestDay = null,
            currentStreakDays = 0,
            dailyGoalMl = 2000,
        )

        val result = provider.getMessage(MessageSpec.InsightStats, ctx)

        Assertions.assertTrue(
            result.text.contains("Сегодня") || result.text.contains("начнём день"),
            "expected today-scoped phrasing, got: ${result.text}"
        )
    }

    @Test
    @DisplayName("WEEK+empty: phrasing references the week")
    fun `week empty references the week`() {
        val ctx = InsightStatsContext(
            period = StatisticsPeriodType.WEEK,
            avgMlPerDay = 0,
            bestDay = null,
            currentStreakDays = 0,
            dailyGoalMl = 2000,
        )

        val result = provider.getMessage(MessageSpec.InsightStats, ctx)

        Assertions.assertTrue(
            result.text.contains("недел"),
            "expected week-scoped phrasing, got: ${result.text}"
        )
    }

    @Test
    @DisplayName("MONTH+empty: phrasing references the month")
    fun `month empty references the month`() {
        val ctx = InsightStatsContext(
            period = StatisticsPeriodType.MONTH,
            avgMlPerDay = 0,
            bestDay = null,
            currentStreakDays = 0,
            dailyGoalMl = 2000,
        )

        val result = provider.getMessage(MessageSpec.InsightStats, ctx)

        Assertions.assertTrue(
            result.text.contains("месяц"),
            "expected month-scoped phrasing, got: ${result.text}"
        )
    }

    @Test
    @DisplayName("WEEK+bestDay: uses localized weekday, not a calendar date")
    fun `week best day uses weekday`() {
        // 2026-05-12 is a Tuesday
        val ctx = InsightStatsContext(
            period = StatisticsPeriodType.WEEK,
            avgMlPerDay = 1800,
            bestDay = BestDayDto(LocalDate.of(2026, 5, 12), 1500, DayOfWeek.TUESDAY),
            currentStreakDays = 0,
            dailyGoalMl = 2000,
        )

        val result = provider.getMessage(MessageSpec.InsightStats, ctx)

        Assertions.assertTrue(result.text.contains("вторник"), "expected weekday 'вторник', got: ${result.text}")
        Assertions.assertTrue(result.text.contains("1500"), "expected bestDay value, got: ${result.text}")
        // Date "12 мая" must NOT leak into WEEK phrasing.
        Assertions.assertFalse(result.text.contains("12 мая"), "WEEK must not use a calendar date: ${result.text}")
    }

    @Test
    @DisplayName("MONTH+bestDay: uses calendar date (d MMMM ru), not weekday")
    fun `month best day uses calendar date`() {
        // 2026-05-12 is a Tuesday
        val ctx = InsightStatsContext(
            period = StatisticsPeriodType.MONTH,
            avgMlPerDay = 96,
            bestDay = BestDayDto(LocalDate.of(2026, 5, 12), 1500, DayOfWeek.TUESDAY),
            currentStreakDays = 0,
            dailyGoalMl = 2000,
        )

        val result = provider.getMessage(MessageSpec.InsightStats, ctx)

        Assertions.assertTrue(result.text.contains("12 мая"), "expected calendar date '12 мая', got: ${result.text}")
        Assertions.assertTrue(result.text.contains("1500"), "expected bestDay value, got: ${result.text}")
        // Weekday "вторник" must NOT leak into MONTH phrasing.
        Assertions.assertFalse(result.text.contains("вторник"), "MONTH must not use a weekday: ${result.text}")
    }

    @Test
    @DisplayName("DAY+goal reached: phrasing mentions today and the value")
    fun `day goal reached`() {
        val ctx = InsightStatsContext(
            period = StatisticsPeriodType.DAY,
            avgMlPerDay = 2200,
            bestDay = null,
            currentStreakDays = 0,
            dailyGoalMl = 2000,
        )

        val result = provider.getMessage(MessageSpec.InsightStats, ctx)

        Assertions.assertTrue(result.text.contains("2200"), "expected value 2200 in text, got: ${result.text}")
        Assertions.assertTrue(
            result.text.contains("Цель") || result.text.contains("перебрала"),
            "expected goal-reached phrasing, got: ${result.text}"
        )
    }

    @Test
    @DisplayName("DAY+partial: phrasing mentions consumed and remaining ml")
    fun `day partial mentions remaining`() {
        val ctx = InsightStatsContext(
            period = StatisticsPeriodType.DAY,
            avgMlPerDay = 800,
            bestDay = null,
            currentStreakDays = 0,
            dailyGoalMl = 2000,
        )

        val result = provider.getMessage(MessageSpec.InsightStats, ctx)

        Assertions.assertTrue(result.text.contains("800"), "expected consumed value 800, got: ${result.text}")
        Assertions.assertTrue(result.text.contains("1200"), "expected remaining 1200 (2000-800), got: ${result.text}")
    }

    @Test
    @DisplayName("streak >= 3 + bestDay: ACHIEVEMENT bucket shares airtime between STREAK_HIGH and BEST_DAY")
    fun `achievement bucket shares airtime between streak high and best day`() {
        // DAY excluded on purpose: streak phrasing ("N дней подряд") is meaningless on a single-day view,
        // and DAY has no BEST_DAY rule.
        // 2026-05-06 is a Wednesday - WEEK phrasing uses weekday ("среда"), MONTH uses date ("6 мая").
        val bestDayMarker = mapOf(
            StatisticsPeriodType.WEEK to "среда",
            StatisticsPeriodType.MONTH to "6 мая",
        )
        listOf(StatisticsPeriodType.WEEK, StatisticsPeriodType.MONTH).forEach { period ->
            val ctx = InsightStatsContext(
                period = period,
                avgMlPerDay = 1800,
                bestDay = BestDayDto(LocalDate.of(2026, 5, 6), 2400, DayOfWeek.WEDNESDAY),
                currentStreakDays = 5,
                dailyGoalMl = 2000,
            )

            // Both outcomes must be reachable across seeds; the picker is random so we sample broadly.
            val outcomes = (0L until 100L).map { seed ->
                LocalMessageProvider(Random(seed)).getMessage(MessageSpec.InsightStats, ctx).text
            }
            Assertions.assertTrue(
                outcomes.any { it.contains("подряд") || it.contains("по цели") },
                "[$period] STREAK_HIGH never picked across 100 seeds"
            )
            Assertions.assertTrue(
                outcomes.any { it.contains(bestDayMarker.getValue(period)) },
                "[$period] BEST_DAY never picked across 100 seeds"
            )
            // Every outcome must belong to the ACHIEVEMENT bucket - no AVG/FALLBACK leakage when ACHIEVEMENT matches.
            val isAchievement: (String) -> Boolean = {
                it.contains("подряд") || it.contains("по цели") || it.contains(bestDayMarker.getValue(period))
            }
            Assertions.assertTrue(outcomes.all(isAchievement)) {
                "[$period] ACHIEVEMENT bucket leaked: ${outcomes.firstOrNull { !isAchievement(it) }}"
            }
        }
    }

    @Test
    @DisplayName("DAY: streak phrasing is intentionally absent - high streak does not surface STREAK_HIGH")
    fun `day has no streak rule`() {
        // Streak is a multi-day concept and has no meaning on a single-day view. Even with streak=5,
        // the DAY chain must NOT produce STREAK_HIGH phrasing - falls into avg-low instead.
        val ctx = InsightStatsContext(
            period = StatisticsPeriodType.DAY,
            avgMlPerDay = 800,
            bestDay = null,
            currentStreakDays = 5,
            dailyGoalMl = 2000,
        )

        val result = provider.getMessage(MessageSpec.InsightStats, ctx)

        Assertions.assertFalse(result.text.contains("подряд"), "DAY must not surface streak phrasing: ${result.text}")
        Assertions.assertFalse(result.text.contains("по цели"), "DAY must not surface streak phrasing: ${result.text}")
    }

    @Test
    @DisplayName("WEEK+avg >= goal: AVG_GOOD phrasing with the avg value")
    fun `week avg good`() {
        val ctx = InsightStatsContext(
            period = StatisticsPeriodType.WEEK,
            avgMlPerDay = 2200,
            bestDay = null,
            currentStreakDays = 0,
            dailyGoalMl = 2000,
        )

        val result = provider.getMessage(MessageSpec.InsightStats, ctx)

        Assertions.assertTrue(result.text.contains("2200"), "expected avg value in text, got: ${result.text}")
        Assertions.assertTrue(
            result.text.contains("выше цели") || result.text.contains("так держать"),
            "expected AVG_GOOD phrasing, got: ${result.text}"
        )
    }

    @Test
    @DisplayName("WEEK+avg below goal: AVG_LOW phrasing with the avg value")
    fun `week avg low`() {
        val ctx = InsightStatsContext(
            period = StatisticsPeriodType.WEEK,
            avgMlPerDay = 1200,
            bestDay = null,
            currentStreakDays = 0,
            dailyGoalMl = 2000,
        )

        val result = provider.getMessage(MessageSpec.InsightStats, ctx)

        Assertions.assertTrue(result.text.contains("1200"), "expected avg value, got: ${result.text}")
    }

    @Test
    @DisplayName("WEEK+bestDay valueMl=0 falls through to FALLBACK")
    fun `week best day zero falls through to fallback`() {
        val ctx = InsightStatsContext(
            period = StatisticsPeriodType.WEEK,
            avgMlPerDay = 0,
            bestDay = BestDayDto(LocalDate.of(2026, 5, 6), 0, DayOfWeek.WEDNESDAY),
            currentStreakDays = 0,
            dailyGoalMl = 2000,
        )

        val result = provider.getMessage(MessageSpec.InsightStats, ctx)

        Assertions.assertTrue(
            result.text.contains("До конца недели"),
            "expected WEEK FALLBACK phrasing, got: ${result.text}"
        )
    }

    @Test
    @DisplayName("DAY: fallback phrasing references today (regression DPTB-126: must not say week/month)")
    fun `day fallback references today`() {
        // avgMlPerDay > 0 but >= goal? Use avg > 0 < goal would hit AVG_LOW. Pick a state hitting only the wildcard:
        // EMPTY requires avg==0 && streak==0. STREAK_HIGH requires streak>=3. GOAL/PARTIAL need avg>0.
        // streak=1, avg=0 -> EMPTY predicate fails (streak!=0), STREAK_HIGH fails, GOAL/PARTIAL fail (avg=0) -> FALLBACK.
        val ctx = InsightStatsContext(
            period = StatisticsPeriodType.DAY,
            avgMlPerDay = 0,
            bestDay = null,
            currentStreakDays = 1,
            dailyGoalMl = 2000,
        )

        val result = provider.getMessage(MessageSpec.InsightStats, ctx)

        Assertions.assertTrue(
            result.text.contains("Сегодня ещё можно попить"),
            "expected DAY FALLBACK phrasing, got: ${result.text}"
        )
        // Regression: DAY must NOT borrow WEEK/MONTH phrasing.
        Assertions.assertFalse(result.text.contains("недел"), "DAY fallback must not mention week: ${result.text}")
        Assertions.assertFalse(result.text.contains("месяц"), "DAY fallback must not mention month: ${result.text}")
    }

    @Test
    @DisplayName("MONTH: fallback phrasing references the month (regression DPTB-126)")
    fun `month fallback references month`() {
        // streak=1, avg=0, bestDay=null -> EMPTY fails (streak!=0), STREAK_HIGH fails, BEST_DAY fails,
        // GOAL/PARTIAL fail (avg=0) -> FALLBACK.
        val ctx = InsightStatsContext(
            period = StatisticsPeriodType.MONTH,
            avgMlPerDay = 0,
            bestDay = null,
            currentStreakDays = 1,
            dailyGoalMl = 2000,
        )

        val result = provider.getMessage(MessageSpec.InsightStats, ctx)

        Assertions.assertTrue(
            result.text.contains("До конца месяца"),
            "expected MONTH FALLBACK phrasing, got: ${result.text}"
        )
        // Regression: MONTH must NOT borrow WEEK phrasing.
        Assertions.assertFalse(
            result.text.contains("До конца недели"),
            "MONTH fallback must not mention week: ${result.text}"
        )
    }

    @Test
    @DisplayName("DAY+bestDay valueMl=0 falls through to FALLBACK (bestDay has no rule on DAY)")
    fun `day best day zero falls through to fallback`() {
        // On DAY there is no BEST_DAY rule at all; with avg=0 && streak=0 && bestDay!=null the EMPTY
        // predicate (avgMlPerDay==0 && currentStreakDays==0) still matches, so we use streak=1 to skip EMPTY too.
        val ctx = InsightStatsContext(
            period = StatisticsPeriodType.DAY,
            avgMlPerDay = 0,
            bestDay = BestDayDto(LocalDate.of(2026, 5, 6), 0, DayOfWeek.WEDNESDAY),
            currentStreakDays = 1,
            dailyGoalMl = 2000,
        )

        val result = provider.getMessage(MessageSpec.InsightStats, ctx)

        Assertions.assertTrue(
            result.text.contains("Сегодня ещё можно попить"),
            "expected DAY FALLBACK phrasing, got: ${result.text}"
        )
    }

    @Test
    @DisplayName("MONTH+bestDay valueMl=0 falls through to FALLBACK (bestDay rule guarded by valueMl>0)")
    fun `month best day zero falls through to fallback`() {
        val ctx = InsightStatsContext(
            period = StatisticsPeriodType.MONTH,
            avgMlPerDay = 0,
            bestDay = BestDayDto(LocalDate.of(2026, 5, 6), 0, DayOfWeek.WEDNESDAY),
            currentStreakDays = 0,
            dailyGoalMl = 2000,
        )

        val result = provider.getMessage(MessageSpec.InsightStats, ctx)

        Assertions.assertTrue(
            result.text.contains("До конца месяца"),
            "expected MONTH FALLBACK phrasing, got: ${result.text}"
        )
        // BEST_DAY rule must NOT have fired - no calendar date in text.
        Assertions.assertFalse(result.text.contains("6 мая"), "BEST_DAY rule must not fire on valueMl=0: ${result.text}")
    }

    @Test
    @DisplayName("MONTH+bestDay: russian genitive month names rendered via CLDR for jan/may/aug/dec")
    fun `month best day renders russian genitive month names`() {
        // CLDR locale provider renders "d MMMM" in ru-RU as genitive (января, мая, августа, декабря).
        // The legacy JRE locale provider would render nominative (январь, май, август, декабрь).
        // This test guards against a CI/JDK that ships without CLDR as the default provider.
        val cases = listOf(
            // date, weekday, expected substring
            Triple(LocalDate.of(2026, 1, 1), DayOfWeek.THURSDAY, "1 января"),
            Triple(LocalDate.of(2026, 5, 12), DayOfWeek.TUESDAY, "12 мая"),
            Triple(LocalDate.of(2026, 8, 15), DayOfWeek.SATURDAY, "15 августа"),
            Triple(LocalDate.of(2026, 12, 31), DayOfWeek.THURSDAY, "31 декабря"),
        )

        cases.forEach { (date, weekday, expectedFragment) ->
            val ctx = InsightStatsContext(
                period = StatisticsPeriodType.MONTH,
                avgMlPerDay = 96,
                bestDay = BestDayDto(date, 1500, weekday),
                currentStreakDays = 0,
                dailyGoalMl = 2000,
            )

            val result = provider.getMessage(MessageSpec.InsightStats, ctx)

            Assertions.assertTrue(
                result.text.contains(expectedFragment),
                "expected '$expectedFragment' (russian genitive) for $date, got: ${result.text}"
            )
        }
    }

    @Test
    @DisplayName("rendered text never contains unfilled {placeholder} markers across all periods/groups")
    fun `no unfilled placeholders across periods and groups`() {
        val unfilledRegex = Regex("\\{[^}]+}")
        val date = LocalDate.of(2026, 5, 6)
        val bestDay = BestDayDto(date, 2400, DayOfWeek.WEDNESDAY)

        val contexts = StatisticsPeriodType.values().flatMap { period ->
            listOf(
                InsightStatsContext(period, 0, null, 0, 2000),
                InsightStatsContext(period, 1800, bestDay, 5, 2000),
                InsightStatsContext(period, 1800, bestDay, 0, 2000),
                InsightStatsContext(period, 2200, null, 0, 2000),
                InsightStatsContext(period, 1200, null, 0, 2000),
                InsightStatsContext(period, 0, BestDayDto(date, 0, DayOfWeek.WEDNESDAY), 0, 2000),
            )
        }

        contexts.forEach { ctx ->
            val result = provider.getMessage(MessageSpec.InsightStats, ctx)

            Assertions.assertFalse(
                unfilledRegex.containsMatchIn(result.text),
                "unfilled placeholder for ctx=$ctx: ${result.text}"
            )
            Assertions.assertTrue(result.text.isNotBlank(), "blank text for ctx=$ctx")
        }
    }

    @Test
    @DisplayName("fixed seed yields stable choice across providers")
    fun `fixed seed yields stable choice`() {
        val p1 = LocalMessageProvider(Random(42))
        val p2 = LocalMessageProvider(Random(42))
        val ctx = InsightStatsContext(
            period = StatisticsPeriodType.WEEK,
            avgMlPerDay = 1800,
            bestDay = BestDayDto(LocalDate.of(2026, 5, 6), 2400, DayOfWeek.WEDNESDAY),
            currentStreakDays = 0,
            dailyGoalMl = 2000,
        )

        val a = p1.getMessage(MessageSpec.InsightStats, ctx).text
        val b = p2.getMessage(MessageSpec.InsightStats, ctx).text

        Assertions.assertEquals(a, b)
    }

    @Test
    @DisplayName("streak boundary (streak=3) triggers STREAK_HIGH group")
    fun `streak boundary three triggers high group`() {
        val ctx = InsightStatsContext(
            period = StatisticsPeriodType.WEEK,
            avgMlPerDay = 1800,
            bestDay = null,
            currentStreakDays = 3,
            dailyGoalMl = 2000,
        )

        val result = provider.getMessage(MessageSpec.InsightStats, ctx)

        Assertions.assertTrue(result.text.contains("3"), "expected streak number in text, got: ${result.text}")
    }

    @Test
    @DisplayName("streak=2 does NOT trigger STREAK_HIGH")
    fun `streak two below threshold`() {
        val ctx = InsightStatsContext(
            period = StatisticsPeriodType.WEEK,
            avgMlPerDay = 1200,
            bestDay = null,
            currentStreakDays = 2,
            dailyGoalMl = 2000,
        )

        val result = provider.getMessage(MessageSpec.InsightStats, ctx)

        Assertions.assertFalse(result.text.contains("подряд"), "streak=2 must not trigger STREAK_HIGH: ${result.text}")
        Assertions.assertTrue(result.text.contains("1200"), "expected AVG_LOW with avg=1200, got: ${result.text}")
    }

    @Test
    @DisplayName("ACHIEVEMENT bucket: only STREAK_HIGH matches when bestDay=null - in-bucket filter selects single candidate")
    fun `achievement bucket in-bucket filter picks only matching candidate`() {
        // WEEK with streak=5 and bestDay=null:
        //   - EMPTY predicate fails (streak != 0)
        //   - ACHIEVEMENT bucket: STREAK_HIGH matches (streak >= 3), BEST_DAY predicate fails (bestDay == null)
        // The provider must keep ACHIEVEMENT bucket (bucket.filter(...).isNotEmpty()) and pick STREAK_HIGH.
        // No BEST_DAY phrasing must surface; no leakage into AVG/FALLBACK is allowed.
        val ctx = InsightStatsContext(
            period = StatisticsPeriodType.WEEK,
            avgMlPerDay = 1800,
            bestDay = null,
            currentStreakDays = 5,
            dailyGoalMl = 2000,
        )

        // Sample broadly so randomness inside the bucket cannot mask a leak.
        val outcomes = (0L until 100L).map { seed ->
            LocalMessageProvider(Random(seed)).getMessage(MessageSpec.InsightStats, ctx).text
        }

        Assertions.assertTrue(outcomes.all { it.contains("подряд") || it.contains("по цели") }) {
            "expected only STREAK_HIGH phrasing, leak found: ${outcomes.firstOrNull { !(it.contains("подряд") || it.contains("по цели")) }}"
        }
        Assertions.assertTrue(outcomes.all { it.contains("5") }, "expected streak number 5 in all outcomes")
        // Defensive: no AVG/FALLBACK phrasing leaks even when only one candidate in the bucket matches.
        Assertions.assertTrue(outcomes.none { it.contains("В среднем") }, "AVG leaked into ACHIEVEMENT result")
        Assertions.assertTrue(outcomes.none { it.contains("До конца недели") }, "FALLBACK leaked into ACHIEVEMENT result")
    }

    @Test
    @DisplayName("first bucket (EMPTY) skipped when no candidate matches, second bucket (ACHIEVEMENT) wins")
    fun `bucket priority skips empty first bucket and picks next matching bucket`() {
        // WEEK with avg=0, streak=5, bestDay=null:
        //   - EMPTY predicate (avg==0 && streak==0 && bestDay==null) fails because streak=5
        //   - ACHIEVEMENT bucket: STREAK_HIGH matches
        // This guards the buckets.firstNotNullOf { ... takeIf { isNotEmpty() } } contract:
        // an empty bucket must be skipped, not produce an error or pick fallback prematurely.
        val ctx = InsightStatsContext(
            period = StatisticsPeriodType.WEEK,
            avgMlPerDay = 0,
            bestDay = null,
            currentStreakDays = 5,
            dailyGoalMl = 2000,
        )

        val result = provider.getMessage(MessageSpec.InsightStats, ctx)

        Assertions.assertTrue(
            result.text.contains("подряд") || result.text.contains("по цели"),
            "expected STREAK_HIGH after skipping EMPTY bucket, got: ${result.text}"
        )
        // EMPTY bucket phrasing must NOT have been selected.
        Assertions.assertFalse(
            result.text.contains("пока пустая") || result.text.contains("начнём неделю"),
            "EMPTY bucket leaked despite predicate mismatch: ${result.text}"
        )
    }

    @Test
    @DisplayName("DAY: high streak never produces STREAK_HIGH across many seeds (no leakage from cross-period rules)")
    fun `day never surfaces streak high across many seeds`() {
        // DPTB-126 regression guard: STREAK_HIGH_RULE is shared between WEEK and MONTH buckets only.
        // DAY_BUCKETS must NOT include the rule under any period-gate. Sampling broadly catches
        // accidental cross-period leakage that a single-seed test might miss.
        val ctx = InsightStatsContext(
            period = StatisticsPeriodType.DAY,
            avgMlPerDay = 0,
            bestDay = null,
            currentStreakDays = 7,
            dailyGoalMl = 2000,
        )

        val outcomes = (0L until 100L).map { seed ->
            LocalMessageProvider(Random(seed)).getMessage(MessageSpec.InsightStats, ctx).text
        }

        Assertions.assertTrue(outcomes.none { it.contains("подряд") }) {
            "DAY leaked STREAK_HIGH: ${outcomes.firstOrNull { it.contains("подряд") }}"
        }
        Assertions.assertTrue(outcomes.none { it.contains("по цели") }) {
            "DAY leaked STREAK_HIGH: ${outcomes.firstOrNull { it.contains("по цели") }}"
        }
    }

    @Test
    @DisplayName("all group-triggering contexts produce non-empty text across all periods")
    fun `all groups render without error`() {
        val rng = Random(1)
        val p = LocalMessageProvider(rng)
        val unfilledRegex = Regex("\\{[^}]+}")
        val date = LocalDate.of(2026, 5, 6)
        val bestDay = BestDayDto(date, 2400, DayOfWeek.WEDNESDAY)

        StatisticsPeriodType.values().forEach { period ->
            listOf(
                InsightStatsContext(period, 0, null, 0, 2000),                  // EMPTY
                InsightStatsContext(period, 2200, null, 5, 2000),               // STREAK_HIGH
                InsightStatsContext(period, 1800, bestDay, 0, 2000),            // BEST_DAY (DAY has no BEST_DAY rule -> AVG_LOW)
                InsightStatsContext(period, 2200, null, 0, 2000),               // AVG_GOOD
                InsightStatsContext(period, 1200, null, 0, 2000),               // AVG_LOW
                InsightStatsContext(period, 0, BestDayDto(date, 0, DayOfWeek.WEDNESDAY), 0, 2000), // FALLBACK
            ).forEach { ctx ->
                val text = p.getMessage(MessageSpec.InsightStats, ctx).text

                Assertions.assertTrue(text.isNotBlank(), "blank text for ctx=$ctx")
                Assertions.assertFalse(unfilledRegex.containsMatchIn(text), "unfilled placeholder for ctx=$ctx: $text")
            }
        }
    }
}
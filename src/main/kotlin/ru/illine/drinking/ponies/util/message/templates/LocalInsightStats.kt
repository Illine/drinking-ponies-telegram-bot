package ru.illine.drinking.ponies.util.message.templates

import ru.illine.drinking.ponies.model.base.StatisticsPeriodType
import ru.illine.drinking.ponies.model.dto.message.InsightStatsContext
import ru.illine.drinking.ponies.util.message.TemplateRule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

object LocalInsightStats {

    private const val STREAK_HIGH_THRESHOLD = 3

    private val RU_LOCALE: Locale = Locale.forLanguageTag("ru")

    // Renders as "12 мая" - MMMM uses CLDR "format" (contextual) form, which is genitive in ru-RU.
    private val DAY_MONTH_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMMM", RU_LOCALE)

    private fun weekdayRu(weekday: DayOfWeek): String =
        weekday.getDisplayName(TextStyle.FULL, RU_LOCALE)

    private fun dayMonthRu(date: LocalDate): String =
        date.format(DAY_MONTH_FORMATTER)

    // STREAK_HIGH phrasing is identical across periods - one canonical rule, reused in WEEK/MONTH lists.
    private val STREAK_HIGH_RULE: TemplateRule<InsightStatsContext> = TemplateRule(
        predicate = { it.currentStreakDays >= STREAK_HIGH_THRESHOLD },
        templates = listOf(
            { "Котик, ты пьёшь водицу ${it.currentStreakDays} дней подряд - так держать!" },
            { "Уже ${it.currentStreakDays} дней подряд по цели, солнышко" }
        )
    )

    private val DAY_RULES: List<TemplateRule<InsightStatsContext>> = listOf(
        TemplateRule(
            predicate = { it.avgMlPerDay == 0 && it.currentStreakDays == 0 },
            templates = listOf(
                { "Сегодня пока пусто, котик. Сделай первый глоток" },
                { "Солнышко, начнём день - выпей водицу в первый раз!" }
            )
        ),
        TemplateRule(
            predicate = { it.avgMlPerDay >= it.dailyGoalMl },
            templates = listOf(
                { "Цель на сегодня уже взята, солнышко - ${it.avgMlPerDay} мл" },
                { "Сегодня перебрала цель, котик - ${it.avgMlPerDay} мл" }
            )
        ),
        TemplateRule(
            predicate = { it.avgMlPerDay > 0 && it.avgMlPerDay < it.dailyGoalMl },
            templates = listOf(
                { "Сегодня выпито ${it.avgMlPerDay} мл - до цели ещё ${it.dailyGoalMl - it.avgMlPerDay} мл, котик" },
                { "Уже ${it.avgMlPerDay} мл сегодня - осталось ${it.dailyGoalMl - it.avgMlPerDay} мл до цели, солнышко" }
            )
        ),
        TemplateRule(
            predicate = { true },
            templates = listOf(
                { "Сегодня ещё можно попить воды, солнышко!" }
            )
        )
    )

    private val WEEK_RULES: List<TemplateRule<InsightStatsContext>> = listOf(
        TemplateRule(
            predicate = { it.avgMlPerDay == 0 && it.currentStreakDays == 0 && it.bestDay == null },
            templates = listOf(
                { "Эта неделя пока пустая, котик. Сделай первый глоток" },
                { "Солнышко, начнём неделю - выпей водицу в первый раз!" }
            )
        ),
        STREAK_HIGH_RULE,
        // bestDay !! is guarded by the predicate above (bestDay != null && valueMl > 0).
        TemplateRule(
            predicate = { it.bestDay != null && it.bestDay.valueMl > 0 },
            templates = listOf(
                { "Лучший день недели - ${weekdayRu(it.bestDay!!.weekday)} (${it.bestDay.valueMl} мл)" },
                { "Пик недели - ${weekdayRu(it.bestDay!!.weekday)}: ${it.bestDay.valueMl} мл водицы" }
            )
        ),
        TemplateRule(
            predicate = { it.avgMlPerDay >= it.dailyGoalMl },
            templates = listOf(
                { "В среднем ${it.avgMlPerDay} мл в день - выше цели, молодец" },
                { "Котик, в среднем по ${it.avgMlPerDay} мл - так держать" }
            )
        ),
        TemplateRule(
            predicate = { it.avgMlPerDay > 0 && it.avgMlPerDay < it.dailyGoalMl },
            templates = listOf(
                { "В среднем ${it.avgMlPerDay} мл - давай чуть больше водицы, котик" },
                { "Не дотягиваем чуть-чуть: ${it.avgMlPerDay} мл при цели ${it.dailyGoalMl} мл" }
            )
        ),
        TemplateRule(
            predicate = { true },
            templates = listOf(
                { "До конца недели ещё успеешь налить водицы, солнышко!" }
            )
        )
    )

    private val MONTH_RULES: List<TemplateRule<InsightStatsContext>> = listOf(
        TemplateRule(
            predicate = { it.avgMlPerDay == 0 && it.currentStreakDays == 0 && it.bestDay == null },
            templates = listOf(
                { "Этот месяц пока пустой, котик. Сделай первый глоток" },
                { "Солнышко, начнём месяц - выпей водицу в первый раз!" }
            )
        ),
        STREAK_HIGH_RULE,
        // bestDay !! is guarded by the predicate above (bestDay != null && valueMl > 0).
        TemplateRule(
            predicate = { it.bestDay != null && it.bestDay.valueMl > 0 },
            templates = listOf(
                { "Лучший день месяца - ${dayMonthRu(it.bestDay!!.date)} (${it.bestDay.valueMl} мл)" },
                { "Пик месяца - ${dayMonthRu(it.bestDay!!.date)}: ${it.bestDay.valueMl} мл водицы" }
            )
        ),
        TemplateRule(
            predicate = { it.avgMlPerDay >= it.dailyGoalMl },
            templates = listOf(
                { "В среднем ${it.avgMlPerDay} мл в день - выше цели, молодец" },
                { "Котик, в среднем по ${it.avgMlPerDay} мл - так держать" }
            )
        ),
        TemplateRule(
            predicate = { it.avgMlPerDay > 0 && it.avgMlPerDay < it.dailyGoalMl },
            templates = listOf(
                { "В среднем ${it.avgMlPerDay} мл - давай чуть больше водицы, котик" },
                { "Не дотягиваем чуть-чуть: ${it.avgMlPerDay} мл при цели ${it.dailyGoalMl} мл" }
            )
        ),
        TemplateRule(
            predicate = { true },
            templates = listOf(
                { "До конца месяца ещё успеешь налить водицы, солнышко!" }
            )
        )
    )

    val RULES: List<TemplateRule<InsightStatsContext>> = listOf(
        StatisticsPeriodType.DAY to DAY_RULES,
        StatisticsPeriodType.WEEK to WEEK_RULES,
        StatisticsPeriodType.MONTH to MONTH_RULES,
    ).flatMap { (period, rules) ->
        rules.map { rule ->
            TemplateRule(
                predicate = { it.period == period && rule.predicate(it) },
                templates = rule.templates
            )
        }
    }

}
package ru.illine.drinking.ponies.util.message.templates

import ru.illine.drinking.ponies.model.dto.message.InsightStatsContext
import ru.illine.drinking.ponies.util.message.TemplateRule
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.*

object LocalInsightStats {

    private const val STREAK_HIGH_THRESHOLD = 3

    private val RU_LOCALE: Locale = Locale.forLanguageTag("ru")

    private fun weekdayRu(weekday: DayOfWeek): String =
        weekday.getDisplayName(TextStyle.FULL, RU_LOCALE)

    val RULES: List<TemplateRule<InsightStatsContext>> = listOf(
        TemplateRule(
            predicate = { it.avgMlPerDay == 0 && it.currentStreakDays == 0 && it.bestDay == null },
            templates = listOf(
                { "Здесь пока пусто, котик. Сделай первый глоток." },
                { "Солнышко, начнём заново - выпей водицу в первый раз!" }
            )
        ),
        TemplateRule(
            predicate = { it.currentStreakDays >= STREAK_HIGH_THRESHOLD },
            templates = listOf(
                { "Котик, ты пьёшь водицу ${it.currentStreakDays} дней подряд - так держать!" },
                { "Уже ${it.currentStreakDays} дней подряд по цели, солнышко." }
            )
        ),
        TemplateRule(
            predicate = { it.bestDay != null && it.bestDay.valueMl > 0 },
            templates = listOf(
                { "Твой лучший день - ${weekdayRu(it.bestDay!!.weekday)} (${it.bestDay.valueMl} мл)." },
                { "Пик недели - ${weekdayRu(it.bestDay!!.weekday)}: ${it.bestDay.valueMl} мл водицы." }
            )
        ),
        TemplateRule(
            predicate = { it.avgMlPerDay >= it.dailyGoalMl },
            templates = listOf(
                { "Средняя ${it.avgMlPerDay} мл в день - выше цели, молодец." },
                { "Котик, в среднем по ${it.avgMlPerDay} мл - так держать." }
            )
        ),
        TemplateRule(
            predicate = { it.avgMlPerDay > 0 && it.avgMlPerDay < it.dailyGoalMl },
            templates = listOf(
                { "В среднем ${it.avgMlPerDay} мл - давай чуть больше водицы, котик." },
                { "Не дотягиваем чуть-чуть: ${it.avgMlPerDay} мл при цели ${it.dailyGoalMl} мл." }
            )
        ),
        TemplateRule(
            predicate = { true },
            templates = listOf(
                { "Сегодня ещё можно попить воды, солнышко." }
            )
        )
    )

}
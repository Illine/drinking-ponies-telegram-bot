package ru.illine.drinking.ponies.util.message.templates

import ru.illine.drinking.ponies.model.dto.message.InsightStatsContext
import ru.illine.drinking.ponies.util.PluralizationHelper.pluralizeDays
import ru.illine.drinking.ponies.util.message.RuleBucket
import ru.illine.drinking.ponies.util.message.TemplateRule

object LocalInsightStats {
    val PHRASES: List<RuleBucket<InsightStatsContext>> =
        listOf(
            // Specific signals
            listOf(
                TemplateRule(
                    predicate = { it.currentStreakDays in 2..6 },
                    templates =
                        listOf(
                            {
                                "${it.currentStreakDays} ${pluralizeDays(
                                    it.currentStreakDays,
                                )} подряд - поняши заметили!"
                            },
                            { "${it.currentStreakDays} ${pluralizeDays(it.currentStreakDays)} - и ритм пошёл!" },
                        ),
                ),
                TemplateRule(
                    predicate = { it.currentStreakDays in 7..13 },
                    templates =
                        listOf(
                            {
                                "${it.currentStreakDays} ${pluralizeDays(
                                    it.currentStreakDays,
                                )} - это уже не случайность!"
                            },
                            { "Столько дней выпивать норму подряд - чувствуешь как вырабатывается привычка?" },
                        ),
                ),
                TemplateRule(
                    predicate = { it.currentStreakDays >= 14 },
                    templates =
                        listOf(
                            { "${it.currentStreakDays} ${pluralizeDays(it.currentStreakDays)} - это уже ритуал :)" },
                            {
                                "${it.currentStreakDays} ${pluralizeDays(
                                    it.currentStreakDays,
                                )} подряд - поняши аплодируют тебе!"
                            },
                            { "${it.currentStreakDays} ${pluralizeDays(it.currentStreakDays)} - ого!" },
                        ),
                ),
                TemplateRule(
                    predicate = { it.avgMlPerDay >= it.dailyGoalMl && it.dailyGoalMl > 0 },
                    templates =
                        listOf(
                            { "В среднем выше цели - поняши шепчутся, что ты самое гидрированное солнышко!" },
                            { "Среднее перебирает план - ты молодец, но перепей :)" },
                        ),
                ),
                TemplateRule(
                    predicate = { it.bestDay != null },
                    templates =
                        listOf(
                            { "Лучший день - ${it.bestDay!!.valueMl} мл, помнишь как это было?" },
                            { "День с ${it.bestDay!!.valueMl} мл воды - вот это размах :)" },
                        ),
                ),
                // Generic fallback - used only if nothing specific matched
                TemplateRule(
                    predicate = { true },
                    templates =
                        listOf(
                            { "Поняши тобой довольны!" },
                            { "Поняши машут хвостами в твою честь" },
                            { "Твой внутренний пони в восторге!" },
                            { "Кто-то тут молодец, и это ты" },
                            { "Маленькая пони тебе кивает" },
                            { "Кактусы тебе завидуют" },
                            { "Радуга где-то поднялась в твою честь" },
                            { "Твои почки шлют сердечко" },
                            { "Тело тебя слышит, и это редко" },
                            { "Без громких слов - ты просто молодец!" },
                            { "Заметно, что ты стараешься!" },
                            { "Тихий прогресс - самый честный" },
                            { "Самое важное - не останавливаться!" },
                        ),
                ),
            ),
        )
}

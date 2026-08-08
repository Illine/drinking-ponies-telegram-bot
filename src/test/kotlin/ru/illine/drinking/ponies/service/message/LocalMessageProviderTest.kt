package ru.illine.drinking.ponies.service.message

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.illine.drinking.ponies.exception.MessageTemplateException
import ru.illine.drinking.ponies.model.dto.internal.DefaultSettingsContext
import ru.illine.drinking.ponies.model.dto.internal.GreetingContext
import ru.illine.drinking.ponies.model.dto.internal.NoContext
import ru.illine.drinking.ponies.model.dto.internal.NotificationQuestionEditedContext
import ru.illine.drinking.ponies.model.dto.internal.NotificationSuspendContext
import ru.illine.drinking.ponies.service.message.impl.LocalMessageProvider
import ru.illine.drinking.ponies.test.generator.DtoGenerator
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
        provider = LocalMessageProvider(Random(42))
    }

    @Test
    @DisplayName("getMessage(): streak in 2..6 phrasing is reachable and includes 'подряд'")
    fun `streak small bucket contains number and word`() {
        val outcomes =
            (0L until 50L).map { seed ->
                LocalMessageProvider(Random(seed))
                    .getMessage(
                        MessageSpec.InsightStats,
                        DtoGenerator.generateInsightStatsContext(currentStreakDays = 3),
                    ).text
            }
        assertTrue(outcomes.any { it.contains("3") }, "expected streak number 3 at least once")
        assertTrue(outcomes.any { it.contains("подряд") }, "expected 'подряд' phrasing at least once")
    }

    @Test
    @DisplayName("getMessage(): streak boundary streak=2 triggers the 2..6 rule")
    fun `streak boundary two`() {
        val result =
            provider
                .getMessage(
                    MessageSpec.InsightStats,
                    DtoGenerator.generateInsightStatsContext(currentStreakDays = 2),
                ).text

        assertTrue(result.contains("2"), "expected streak number 2, got: $result")
    }

    @Test
    @DisplayName("getMessage(): streak in 7..13 references the streak (no off-by-one in bucket boundary)")
    fun `streak medium bucket`() {
        val outcomes =
            (0L until 50L).map { seed ->
                LocalMessageProvider(Random(seed))
                    .getMessage(
                        MessageSpec.InsightStats,
                        DtoGenerator.generateInsightStatsContext(currentStreakDays = 7),
                    ).text
            }
        assertTrue(
            outcomes.any { it.contains("7") || it.contains("привычка") },
            "expected medium streak phrasing, sample: ${outcomes.first()}",
        )
    }

    @Test
    @DisplayName("getMessage(): streak >= 14 long-bucket phrasing is reachable")
    fun `streak long bucket`() {
        val outcomes =
            (0L until 50L).map { seed ->
                LocalMessageProvider(Random(seed))
                    .getMessage(
                        MessageSpec.InsightStats,
                        DtoGenerator.generateInsightStatsContext(currentStreakDays = 30),
                    ).text
            }
        assertTrue(
            outcomes.any { it.contains("30") || it.contains("ритуал") },
            "long streak phrasing must be reachable across seeds",
        )
    }

    @Test
    @DisplayName("getMessage(): streak phrase includes correct russian plural form (1 -> день, 2 -> дня, 5 -> дней)")
    fun `streak pluralization`() {
        val outcomes2 =
            (0L until 30L).map { seed ->
                LocalMessageProvider(Random(seed))
                    .getMessage(
                        MessageSpec.InsightStats,
                        DtoGenerator.generateInsightStatsContext(currentStreakDays = 2),
                    ).text
            }
        assertTrue(outcomes2.any { it.contains("2 дня") }, "expected '2 дня' at least once, got: ${outcomes2.first()}")

        val outcomes5 =
            (0L until 30L).map { seed ->
                LocalMessageProvider(Random(seed))
                    .getMessage(
                        MessageSpec.InsightStats,
                        DtoGenerator.generateInsightStatsContext(currentStreakDays = 5),
                    ).text
            }
        assertTrue(
            outcomes5.any { it.contains("5 дней") },
            "expected '5 дней' at least once, got: ${outcomes5.first()}",
        )
    }

    @Test
    @DisplayName("getMessage(): avg >= goal produces AVG_GOOD phrasing in specific bucket")
    fun `avg above goal triggers specific bucket`() {
        val outcomes =
            (0L until 50L).map { seed ->
                LocalMessageProvider(Random(seed))
                    .getMessage(
                        MessageSpec.InsightStats,
                        DtoGenerator.generateInsightStatsContext(avgMlPerDay = 2200, dailyGoalMl = 2000),
                    ).text
            }
        assertTrue(
            outcomes.any { it.contains("выше цели") || it.contains("перебирает") },
            "expected AVG_GOOD phrasing at least once across seeds",
        )
    }

    @Test
    @DisplayName("getMessage(): avg >= goal but goal <= 0 does NOT trigger AVG_GOOD (guards against bogus daily goal)")
    fun `avg above non-positive goal does not trigger specific bucket`() {
        val outcomes =
            (0L until 100L).map { seed ->
                LocalMessageProvider(Random(seed))
                    .getMessage(
                        MessageSpec.InsightStats,
                        DtoGenerator.generateInsightStatsContext(
                            currentStreakDays = 0,
                            avgMlPerDay = 0,
                            dailyGoalMl = 0,
                            bestDay = null,
                        ),
                    ).text
            }

        outcomes.forEach { text ->
            assertFalse(
                text.contains("выше цели") || text.contains("перебирает"),
                "AVG_GOOD must not trigger when dailyGoalMl <= 0, got: $text",
            )
            assertTrue(text.isNotBlank(), "fallback must produce non-empty text")
        }
    }

    @Test
    @DisplayName("getMessage(): bestDay set with valueMl > 0 renders BEST_DAY phrasing reachably")
    fun `bestDay rule surfaces value`() {
        val outcomes =
            (0L until 100L).map { seed ->
                LocalMessageProvider(Random(seed))
                    .getMessage(
                        MessageSpec.InsightStats,
                        DtoGenerator.generateInsightStatsContext(
                            bestDay =
                                DtoGenerator.generateBestDayDto(
                                    date = LocalDate.of(2026, 5, 6),
                                    valueMl = 2400,
                                    weekday = DayOfWeek.WEDNESDAY,
                                ),
                        ),
                    ).text
            }
        assertTrue(outcomes.any { it.contains("2400") }, "expected bestDay value 2400 at least once")
    }

    @Test
    @DisplayName("getMessage(): specific and fallback rules both reachable when only streak rule matches")
    fun `specific and fallback mix when streak matches`() {
        val outcomes =
            (0L until 100L).map { seed ->
                LocalMessageProvider(Random(seed))
                    .getMessage(
                        MessageSpec.InsightStats,
                        DtoGenerator.generateInsightStatsContext(currentStreakDays = 5),
                    ).text
            }
        val fallbackMarkers =
            listOf("довольны", "тебе кивает", "сердечко", "почки", "Кактусы", "стараешься", "прогресс", "молодец")
        assertTrue(outcomes.any { it.contains("5") }, "expected streak phrasing at least once")
        assertTrue(
            outcomes.any { text -> fallbackMarkers.any { text.contains(it) } },
            "expected fallback phrasing at least once (rules compete with equal priority)",
        )
    }

    @Test
    @DisplayName("getMessage(): fallback bucket wins when no specific rule matches (streak=0, avg<goal, bestDay=null)")
    fun `fallback when nothing specific matches`() {
        val outcomes =
            (0L until 100L).map { seed ->
                LocalMessageProvider(Random(seed))
                    .getMessage(
                        MessageSpec.InsightStats,
                        DtoGenerator.generateInsightStatsContext(
                            currentStreakDays = 0,
                            avgMlPerDay = 0,
                            bestDay = null,
                        ),
                    ).text
            }
        val specificMarkers = listOf("подряд", "выше цели", "перебирает", "Лучший день", "размах")
        outcomes.forEach { text ->
            assertFalse(
                specificMarkers.any { it in text },
                "specific phrasing leaked into fallback context: $text",
            )
            assertTrue(text.isNotBlank(), "fallback must produce non-empty text")
        }
    }

    @Test
    @DisplayName(
        "getMessage(): bestDay with valueMl=0 still matches specific bucket (rule only checks non-null bestDay)",
    )
    fun `bestDay zero value still matches specific bucket`() {
        val text =
            provider
                .getMessage(
                    MessageSpec.InsightStats,
                    DtoGenerator.generateInsightStatsContext(
                        bestDay =
                            DtoGenerator.generateBestDayDto(
                                date = LocalDate.of(2026, 5, 6),
                                valueMl = 0,
                                weekday = DayOfWeek.WEDNESDAY,
                            ),
                    ),
                ).text

        assertTrue(
            text.contains("0 мл") || text.contains("День с 0"),
            "expected BEST_DAY phrasing with 0 ml, got: $text",
        )
    }

    @Test
    @DisplayName("getMessage(): fixed seed yields stable text across instances")
    fun `fixed seed stable`() {
        val p1 = LocalMessageProvider(Random(42))
        val p2 = LocalMessageProvider(Random(42))
        val context = DtoGenerator.generateInsightStatsContext(currentStreakDays = 5)

        val a = p1.getMessage(MessageSpec.InsightStats, context).text
        val b = p2.getMessage(MessageSpec.InsightStats, context).text

        assertEquals(a, b)
    }

    @Test
    @DisplayName("getMessage(): registered spec always produces non-blank text")
    fun `unregistered spec throws`() {
        val text = provider.getMessage(MessageSpec.InsightStats, DtoGenerator.generateInsightStatsContext()).text
        assertTrue(text.isNotBlank())
    }

    @Test
    @DisplayName("getMessage(): no rule match falls back to fallback bucket and produces text")
    fun `no rule throws`() {
        val text =
            provider
                .getMessage(
                    MessageSpec.InsightStats,
                    DtoGenerator.generateInsightStatsContext(currentStreakDays = 0, avgMlPerDay = 0, bestDay = null),
                ).text

        assertTrue(text.isNotBlank(), "fallback bucket must always produce text")
        assertThrows(MessageTemplateException::class.java) {
            throw MessageTemplateException("test")
        }
    }

    @Test
    @DisplayName("getMessage(): rendered text never contains unfilled {placeholder} markers")
    fun `no unfilled placeholders`() {
        val unfilled = Regex("\\{[^}]+}")
        val bestDay =
            DtoGenerator.generateBestDayDto(
                date = LocalDate.of(2026, 5, 6),
                valueMl = 2400,
                weekday = DayOfWeek.WEDNESDAY,
            )

        val contexts =
            listOf(
                DtoGenerator.generateInsightStatsContext(),
                DtoGenerator.generateInsightStatsContext(currentStreakDays = 1),
                DtoGenerator.generateInsightStatsContext(currentStreakDays = 5),
                DtoGenerator.generateInsightStatsContext(currentStreakDays = 10),
                DtoGenerator.generateInsightStatsContext(currentStreakDays = 30),
                DtoGenerator.generateInsightStatsContext(avgMlPerDay = 2200),
                DtoGenerator.generateInsightStatsContext(avgMlPerDay = 1000),
                DtoGenerator.generateInsightStatsContext(bestDay = bestDay),
                DtoGenerator.generateInsightStatsContext(currentStreakDays = 5, bestDay = bestDay, avgMlPerDay = 2200),
                DtoGenerator.generateInsightStatsContext(
                    bestDay =
                        DtoGenerator.generateBestDayDto(
                            date = LocalDate.of(2026, 5, 6),
                            valueMl = 0,
                            weekday = DayOfWeek.WEDNESDAY,
                        ),
                ),
            )

        contexts.forEach { c ->
            val text = provider.getMessage(MessageSpec.InsightStats, c).text

            assertTrue(text.isNotBlank(), "blank text for ctx=$c")
            assertFalse(unfilled.containsMatchIn(text), "unfilled placeholder for ctx=$c: $text")
        }
    }

    @Test
    @DisplayName("getMessage(): NotificationAnswerYes returns the static confirmation phrase")
    fun `notification answer yes static phrase`() {
        val text = provider.getMessage(MessageSpec.NotificationAnswerYes, NoContext).text

        assertEquals("Ты - солнышко! Так держать!", text)
    }

    @Test
    @DisplayName("getMessage(): NotificationAnswerCancel returns the static cancel phrase")
    fun `notification answer cancel static phrase`() {
        val text = provider.getMessage(MessageSpec.NotificationAnswerCancel, NoContext).text

        assertEquals("Милый зайчик, пожалуйста, напейся в следующий раз!", text)
    }

    @Test
    @DisplayName("getMessage(): NotificationWaterAmountMenu returns the static menu prompt")
    fun `notification water amount menu static phrase`() {
        val text = provider.getMessage(MessageSpec.NotificationWaterAmountMenu, NoContext).text

        assertEquals("Сколько водицы выпито?", text)
    }

    @Test
    @DisplayName("getMessage(): NotificationSnoozeMenu returns the static menu prompt")
    fun `notification snooze menu static phrase`() {
        val text = provider.getMessage(MessageSpec.NotificationSnoozeMenu, NoContext).text

        assertEquals("Выбери, на сколько хочешь отложить уведомление", text)
    }

    @Test
    @DisplayName("getMessage(): NotificationQuestionEdited substitutes the answer display name")
    fun `notification question edited substitutes answer`() {
        val text =
            provider
                .getMessage(
                    MessageSpec.NotificationQuestionEdited,
                    NotificationQuestionEditedContext("Выпил"),
                ).text

        assertEquals("Водица выпита?\nБыло выбрано: *Выпил*", text)
    }

    @Test
    @DisplayName("getMessage(): NotificationSuspend substitutes the duration display name")
    fun `notification suspend substitutes duration`() {
        val text =
            provider
                .getMessage(
                    MessageSpec.NotificationSuspend,
                    NotificationSuspendContext("10 минут"),
                ).text

        assertEquals(
            "Котик, твое уведомление отложено!\nЧерез 10 минут я тебя снова побеспокою, жди!",
            text,
        )
    }

    @Test
    @DisplayName("getMessage(): Greeting substitutes the user name")
    fun `greeting substitutes user name`() {
        val text = provider.getMessage(MessageSpec.Greeting, GreetingContext("Женя")).text

        assertEquals(
            "Здравствуй, Женя!\n" +
                "Я бот Пьющие Поняшки. Я полностью понимающий и знаю, что всем нужно пить!",
            text,
        )
    }

    @Test
    @DisplayName("getMessage(): DefaultSettings substitutes the interval display name")
    fun `default settings substitutes interval`() {
        val text = provider.getMessage(MessageSpec.DefaultSettings, DefaultSettingsContext("каждый час")).text

        assertEquals(
            "Установлены настройки по-умолчанию:\n" +
                "Периодичность уведомлений: каждый час\n" +
                "Часовой пояс: Москва\n" +
                "Время тихого режима: с 23:00 до 11:00",
            text,
        )
    }
}

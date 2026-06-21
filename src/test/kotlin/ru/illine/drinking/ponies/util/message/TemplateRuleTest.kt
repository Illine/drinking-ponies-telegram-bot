package ru.illine.drinking.ponies.util.message

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.illine.drinking.ponies.model.dto.message.InsightStatsContext
import ru.illine.drinking.ponies.test.tag.UnitTest

@UnitTest
@DisplayName("TemplateRule Unit Test")
class TemplateRuleTest {
    @Test
    @DisplayName("init: empty templates list is rejected (rule must carry at least one template)")
    fun `empty templates rejected`() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                TemplateRule<InsightStatsContext>(
                    predicate = { true },
                    templates = emptyList(),
                )
            }

        assertEquals("TemplateRule must have at least one template", exception.message)
    }

    @Test
    @DisplayName("init: non-empty templates list is accepted")
    fun `non-empty templates accepted`() {
        val rule =
            TemplateRule<InsightStatsContext>(
                predicate = { true },
                templates = listOf({ "phrase" }),
            )

        assertTrue(
            rule.predicate(
                InsightStatsContext(avgMlPerDay = 0, bestDay = null, currentStreakDays = 0, dailyGoalMl = 0),
            ),
        )
        assertEquals(1, rule.templates.size)
    }
}

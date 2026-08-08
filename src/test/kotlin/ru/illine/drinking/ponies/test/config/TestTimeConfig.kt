package ru.illine.drinking.ponies.test.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import ru.illine.drinking.ponies.test.util.TestClockHelper
import java.time.Clock
import java.time.Instant
import kotlin.random.Random

@TestConfiguration
class TestTimeConfig {
    @Bean
    fun clock(): Clock =
        TestClockHelper.MutableClock(
            Instant.parse(TestClockHelper.DEFAULT_TIME),
            TestClockHelper.DEFAULT_ZONE,
        )

    @Bean
    fun random(): Random = Random(42)
}

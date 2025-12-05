package ru.illine.drinking.ponies.test.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import ru.illine.drinking.ponies.test.util.ClockHelperTest
import java.time.Clock
import java.time.Instant

@TestConfiguration
class TestTimeConfig {

    @Bean
    fun clock(): Clock {
        return ClockHelperTest.MutableClock(
            Instant.parse(ClockHelperTest.DEFAULT_TIME), ClockHelperTest.DEFAULT_ZONE
        )
    }

}
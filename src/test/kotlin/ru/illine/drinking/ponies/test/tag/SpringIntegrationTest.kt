package ru.illine.drinking.ponies.test.tag

import org.junit.jupiter.api.Tag
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import ru.illine.drinking.ponies.DrinkingPoniesApplication
import ru.illine.drinking.ponies.test.config.TestDatabaseConfig
import ru.illine.drinking.ponies.test.config.TestTimeConfig

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("spring-integration")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [
        DrinkingPoniesApplication::class,
        TestDatabaseConfig::class,
        TestTimeConfig::class
    ]
)
@ActiveProfiles("integration-test")
annotation class SpringIntegrationTest

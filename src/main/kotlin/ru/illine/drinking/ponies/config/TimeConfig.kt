package ru.illine.drinking.ponies.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import kotlin.random.Random

@Configuration
class TimeConfig {

    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun random(): Random = Random.Default

}

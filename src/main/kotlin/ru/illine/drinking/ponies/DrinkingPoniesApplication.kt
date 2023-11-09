package ru.illine.drinking.ponies

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@ConfigurationPropertiesScan
@SpringBootApplication
class DrinkingPoniesApplication

fun main(args: Array<String>) {
    runApplication<DrinkingPoniesApplication>(*args)
}

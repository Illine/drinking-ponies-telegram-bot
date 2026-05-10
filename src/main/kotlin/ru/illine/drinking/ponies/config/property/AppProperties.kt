package ru.illine.drinking.ponies.config.property

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotBlank

@Validated
@ConfigurationProperties("app")
data class AppProperties(

    @NotBlank
    val version: String = "local"
)

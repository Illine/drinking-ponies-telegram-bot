package ru.illine.drinking.ponies.config.property

import jakarta.validation.constraints.NotEmpty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("spring.cors")
data class CorsProperties(
    @NotEmpty val allowedOrigins: List<String>
)
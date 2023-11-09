package ru.illine.drinking.ponies.config.property

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotEmpty

@Validated
@ConfigurationProperties("logbook.logger")
data class LogbookProperties(

    @NotEmpty val name: String
)

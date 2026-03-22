package ru.illine.drinking.ponies.config.property

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("buttons")
data class ButtonProperties(

    @NotNull
    var data: Data

) {
    data class Data(

        @NotEmpty
        val notificationInterval: String,

        @NotEmpty
        val quietModeTime: String,

        @NotEmpty
        val timezone: String
    )
}
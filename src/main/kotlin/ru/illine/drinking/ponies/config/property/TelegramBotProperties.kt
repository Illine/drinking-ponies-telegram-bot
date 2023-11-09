package ru.illine.drinking.ponies.config.property

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

@Validated
@ConfigurationProperties("telegram-bot")
data class TelegramBotProperties(

    @NotEmpty
    val token: String,

    @NotNull
    val http: Http

) {
    data class Http(

        @Min(30)
        @Max(120)
        val connectionTimeToLiveInSec: Long,

        @Min(10)
        @Max(200)
        val maxConnectionTotal: Int
    )
}

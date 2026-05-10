package ru.illine.drinking.ponies.config.property

import org.hibernate.validator.constraints.time.DurationMin
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

@Validated
@ConfigurationProperties("cache")
data class CacheProperties(
    val default: CacheEntry = CacheEntry(),
    val overrides: Map<String, CacheEntryOverride> = emptyMap()
) {
    data class CacheEntry(

        @NotNull
        @DurationMin(seconds = 10)
        var ttl: Duration = Duration.ofMinutes(5),

        @Min(1)
        @Max(1000)
        val maximumSize: Long = 100
    )

    data class CacheEntryOverride(

        @DurationMin(seconds = 10)
        val ttl: Duration? = null,

        @Min(1)
        @Max(1000)
        val maximumSize: Long? = null
    )
}

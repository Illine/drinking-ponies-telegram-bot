package ru.illine.drinking.ponies.model.dto.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.PastOrPresent
import ru.illine.drinking.ponies.util.water.WaterEntryConstants
import java.time.Instant

@JsonIgnoreProperties(ignoreUnknown = true)
data class WaterEntryRequest(
    @field:PastOrPresent
    val consumedAt: Instant,

    @field:Min(WaterEntryConstants.MIN_ML)
    @field:Max(WaterEntryConstants.MAX_ML)
    val amountMl: Int,
)

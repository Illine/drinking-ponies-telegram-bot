package ru.illine.drinking.ponies.model.dto.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.PastOrPresent
import ru.illine.drinking.ponies.util.water.WaterEntryConstants
import java.time.Instant

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Manual water intake entry payload")
data class WaterEntryRequest(
    @field:PastOrPresent
    @Schema(
        description = "Consumption time in ISO 8601 UTC format. " +
            "If null or omitted, server's current time is used.",
        example = "2025-01-01T14:00:00Z",
        nullable = true,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    )
    val consumedAt: Instant?,

    @field:Min(WaterEntryConstants.MIN_ML)
    @field:Max(WaterEntryConstants.MAX_ML)
    @Schema(
        description = "Water amount in milliliters",
        example = "250",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val amountMl: Int,
)

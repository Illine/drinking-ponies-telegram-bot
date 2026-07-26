package ru.illine.drinking.ponies.model.dto.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema
import ru.illine.drinking.ponies.model.base.NotificationHistoryStatus

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Notification journal entry update payload")
data class NotificationHistoryEntryRequest(
    @Schema(
        description = "New status of the entry",
        example = "CONFIRMED",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val status: NotificationHistoryStatus,
    // The range is checked by the service instead of bean validation: a missed entry ignores the amount,
    // and clients do send the whole form snapshot, zero included.
    @Schema(
        description = "Water amount in milliliters, from 50 to 1000. Required when CONFIRMED, ignored when MISSED.",
        example = "300",
        nullable = true,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    )
    val amountMl: Int?,
)

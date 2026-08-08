package ru.illine.drinking.ponies.model.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Error payload returned for a rejected request")
data class ErrorResponse(
    @Schema(description = "Human readable reason", example = "validation failed")
    val message: String,
)

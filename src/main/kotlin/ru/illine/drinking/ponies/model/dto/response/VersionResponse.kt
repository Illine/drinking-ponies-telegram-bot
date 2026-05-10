package ru.illine.drinking.ponies.model.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Backend version")
data class VersionResponse(
    @Schema(
        description = "Backend version from CI (GitVersion SemVer); 'local' when started outside CI",
        example = "8.0.0+1",
    )
    val version: String,
)

package ru.illine.drinking.ponies.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.illine.drinking.ponies.config.property.AppProperties
import ru.illine.drinking.ponies.model.dto.response.VersionResponse

@RestController
@RequestMapping("/systems")
@Tag(name = "System", description = "Backend runtime state and configuration")
class SystemController(
    private val appProperties: AppProperties,
) {

    @GetMapping("/version")
    @Operation(summary = "Get backend version")
    fun getVersion(): VersionResponse = VersionResponse(version = appProperties.version)
}

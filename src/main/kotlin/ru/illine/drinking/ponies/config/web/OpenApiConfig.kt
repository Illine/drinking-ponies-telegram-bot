package ru.illine.drinking.ponies.config.web

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig(
    private val buildProperties: BuildProperties
) {

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Drinking Ponies API")
                    .description("REST API for Drinking Ponies MiniApp")
                    .version(buildProperties.version)
            )
            .addSecurityItem(SecurityRequirement().addList("telegram-auth"))
            .components(
                Components()
                    .addSecuritySchemes(
                        "telegram-auth",
                        SecurityScheme()
                            .type(SecurityScheme.Type.APIKEY)
                            .`in`(SecurityScheme.In.HEADER)
                            .name("X-Authorization-Telegram-Data")
                    )
            )
    }
}
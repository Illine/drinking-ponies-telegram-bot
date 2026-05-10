package ru.illine.drinking.ponies.config.web

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.illine.drinking.ponies.config.property.AppProperties

@Configuration
class OpenApiConfig(
    private val appProperties: AppProperties
) {

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Drinking Ponies API")
                    .description("REST API for Drinking Ponies MiniApp")
                    .version(appProperties.version)
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

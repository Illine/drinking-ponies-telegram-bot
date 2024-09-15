package ru.illine.drinking.ponies.config.web

import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import ru.illine.drinking.ponies.config.property.CorsProperties
import ru.illine.drinking.ponies.config.web.interceptor.TelegramAuthInterceptor

@Configuration
class WebConfig(
    private val corsProperties: CorsProperties,
    private val telegramAuthInterceptor: TelegramAuthInterceptor
) : WebMvcConfigurer {

    private val defaultAllowedMapping = "/**"
    private val defaultAllowedHeaders = "*"

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(telegramAuthInterceptor)
            .addPathPatterns("/settings/modes/silent")
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        corsProperties.allowedOrigins.forEach {
            registry.addMapping(defaultAllowedMapping)
                .allowedOrigins(it)
                .allowedMethods(
                    HttpMethod.GET.name(),
                    HttpMethod.POST.name(),
                    HttpMethod.PUT.name(),
                    HttpMethod.PATCH.name(),
                    HttpMethod.DELETE.name()
                )
                .allowedHeaders(defaultAllowedHeaders)
                .allowCredentials(true)
        }
    }
}
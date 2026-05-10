package ru.illine.drinking.ponies.config.web

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import ru.illine.drinking.ponies.config.property.CorsProperties
import ru.illine.drinking.ponies.config.web.interceptor.AdminAuthInterceptor
import ru.illine.drinking.ponies.config.web.interceptor.TelegramAuthInterceptor

@Configuration
class WebConfig(
    private val corsProperties: CorsProperties,
    private val telegramAuthInterceptor: TelegramAuthInterceptor,
    private val adminAuthInterceptor: AdminAuthInterceptor,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(telegramAuthInterceptor)
            .addPathPatterns(
                "/settings",
                "/settings/**",
                "/notifications/**",
                "/users/**",
                "/systems/**",
                "/statistics/**"
            )

        registry.addInterceptor(adminAuthInterceptor)
            .addPathPatterns("/**")
    }

    @Bean
    fun corsFilter(): CorsFilter {
        val config = CorsConfiguration().apply {
            allowedOrigins = corsProperties.allowedOrigins
            allowedMethods = listOf(
                HttpMethod.GET.name(),
                HttpMethod.OPTIONS.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name()
            )
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return CorsFilter(source)
    }
}

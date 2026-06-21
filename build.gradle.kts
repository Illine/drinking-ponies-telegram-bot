import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask
import java.io.FileInputStream
import java.util.*

plugins {
    jacoco

    alias(libs.plugins.springframework.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.liquibase)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.detekt)
}

group = "ru.illine"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.cache)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.caffeine)

    implementation(libs.kotlin.reflect)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.validation.api)
    implementation(libs.logbook.spring.boot.starter)
    implementation(libs.logbook.okhttp)
    implementation(libs.telegrambots.client)
    implementation(libs.telegrambots.longpolling)
    implementation(libs.telegrambots.abilities)
    implementation(libs.datasource.decorator.spring.boot)
    implementation(libs.p6spy)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.hibernate.micrometer)
    implementation(libs.logstash)
    implementation(libs.commons.codec)
    implementation(libs.commons.lang3)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.konvert.api)

    liquibaseRuntime(libs.liquibase.core)
    liquibaseRuntime(libs.liquibase.groovy.dsl)
    liquibaseRuntime(libs.logback)
    liquibaseRuntime(libs.postgres)
    liquibaseRuntime(libs.snakeyaml)
    liquibaseRuntime(libs.picocli)

    runtimeOnly(libs.postgres)
    runtimeOnly(libs.micrometer.exposition.formats)

    kapt(libs.spring.boot.configuration.processor)
    ksp(libs.konvert)

    testImplementation(libs.spring.boot.starter.test) {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation(libs.liquibase.core)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.xmlunit)
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

ksp {
    // Konvert: make the build fail on incomplete or invalid mappings - the core goal of DPTB-136.
    arg("konvert.invalid-mapping-strategy", "fail")
    arg("konvert.non-constructor-properties-mapping", "all")
    arg("konvert.enforce-not-null", "true")
}

detekt {
    // ktlint owns formatting; detekt runs code-smell/complexity rules only (no formatting ruleset).
    buildUponDefaultConfig = true
    config.setFrom("$projectDir/config/detekt/detekt.yml")
    baseline = file("$projectDir/config/detekt/baseline.xml")
}

liquibase {
    val propertiesPath = System.getenv("LIQUIBASE_PROPERTIES_PATH") ?: "./.liquibase/liquibase.properties"
    val file = File(propertiesPath)
    val properties = Properties()
    if (file.exists()) {
        properties.load(FileInputStream(file))
    }

    val resourceDir = "./src/main/resources"
    activities.register("main") {
        this.arguments = mapOf(
            "changeLogFile" to properties.getOrDefault("changeLogFile", "$resourceDir/liquibase/changelog.yaml"),
            "url" to properties.getOrDefault("url", "jdbc:postgresql://localhost:5432/dptb"),
            "username" to properties.getOrDefault("username", "liquibase"),
            "password" to properties.getOrDefault("password", "liquibase"),
            "contexts" to properties.getOrDefault("context", "local"),
            "logLevel" to properties.getOrDefault("logLevel", "info")
        )
    }
    runList = "main"
}

tasks {
    bootJar {
        archiveFileName = "drinking-ponies.jar"
    }

    jar {
        enabled = false
    }

    compileKotlin {
        compilerOptions {
            freeCompilerArgs.add("-Xjsr305=strict")
            // KT-73255: opt in to the upcoming Kotlin default where a constructor-parameter
            // annotation without an explicit use-site target is applied to both the parameter
            // and the property/field. See https://youtrack.jetbrains.com/issue/KT-73255
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
        }
    }

    compileTestKotlin {
        compilerOptions {
            // Keep the annotation default target consistent with main (KT-73255).
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
        }
    }

    test {
        useJUnitPlatform {
            includeTags("unit", "spring-integration")
        }

        finalizedBy(jacocoTestReport)
    }

    jacocoTestReport {
        reports {
            html.required = false
            xml.required = true
            xml.outputLocation = layout.buildDirectory.file("jacoco/coverage.xml")
        }

        classDirectories.setFrom(
            files(classDirectories.files.map {
                fileTree(it) {
                    exclude(
                        "**/DrinkingPoniesApplicationKt*",
                        "**/DrinkingPoniesTelegramBot*",
                        "**/*\$DefaultImpls*",
                        // Kotlin inline functions: JaCoCo cannot track coverage in the original class
                        "**/FunctionHelper*",
                        // Spring @Configuration classes: beans are mocked or overridden in tests
                        "**/TelegramBotConfig*",
                        "**/TimeConfig*",
                        "**/OpenApiConfig*",
                        // JPA entities: boilerplate managed by Hibernate, not application logic
                        "**/*Entity*",
                        // Response: not application logic
                        "**/*Response*",
                        // Spring @ConfigurationProperties: no business logic, Kotlin data class boilerplate
                        "**/*Properties*",
                        // P6Spy logger: extends third-party Slf4JLogger, JaCoCo cannot correctly map coverage through parent bytecode
                        "**/CustomP6SpyLogger*",
                        // DTOs: Kotlin data class / Jackson DTO boilerplate (auto-generated setters/copy/component*), no business logic
                        "**/model/dto/**",
                        // Plain Kotlin enums: synthetic getEntries() generated at bytecode level, no application logic
                        "**/AuthErrorType*",
                        "**/IntervalNotificationType*"
                    )
                }
            })
        )
    }

    // ktlint (kotlinter) must not lint generated KSP/Konvert sources - KSP adds build/generated to the source set.
    withType<LintTask>().configureEach { exclude { it.file.path.contains("/build/generated/") } }
    withType<FormatTask>().configureEach { exclude { it.file.path.contains("/build/generated/") } }
}

import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask
import java.io.FileInputStream
import java.util.*

plugins {
    jacoco

    alias(libs.plugins.springframework.boot)
    alias(libs.plugins.spring.dependency.management)
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
    implementation(libs.spring.boot.starter.data.jpa) {
        // Unused: caching/@Transactional are proxy-based, no AspectJ weaving needed.
        exclude(group = "org.springframework", module = "spring-aspects")
    }
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.web) {
        // Unused: REST + long polling only, no websocket.
        exclude(group = "org.apache.tomcat.embed", module = "tomcat-embed-websocket")
    }
    implementation(libs.caffeine)

    implementation(libs.kotlin.reflect)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.validation.api)
    implementation(libs.logbook.spring.boot.starter)
    implementation(libs.logbook.okhttp)
    implementation(libs.telegrambots.client)
    implementation(libs.telegrambots.longpolling)
    implementation(libs.telegrambots.abilities) {
        // Unused: webhook server, long polling only.
        exclude(group = "org.telegram", module = "telegrambots-webhook")
        // MapDB replaced by InMemoryDBContext.
        exclude(group = "org.mapdb", module = "mapdb")
    }
    implementation(libs.datasource.decorator.spring.boot)
    implementation(libs.p6spy)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.hibernate.micrometer)
    implementation(libs.logstash)
    implementation(libs.commons.codec)
    implementation(libs.commons.lang3)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.konvert.api)


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
    testImplementation(libs.konsist)
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

// Liquibase runs through the official image, the same version the CI runner installs
// (.ansible/Dockerfile, ARG LIQUIBASE_VERSION). Extra command arguments - a rollback tag,
// for instance - are passed as -PliquibaseArgs="...".
val liquibaseSettings = Properties().apply {
    val file = File(System.getenv("LIQUIBASE_PROPERTIES_PATH") ?: "./.liquibase/liquibase.properties")
    if (file.exists()) {
        FileInputStream(file).use { load(it) }
    }
}

// Environment wins over the properties file, so credentials can stay out of it.
fun liquibaseSetting(key: String, fallback: String): String =
    System.getenv("LIQUIBASE_${key.uppercase()}") ?: liquibaseSettings.getProperty(key) ?: fallback

// A database on the host is localhost for us and host.docker.internal for the container.
// Docker Desktop provides that name itself; --add-host below adds it on Linux.
val liquibaseUrl = liquibaseSetting("url", "jdbc:postgresql://localhost:5432/dptb")
    .replace("localhost", "host.docker.internal")
    .replace("127.0.0.1", "host.docker.internal")

listOf(
    "update", "updateSql", "status", "validate", "history", "diff",
    "tag", "rollback", "rollbackCount", "rollbackSql", "dropAll", "listLocks", "releaseLocks"
).forEach { command ->
    tasks.register<Exec>(command) {
        group = "liquibase"
        description = "Runs liquibase $command against the configured database"

        commandLine(
            listOf(
                "docker", "run", "--rm",
                "--add-host=host.docker.internal:host-gateway",
                "-v", "${layout.projectDirectory.dir("src/main/resources").asFile}:/liquibase/changelog",
                "liquibase/liquibase:${libs.versions.liquibase.core.get()}",
                "--search-path=/liquibase/changelog",
                "--changelog-file=${liquibaseSetting("changeLogFile", "liquibase/changelog.yaml")}",
                "--url=$liquibaseUrl",
                "--username=${liquibaseSetting("username", "liquibase")}",
                "--password=${liquibaseSetting("password", "liquibase")}",
                "--contexts=${liquibaseSetting("context", "local")}",
                "--log-level=${liquibaseSetting("logLevel", "info")}",
                command
            ) + providers.gradleProperty("liquibaseArgs").getOrElse("").split(" ").filter { it.isNotBlank() }
        )
    }
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
            includeTags("unit", "spring-integration", "architecture")
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

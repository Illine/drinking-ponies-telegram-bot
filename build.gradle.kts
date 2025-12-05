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
}

group = "ru.illine"
version = "7.4.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

@Suppress("UnstableApiUsage")
configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.web)

    implementation(libs.jaxb.api)
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

    liquibaseRuntime(libs.liquibase.core)
    liquibaseRuntime(libs.liquibase.groovy.dsl)
    liquibaseRuntime(libs.logback)
    liquibaseRuntime(libs.postgres)
    liquibaseRuntime(libs.snakeyaml)
    liquibaseRuntime(libs.picocli)
    liquibaseRuntime(libs.jaxb.api)

    runtimeOnly(libs.postgres)
    runtimeOnly(libs.micrometer.exposition.formats)

    annotationProcessor(libs.spring.boot.configuration.processor)

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
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.MappedSuperclass")
    annotation("javax.persistence.Embeddable")
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
        kotlinOptions {
            freeCompilerArgs += "-Xjsr305=strict"
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
    }
}

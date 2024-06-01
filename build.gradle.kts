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
version = "0.5.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

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
    implementation(libs.logbook.httpclient)
    implementation(libs.telegrambots.abilities)
    implementation(libs.datasource.decorator.spring.boot)
    implementation(libs.p6spy)

    liquibaseRuntime(libs.liquibase.core)
    liquibaseRuntime(libs.liquibase.groovy.dsl)
    liquibaseRuntime(libs.logback)
    liquibaseRuntime(libs.postgres)
    liquibaseRuntime(libs.snakeyaml)
    liquibaseRuntime(libs.picocli)
    liquibaseRuntime(libs.jaxb.api)

    runtimeOnly(libs.postgres)

    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(libs.spring.boot.starter.test) {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation(libs.liquibase.core)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.datafaker)
    testImplementation(libs.mockito.kotlin)
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
            "url" to properties.getOrDefault("url", "jdbc:postgresql://localhost:5432/drinking_ponies"),
            "username" to properties.getOrDefault("username", "liquibase"),
            "password" to properties.getOrDefault("password", "liquibase"),
            "defaultSchemaName" to properties.getOrDefault("schema", "drinking_ponies"),
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

    test {
        useJUnitPlatform {
            includeTags("unit", "spring-integration")
        }

        finalizedBy(jacocoTestReport)
    }

    jacocoTestReport {
        reports {
            println(layout.buildDirectory.dir("/jacoco/coverage.xml"))
            html.required = false
            xml.required = true
            xml.outputLocation = layout.buildDirectory.file("jacoco/coverage.xml")
        }
    }

    compileKotlin {
        kotlinOptions {
            freeCompilerArgs += "-Xjsr305=strict"
            jvmTarget = "17"
        }
    }
}
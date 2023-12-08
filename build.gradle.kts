import java.io.FileInputStream
import java.util.*

plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.liquibase.gradle") version "2.2.0"

    kotlin("jvm") version "1.8.22"
    kotlin("plugin.spring") version "1.8.22"
    kotlin("plugin.jpa") version "1.8.22"
}

group = "ru.illine"

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
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("javax.xml.bind:jaxb-api:2.3.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("javax.validation:validation-api:2.0.1.Final")
    implementation("org.zalando:logbook-spring-boot-starter:3.6.0")
    implementation("org.zalando:logbook-httpclient:3.6.0")
    implementation("org.telegram:telegrambots-abilities:6.8.0")
    implementation("com.github.gavlyukovskiy:datasource-decorator-spring-boot-autoconfigure:1.9.1")
    implementation("p6spy:p6spy:3.9.1")

    liquibaseRuntime("org.liquibase:liquibase-core:4.20.0")
    liquibaseRuntime("org.liquibase:liquibase-groovy-dsl:3.0.2")
    liquibaseRuntime("ch.qos.logback:logback-classic:1.4.5")
    liquibaseRuntime("ch.qos.logback:logback-classic:1.4.5")
    liquibaseRuntime("org.postgresql:postgresql:42.5.4")
    liquibaseRuntime("org.yaml:snakeyaml:2.0")
    liquibaseRuntime("info.picocli:picocli:4.6.1")
    liquibaseRuntime("javax.xml.bind:jaxb-api:2.3.1")

    runtimeOnly("org.postgresql:postgresql")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
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
        archiveFileName = "drinking-ponies-telegram-bot.jar"
    }

    jar {
        enabled = false
    }

    test {
        useJUnitPlatform()
    }

    compileKotlin {
        kotlinOptions {
            freeCompilerArgs += "-Xjsr305=strict"
            jvmTarget = "17"
        }
    }
}
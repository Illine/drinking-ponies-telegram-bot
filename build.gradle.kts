import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask
import java.util.Properties

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
    // Konvert: make the build fail on incomplete or invalid mappings.
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
// Read through providers, not File/System.getenv directly: only then does the configuration
// cache notice that the settings changed and reconfigure instead of replaying stale values.
// Lazily: a build that never materializes a liquibase task must not read the settings file,
// otherwise its content and the LIQUIBASE_* variables become inputs of every other task.
val liquibaseSettings: Properties by lazy {
    providers
        .fileContents(
            layout.projectDirectory.file(
                providers.environmentVariable("LIQUIBASE_PROPERTIES_PATH").getOrElse(".liquibase/liquibase.properties")
            )
        ).asText
        .map { text -> Properties().apply { load(text.reader()) } }
        .getOrElse(Properties())
}

// Environment wins over the properties file, so credentials can stay out of it.
fun liquibaseSetting(key: String, fallback: String): String =
    providers.environmentVariable("LIQUIBASE_${key.uppercase()}").orNull
        ?: liquibaseSettings.getProperty(key)
        ?: fallback

// A database on the host is localhost for us and host.docker.internal for the container.
// Docker Desktop provides that name itself; --add-host below adds it on Linux.
// Only the host part is rewritten - a database or parameter named "localhost" stays intact.
val liquibaseUrl: String by lazy {
    liquibaseSetting("url", "jdbc:postgresql://localhost:5432/dptb")
        .replace(Regex("""(?<=//)(localhost|127\.0\.0\.1)(?=[:/])"""), "host.docker.internal")
}

// Quoted chunks survive as one argument: -PliquibaseArgs="--labels='a b'".
fun splitArguments(raw: String): List<String> {
    val arguments = mutableListOf<String>()
    val current = StringBuilder()
    var quote: Char? = null

    raw.forEach { char ->
        when {
            char == quote -> quote = null
            quote == null && (char == '"' || char == '\'') -> quote = char
            quote == null && char.isWhitespace() -> {
                if (current.isNotEmpty()) arguments += current.toString().also { current.clear() }
            }

            else -> current.append(char)
        }
    }
    if (current.isNotEmpty()) arguments += current.toString()

    return arguments
}

val executablePath: Provider<String> = providers.environmentVariable("PATH")

listOf(
    "update", "status", "validate", "tag", "rollback", "rollbackCount", "dropAll", "listLocks", "releaseLocks"
).forEach { command ->
    tasks.register<Exec>(command) {
        group = "liquibase"
        description = "Runs liquibase $command against the configured database"

        // The password travels as an environment variable of the container: a command-line argument
        // would be visible in `ps` and printed by --info.
        environment("LIQUIBASE_COMMAND_PASSWORD", liquibaseSetting("password", "liquibase"))

        // Without docker Exec fails with a bare "error=2, No such file or directory".
        val dockerInstalled =
            executablePath.getOrElse("").split(File.pathSeparator).any { File(it, "docker").canExecute() }

        doFirst {
            check(dockerInstalled) { "Liquibase tasks run through docker - install it or start Docker Desktop" }
        }

        commandLine(
            listOf(
                "docker", "run", "--rm",
                "--add-host=host.docker.internal:host-gateway",
                "-e", "LIQUIBASE_COMMAND_PASSWORD",
                "-v", "${layout.projectDirectory.dir("src/main/resources").asFile}:/liquibase/changelog:ro",
                "liquibase/liquibase:${libs.versions.liquibase.core.get()}",
                "--search-path=/liquibase/changelog",
                "--changelog-file=${liquibaseSetting("changeLogFile", "liquibase/changelog.yaml")}",
                "--url=$liquibaseUrl",
                "--username=${liquibaseSetting("username", "liquibase")}",
                "--contexts=${liquibaseSetting("context", "local")}",
                "--log-level=${liquibaseSetting("logLevel", "info")}",
                command
            ) + splitArguments(providers.gradleProperty("liquibaseArgs").getOrElse(""))
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

        // ToolingConsistencyTest compares pins that live outside the source set; without these
        // inputs a change to one of the files alone leaves the task UP-TO-DATE and the drift unseen.
        inputs
            .files(
                ".ansible/Dockerfile",
                ".gitlab-ci.yml",
                "gradle/wrapper/gradle-wrapper.properties",
                "gradle/libs.versions.toml",
            ).withPropertyName("toolingPins")

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

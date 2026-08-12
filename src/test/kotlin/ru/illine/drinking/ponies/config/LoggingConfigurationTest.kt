package ru.illine.drinking.ponies.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.logging.LogLevel
import org.springframework.boot.logging.LoggingSystem
import org.springframework.core.io.ClassPathResource
import org.w3c.dom.Element
import ru.illine.drinking.ponies.test.tag.SpringIntegrationTest
import javax.xml.parsers.DocumentBuilderFactory

@SpringIntegrationTest
@DisplayName("Logging Configuration Spring Integration Test")
class LoggingConfigurationTest
    @Autowired
    constructor(
        private val loggingSystem: LoggingSystem,
    ) {
        @Test
        @DisplayName("the root level from the configuration file is the one in effect, not logback's own default")
        fun `root level is applied`() {
            val declared = LogLevel.valueOf(readElements("root").single().getAttribute("level"))

            val applied = loggingSystem.getLoggerConfiguration(LoggingSystem.ROOT_LOGGER_NAME)?.configuredLevel

            assertEquals(declared, applied)
        }

        @Test
        @DisplayName("every logger the configuration file declares carries the declared level")
        fun `declared logger levels are applied`() {
            val declared =
                readElements("logger")
                    .associate { it.getAttribute("name") to LogLevel.valueOf(it.getAttribute("level")) }
            assertTrue(declared.isNotEmpty(), "the configuration file declares no logger, so this test guards nothing")

            val applied = declared.keys.associateWith { loggingSystem.getLoggerConfiguration(it)?.configuredLevel }

            assertEquals(declared, applied)
        }

        private fun readElements(tag: String): List<Element> =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(ClassPathResource(CONFIGURATION_FILE).inputStream)
                .getElementsByTagName(tag)
                .let { nodes -> (0 until nodes.length).map { nodes.item(it) as Element } }

        private companion object {
            const val CONFIGURATION_FILE = "logback-spring.xml"
        }
    }

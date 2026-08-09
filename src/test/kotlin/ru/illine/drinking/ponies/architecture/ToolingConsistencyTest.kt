package ru.illine.drinking.ponies.architecture

import com.lemonappdev.konsist.api.Konsist
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import ru.illine.drinking.ponies.test.tag.ArchitectureTest
import java.io.File

private val CATALOG_LIQUIBASE = Regex("""liquibase-core\s*=\s*"([^"]+)"""")

private val DOCKERFILE_LIQUIBASE = Regex("""ARG\s+LIQUIBASE_VERSION=(\S+)""")

private val WRAPPER_GRADLE = Regex("""gradle-(\d+\.\d+(?:\.\d+)?)-bin\.zip""")

private val CI_GRADLE_IMAGE = Regex("""image:\s*gradle:(\S+?)-jdk\d+""")

private val DECLARED_TAG = Regex("""@Tag\("([^"]+)"\)""")

private val INCLUDED_TAGS = Regex("""includeTags\(([^)]*)\)""")

@ArchitectureTest
@DisplayName("Tooling Consistency Architecture Test")
class ToolingConsistencyTest {
    private val root = File(Konsist.projectRootPath)

    private fun read(path: String): String = root.resolve(path).readText()

    // Two misses compare equal as null, which would silently switch the rule off.
    private fun pin(
        pattern: Regex,
        path: String,
    ): String = pattern.find(read(path))?.groupValues?.get(1) ?: fail("Found no $pattern in $path")

    @Test
    @DisplayName("the same Liquibase version validates and applies the changelog")
    fun `liquibase version is pinned once`() {
        val catalog = pin(CATALOG_LIQUIBASE, "gradle/libs.versions.toml")
        val runner = pin(DOCKERFILE_LIQUIBASE, ".ansible/Dockerfile")

        assertEquals(
            runner,
            catalog,
            "Tests run Liquibase $catalog, the CI runner applies migrations with $runner",
        )
    }

    @Test
    @DisplayName("the wrapper and the CI image agree on the Gradle version")
    fun `gradle version is pinned once`() {
        val wrapper = pin(WRAPPER_GRADLE, "gradle/wrapper/gradle-wrapper.properties")
        val images = CI_GRADLE_IMAGE.findAll(read(".gitlab-ci.yml")).map { it.groupValues[1] }.toSet()

        assertEquals(setOf(wrapper), images, "The wrapper builds with Gradle $wrapper, CI jobs use $images")
    }

    @Test
    @DisplayName("every declared test tag is executed by the test task")
    fun `test tags are executed`() {
        val declared =
            root
                .resolve("src/test/kotlin/ru/illine/drinking/ponies/test/tag")
                .listFiles()
                .orEmpty()
                .mapNotNull { DECLARED_TAG.find(it.readText())?.groupValues?.get(1) }
                .toSet()
        val included =
            pin(INCLUDED_TAGS, "build.gradle.kts")
                .split(",")
                .map { it.trim().trim('"') }
                .filter { it.isNotBlank() }
                .toSet()

        assertTrue(declared.isNotEmpty(), "No @Tag annotation found in test/tag")
        assertEquals(declared, included, "Tags $declared are declared, includeTags runs $included")
    }
}

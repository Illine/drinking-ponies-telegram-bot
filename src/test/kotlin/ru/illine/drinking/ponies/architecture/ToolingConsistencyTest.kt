package ru.illine.drinking.ponies.architecture

import com.lemonappdev.konsist.api.Konsist
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.illine.drinking.ponies.test.tag.ArchitectureTest
import java.io.File

private val CATALOG_LIQUIBASE = Regex("""liquibase-core\s*=\s*"([^"]+)"""")

private val DOCKERFILE_LIQUIBASE = Regex("""ARG\s+LIQUIBASE_VERSION=(\S+)""")

@ArchitectureTest
@DisplayName("Tooling Consistency Architecture Test")
class ToolingConsistencyTest {
    @Test
    @DisplayName("the same Liquibase version validates and applies the changelog")
    fun `liquibase version is pinned once`() {
        val root = File(Konsist.projectRootPath)

        val catalogVersion =
            CATALOG_LIQUIBASE
                .find(root.resolve("gradle/libs.versions.toml").readText())
                ?.groupValues
                ?.get(1)
        val runnerVersion =
            DOCKERFILE_LIQUIBASE
                .find(root.resolve(".ansible/Dockerfile").readText())
                ?.groupValues
                ?.get(1)

        assertNotNull(catalogVersion, "liquibase-core is missing from gradle/libs.versions.toml")
        assertNotNull(runnerVersion, "ARG LIQUIBASE_VERSION is missing from .ansible/Dockerfile")
        assertEquals(
            runnerVersion,
            catalogVersion,
            "Tests run Liquibase $catalogVersion, the CI runner applies migrations with $runnerVersion",
        )
    }
}

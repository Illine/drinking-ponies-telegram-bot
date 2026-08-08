package ru.illine.drinking.ponies.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import com.lemonappdev.konsist.core.exception.KoAssertionFailedException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import ru.illine.drinking.ponies.test.tag.ArchitectureTest

private const val DTO_PATH = "/model/dto/"

private val DTO_PACKAGES = setOf("request", "response", "internal")

private val SERIALIZATION_PACKAGES = listOf("com.fasterxml.jackson", "io.swagger")

private val TEST_TAGS = listOf("UnitTest", "SpringIntegrationTest", "ArchitectureTest")

@ArchitectureTest
@DisplayName("Code Layout Architecture Test")
class CodeLayoutTest {
    @Test
    @DisplayName("model/dto holds exactly three packages and an empty root")
    fun `dto packages`() {
        val buckets =
            Konsist
                .scopeFromProduction()
                .files
                .filter { it.path.contains(DTO_PATH) }
                .map { it.path.substringAfter(DTO_PATH).substringBefore("/") }
                .toSet()

        assertEquals(DTO_PACKAGES, buckets, "model/dto must hold exactly $DTO_PACKAGES and no file in the root")
    }

    @Test
    @DisplayName("internal DTOs carry no serialization imports")
    fun `internal dtos are not serialized`() {
        Konsist
            .scopeFromProduction()
            .files
            .filter { it.path.contains(DTO_PATH + "internal/") }
            .assertFalse { file ->
                file.imports.any { import -> SERIALIZATION_PACKAGES.any { import.name.startsWith(it) } }
            }
    }

    @ParameterizedTest
    @ValueSource(strings = ["..model.dto.response..", "..model.dto.request.."])
    @DisplayName("every wire DTO is documented with @Schema")
    fun `wire dtos are documented`(wirePackage: String) {
        Konsist
            .scopeFromProduction()
            .classes()
            .withPackage(wirePackage)
            .assertTrue { it.hasAnnotationWithName("Schema") }
    }

    @ParameterizedTest
    @CsvSource("Response, ..model.dto.response..", "Request, ..model.dto.request..")
    @DisplayName("a wire-format suffix is reserved for its own package")
    fun `wire suffixes are reserved`(
        suffix: String,
        expectedPackage: String,
    ) {
        Konsist
            .scopeFromProduction()
            .classesAndInterfacesAndObjects(includeNested = false)
            .filter { it.hasNameEndingWith(suffix) }
            .assertTrue { it.resideInPackage(expectedPackage) }
    }

    @Test
    @DisplayName("no test lives in an impl package")
    fun `tests avoid impl packages`() {
        Konsist
            .scopeFromTest()
            .classes(includeNested = false)
            .assertFalse { it.resideInPackage("..impl..") }
    }

    @Test
    @DisplayName("every test class carries exactly one tag, directly or through its parent")
    fun `test classes are tagged`() {
        Konsist
            .scopeFromTest()
            .classes(includeNested = false)
            .filter { it.hasNameEndingWith("Test") && it.resideOutsidePackage("..test.tag..") }
            .assertTrue {
                it.countAnnotations { annotation -> annotation.name in TEST_TAGS } == 1 || it.hasParents()
            }
    }

    @Test
    @DisplayName("guard: a rule broken on purpose still fails")
    fun `rules can fail`() {
        assertThrows<KoAssertionFailedException> {
            Konsist
                .scopeFromProduction()
                .classes()
                .withPackage("..model.dto.internal..")
                .assertTrue { it.hasAnnotationWithName("Schema") }
        }
    }
}

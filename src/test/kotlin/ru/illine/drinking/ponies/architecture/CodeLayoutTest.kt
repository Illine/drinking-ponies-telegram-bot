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

private val SUPPRESS_CALL = Regex("""@(?:file:)?Suppress\(([^)]*)\)""")

private val STRING_LITERAL = Regex(""""([^"]*)"""")

// detekt accepts its own prefix in any case and honours the ruleset id, so the forms that switch a
// boundary off are matched by shape instead of being listed one by one.
private val BOUNDARY_SUPPRESSION =
    Regex("""^(detekt[.:]|style[.:])?(all|style|ForbiddenImport(/.*)?)$""", RegexOption.IGNORE_CASE)

@ArchitectureTest
@DisplayName("Code Layout Architecture Test")
class CodeLayoutTest {
    @Test
    @DisplayName("model/dto holds exactly three packages, an empty root and no nesting")
    fun `dto packages`() {
        val relativePaths =
            Konsist
                .scopeFromProduction()
                .files
                .filter { it.path.contains(DTO_PATH) }
                .map { it.path.substringAfter(DTO_PATH) }

        assertEquals(
            DTO_PACKAGES,
            relativePaths.map { it.substringBefore("/") }.toSet(),
            "model/dto must hold exactly $DTO_PACKAGES and no file in the root",
        )
        assertEquals(
            emptyList<String>(),
            relativePaths.filter { it.count { char -> char == '/' } != 1 },
            "model/dto allows no nesting: a file sits directly in one of $DTO_PACKAGES",
        )
    }

    @Test
    @DisplayName("internal DTOs carry no serialization imports")
    fun `internal dtos are not serialized`() {
        Konsist
            .scopeFromProduction()
            .files
            .filter { it.path.contains(DTO_PATH + "internal/") }
            .assertFalse(strict = true) { file ->
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
            .assertTrue(strict = true) { it.hasAnnotationWithName("Schema") }
    }

    @ParameterizedTest
    @CsvSource(
        "Response, ..model.dto.response..",
        "Request, ..model.dto.request..",
        "Constants, ..util..",
    )
    @DisplayName("a name suffix is reserved for its own package")
    fun `name suffixes are reserved`(
        suffix: String,
        expectedPackage: String,
    ) {
        Konsist
            .scopeFromProduction()
            .classesAndInterfacesAndObjects(includeNested = false)
            .filter { it.hasNameEndingWith(suffix) }
            .assertTrue(strict = true) { it.resideInPackage(expectedPackage) }
    }

    @Test
    @DisplayName("internal carriers end with Dto or Context")
    fun `internal carriers are named consistently`() {
        Konsist
            .scopeFromProduction()
            .classesAndInterfacesAndObjects(includeNested = false)
            .withPackage("..model.dto.internal..")
            .assertTrue(strict = true) { it.hasNameEndingWith("Dto") || it.hasNameEndingWith("Context") }
    }

    @ParameterizedTest
    @CsvSource(
        "Konverter, ..mapper..",
        "Entity, ..model.entity..",
        "RestController, ..controller..",
    )
    @DisplayName("a marker annotation keeps its declarations in one package")
    fun `annotated declarations stay in their package`(
        annotation: String,
        expectedPackage: String,
    ) {
        Konsist
            .scopeFromProduction()
            .classesAndInterfacesAndObjects(includeNested = false)
            .filter { it.hasAnnotationWithName(annotation) }
            .assertTrue(strict = true) { it.resideInPackage(expectedPackage) }
    }

    @Test
    @DisplayName("no test lives in an impl package")
    fun `tests avoid impl packages`() {
        Konsist
            .scopeFromTest()
            .classesAndInterfacesAndObjects(includeNested = false)
            .assertFalse(strict = true) { it.resideInPackage("..impl..") }
    }

    @Test
    @DisplayName("every test class carries exactly one tag, directly or through a tagged parent")
    fun `test classes are tagged`() {
        // An untagged test is silently skipped by includeTags, so the rule looks at objects too - Konsist keeps
        // them out of classes(), and a tag missing there would never be reported.
        val declarations = Konsist.scopeFromTest().classesAndInterfacesAndObjects(includeNested = false)

        // An empty list of tagged parents needs no guard of its own: it leaves every subject with zero tags,
        // which the assertion below reports.
        val taggedParents =
            declarations
                .filter { it.countAnnotations { annotation -> annotation.name in TEST_TAGS } == 1 }
                .map { it.name }

        declarations
            .filter { subject ->
                val holdsTests =
                    subject.hasNameEndingWith("Test") ||
                        subject.functions().any { it.hasAnnotationWithName("Test", "ParameterizedTest") }
                holdsTests && subject.resideOutsidePackage("..test.tag..")
            }.assertTrue(strict = true) { subject ->
                val ownTags = subject.countAnnotations { annotation -> annotation.name in TEST_TAGS }
                // A parent name arrives with its type arguments: EnumTypeOfTest<WaterAmountType>.
                val inheritsTag = subject.parents().any { it.name.substringBefore("<") in taggedParents }
                ownTags == 1 || (ownTags == 0 && inheritsTag)
            }
    }

    @Test
    @DisplayName("no @Suppress switches off a layer boundary or a whole ruleset")
    fun `boundaries are not suppressed`() {
        val suppressed =
            (Konsist.scopeFromProduction().files + Konsist.scopeFromTest().files)
                .flatMap { file ->
                    SUPPRESS_CALL
                        .findAll(file.text)
                        .flatMap { call -> STRING_LITERAL.findAll(call.groupValues[1]) }
                        .map { it.groupValues[1] }
                        .filter { BOUNDARY_SUPPRESSION.matches(it) }
                        .map { "${file.name}: $it" }
                        .toList()
                }

        assertEquals(
            emptyList<String>(),
            suppressed,
            "A boundary is fixed in the code, not silenced with @Suppress",
        )
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

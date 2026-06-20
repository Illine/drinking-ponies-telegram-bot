# Testing guide

Backend tests for the Drinking Ponies bot. This document describes the single,
shared style every test under `src/test/kotlin/**` follows so the suite stays
readable and consistent. The mocking idioms are the canonical reference for
[mockito-kotlin](https://github.com/mockito/mockito-kotlin); do not mix in raw
`org.mockito.Mockito` / `org.mockito.ArgumentMatchers` calls.

## Stack

- **JUnit 5** (Jupiter) - test framework. `@Test`, `@ParameterizedTest`,
  `@DisplayName`, `@Nested`.
- **mockito-kotlin** - the only mocking API used in unit tests (`mock<T>()`,
  `whenever`, `verify`, `argumentCaptor<T>()`, ...). Never the raw Mockito API.
- **Spring Boot Test** - `@SpringBootTest` with `RANDOM_PORT` and
  `TestRestTemplate` for integration tests.
- **Testcontainers (PostgreSQL)** - real database for integration tests, wired
  through `TestDatabaseConfig`.
- **JaCoCo** - coverage, emitted to `build/jacoco/coverage.xml`.

## Test tags

Every test class carries exactly one tag, applied through a meta-annotation:

- `@UnitTest` -> `@Tag("unit")` - pure unit tests, collaborators mocked with
  mockito-kotlin, no Spring context.
- `@SpringIntegrationTest` -> `@Tag("spring-integration")` - full
  `@SpringBootTest` with the real context, Testcontainers DB, and beans replaced
  via `@MockitoBean`. Bundles `TestDatabaseConfig` + `TestTimeConfig` and the
  `integration-test` profile.

Both tags run together:

```bash
./gradlew test                                              # whole suite
./gradlew test --tests "ru.illine.drinking.ponies.service.*" # one package
```

## File layout

- **Co-located by package**: a test mirrors the package of its subject and is
  named `<Subject>Test.kt` (e.g. `service/notification/NotificationServiceTest`).
- Test the public surface, not the `Impl`: the file is named after the
  interface/subject and lives in the interface package, even when it exercises
  the `*Impl`.
- One class per subject, `@DisplayName` on the class. Use `@Nested` inner
  classes to group cases by endpoint/method (see the controller tests).
- Follow **Arrange / Act / Assert** - three visually separated blocks. Short
  tests may collapse them, but keep the order.
- Use `@ParameterizedTest` (`@EnumSource` / `@ValueSource` / `@MethodSource`)
  for table-driven cases instead of copy-pasting near-identical `@Test`s.

## Naming

- Class: `@DisplayName("NotificationService Unit Test")`.
- Method: a backtick function name describing the behavior, plus a `@DisplayName`
  spelling out the contract:

```kotlin
@Test
@DisplayName("reply(): records water statistic with CANCEL event type")
fun `reply records statistic`() { ... }
```

## Mocking (mockito-kotlin)

One import style per file: everything comes from `org.mockito.kotlin.*`. A file
must never mix `org.mockito.Mockito.*` / `org.mockito.ArgumentMatchers.*` with
mockito-kotlin.

- **Create**: `mock<TelegramClient>()` (reified) - never `mock(T::class.java)`.
- **Stub**: `whenever(mock.call()).thenReturn(x)` - never the backtick `` `when` ``.
  For void/throwing stubs use the prefix form: `doThrow(ex).whenever(mock).call()`,
  `doReturn(x).whenever(mock).prop`.
- **Verify**: `verify(mock).call(...)`, `verify(mock, never())`,
  `verify(mock, times(2))`, `verifyNoInteractions(mock)`,
  `verifyNoMoreInteractions(mock)`. For ordering use
  `inOrder(a, b).verify(a)...`.
- **Capture**: `argumentCaptor<SendMessage>()`, read via `captor.firstValue`
  (or `lastValue` / `allValues`) - not `.value`. For a nullable argument use
  `nullableArgumentCaptor<Instant>()`.
- **Matchers**: `any()` / `any<T>()` for non-null arguments, `anyOrNull()` for
  nullable arguments, `eq(value)` when mixing a literal with other matchers.
  Pick `any<T>()` vs `anyOrNull()` by the parameter's nullability - the wrong
  one yields an NPE or a stub that never matches.

## Integration tests

- Annotate the class with `@SpringIntegrationTest` and inject `TestRestTemplate`
  (plus `ObjectMapper`, `CacheManager`, etc.) via `@Autowired constructor`.
- Replace service/collaborator beans with `@MockitoBean` (Spring-native bean
  override). This is the **only** sanctioned non-mockito-kotlin construct -
  there is no mockito-kotlin equivalent, and it is not a style violation.
- Seed and clean the database with `@Sql` (BEFORE/AFTER each method, isolated
  transaction mode), pointing at `classpath:sql/...` scripts.
- Time is deterministic: a fixed `Clock` comes from `TestTimeConfig`; in unit
  tests build one explicitly (`Clock.fixed(...)`). Assert against UTC.
- Assert on the observable contract: HTTP status, response body, error headers
  (`X-Auth-Error-Code`), and the captured arguments forwarded to mocked beans.

## Fixtures

- Build DTOs through `DtoGenerator` (e.g. `generateNotificationDto(...)`,
  `generateWaterStatisticDto(...)`). Add new factory methods there with sensible
  defaults rather than hand-constructing DTOs in each test.

## What we do NOT test

We do not test third-party library behavior (the `no-tests-for-third-party-libs`
rule). A test that only asserts what Spring, Hibernate, Jackson, Liquibase, or
mockito already guarantee gives false confidence. Test **our** logic: branch
selection, computed values, delegated calls, dispatched side effects, and the
HTTP contract.

## Intentional exceptions

These deviate from the rules above on purpose - do not "fix" them:

- **`@MockitoBean`** in integration tests - Spring bean override, kept as-is.
- **`eq()` + `capture()`** and **`any<T>()` for non-null** - inherent
  mockito-kotlin friction (Kotlin null-safety vs Mockito matchers); tolerated,
  not worth a mock-engine migration.
- **`CustomP6SpyLoggerTest`** uses the raw Mockito static-mocking API
  (`mockStatic` + `MockedStatic.`when``): mockito-kotlin 5.2.1 ships no
  `mockStatic` wrapper, so the Java API is the only option here.

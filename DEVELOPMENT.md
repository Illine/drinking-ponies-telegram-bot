# Development

## Code quality

The backend uses two complementary Kotlin tools:

- **ktlint** (via the [kotlinter](https://github.com/jeremymailen/kotlinter-gradle) Gradle plugin) - **formatting**: indentation, import ordering, wrapping, trailing commas. Auto-fixable.
- **detekt** - **static analysis**: code smells, complexity, potential bugs, naming. Not auto-fixable; it points, you fix.

Formatting is owned exclusively by ktlint; detekt runs with its formatting ruleset disabled so the two never fight.

### Single source of style

[`.editorconfig`](.editorconfig) is the single source of truth for code style. It is read by ktlint, detekt **and** IntelliJ IDEA, so the editor, local checks and CI all agree.

### Commands

```bash
./gradlew formatKotlin        # auto-fix formatting (run this before committing)
./gradlew lintKotlin detekt   # formatting + static analysis
./gradlew check -x test       # exactly what the CI lint job runs
./gradlew check               # full local verify: lint + detekt + tests
```

Coverage is measured but not gated: `test` is finalized by `jacocoTestReport`,
the report goes to Codecov, and no threshold can fail the build.

CI runs `gradle check -x test` as the `lint` job, which the `test` job depends on (`needs`), so formatting/smell regressions fail fast before tests. The `lint` job deliberately runs `check` rather than a list of task names: a verification task that its plugin wires into `check` reaches CI on its own. Tasks that merely sit in the `verification` group without being attached to `check` - `detektMain`/`detektTest`, for instance - still need to be named explicitly.

### Configuration cache

[`gradle.properties`](gradle.properties) turns the configuration cache on for every build. Locally that pays off on every repeated command. In CI only the `lint` job benefits: an entry is keyed by the set of requested tasks, and `lint` is the one job that pushes the cache, so its entry survives to the next pipeline. `test` and `build-jar` request different tasks and run with `policy: pull` - whatever they write is thrown away with the container, so they pass `--no-configuration-cache` and skip writing it at all. Build logic must therefore read the environment and files through `providers` - a plain `System.getenv` or `File(...)` is invisible to the cache, and the build silently replays stale values. When a plugin upgrade breaks the cache the build fails rather than degrades; `--no-configuration-cache` unblocks a single invocation.

The same file raises the daemon's memory (`org.gradle.jvmargs`, `kotlin.daemon.jvmargs`): kapt, KSP and the detekt alpha do not fit the Gradle defaults, and a daemon that runs out of metaspace restarts mid-build, throwing away both the warm state and the configuration cache. The values are chosen to stay well inside the CI runner, since `gradle.properties` applies there too.

### detekt baseline

[`config/detekt/baseline.xml`](config/detekt/baseline.xml) freezes the pre-existing findings on legacy code so CI does not fail on them; new code is checked cleanly. The frozen findings are listed in that file, the active rules in [`config/detekt/detekt.yml`](config/detekt/detekt.yml). After clearing findings on purpose, regenerate the baseline with `./gradlew detektBaseline`. `MaxLineLength` overlaps with ktlint's line-length rule and is a planned follow-up to disable in detekt (ktlint owns line length).

### IntelliJ IDEA setup

`.editorconfig` is the only setup the editor needs. Make sure `Settings → Editor → Code Style → Enable EditorConfig support` is on (it is by default) - a *"Settings may be overridden by EditorConfig"* banner on the Code Style page confirms IDEA is reading it.

With that, the native `Reformat Code` (Cmd/Ctrl+Alt+L) picks up `ktlint_official` and the `ij_kotlin_*` rules from `.editorconfig`, so it is already close to ktlint and fine for quick touch-ups. It does **not** match ktlint exactly (trailing commas and some wrapping rules diverge), so the source of truth stays `./gradlew formatKotlin` before pushing - CI runs the same check and fails on any mismatch.

**Optional plugins** (inline feedback, not required):

- **[Ktlint plugin](https://plugins.jetbrains.com/plugin/15057-ktlint)** (by Nikolay Badal) - formats exactly like CI. Caveat: its format-on-save reacts to *explicit* saves only; IDEA's autosave (timer / focus loss) does not trigger it, so on an autosave workflow run `formatKotlin` manually or bind the plugin's format action to a shortcut. Its mode is stored per project - enable it here, leave it `Disabled` elsewhere; global IDE settings are untouched.
- **detekt plugin** - surfaces static-analysis findings inline; point it at `config/detekt/detekt.yml` and `config/detekt/baseline.xml`.

### Caveats

- **Kotlin 2.3.21.** detekt 2.0 (alpha) is compiled against an exact Kotlin compiler version and refuses to run on a mismatch. The project is pinned to the Kotlin version detekt's alpha targets. A Kotlin bump may require a matching detekt alpha (and vice versa) until detekt 2.0 reaches stable.
- **No type resolution (yet).** CI runs the flat `detekt` task, which analyses sources without type resolution. The type-aware variants (`detektMain`/`detektTest`) are intentionally left out for now - they are the most fragile path on the detekt alpha. Rules that require type resolution therefore do not run yet; enabling them is a follow-up once detekt 2.0 stabilises.
- Generated KSP/Konvert sources under `build/generated/**` are excluded from ktlint and are not picked up by detekt.

## Database migrations

One Liquibase, one version, everywhere. The CI `migration` stage runs the native CLI from the ansible image (`.ansible/Dockerfile`, `ARG LIQUIBASE_VERSION`); locally the Gradle tasks below run the official image at the version pinned in [`libs.versions.toml`](gradle/libs.versions.toml), and the integration tests apply the same changelog through Spring Boot with the same `liquibase-core`. The two pins must match - a changelog validated by one version and applied by another is exactly the drift this project avoids - and `architecture/ToolingConsistencyTest` fails the build when they do not.

```bash
./gradlew status                            # pending changesets
./gradlew update                            # apply them
./gradlew rollback -PliquibaseArgs="8.8.0"  # arguments for a command
```

The Liquibase commands used day to day are Gradle tasks of the same name, grouped under `liquibase` in `./gradlew tasks`. The list in [`build.gradle.kts`](build.gradle.kts) is not the full Liquibase surface - add the command there when a new one is needed. Arguments passed through `-PliquibaseArgs` are split on spaces; quote a value that contains one (`-PliquibaseArgs="--label-filter='a b'"`).

Connection settings come from `.liquibase/liquibase.properties`, whose path itself is overridable by `LIQUIBASE_PROPERTIES_PATH`. Every key in that file is overridable by the matching `LIQUIBASE_<KEY>` environment variable (`LIQUIBASE_URL`, `LIQUIBASE_USERNAME`, `LIQUIBASE_PASSWORD`, `LIQUIBASE_CONTEXT`, ...) - point them at another host and the changelog is applied there, exactly as the pipeline does it. The password reaches the container as an environment variable, so it stays out of `ps` and of the build log. Docker is required; `localhost` in the URL is rewritten to `host.docker.internal` so the container reaches a database on the host (Docker Desktop resolves that name, `--add-host` adds it on Linux).

`changeLogFile` is relative to `src/main/resources`, which is what the pipeline uses as well. A database created before that, by the old Gradle plugin, holds the repository-root path in `DATABASECHANGELOG.FILENAME` and will read every changeset as new - recreate it, or realign it once:

```sql
UPDATE databasechangelog SET filename = replace(filename, 'src/main/resources/', '');
```

There is no Liquibase Gradle plugin: it needed Liquibase on the buildscript classpath, blocked the configuration cache, and gave us a second Liquibase version that the pipeline never used.

## Code layout

### Packages

| Package | What lives there |
|---|---|
| `bot` | Telegram bot entry point |
| `config` | Spring configuration: caching, properties, web (interceptors, security, exception handler) |
| `controller` | REST controllers - the only place that knows about the HTTP wire format |
| `dao/repository` | Spring Data repositories, plus `projection/` (interface projections of native queries) and `query/` (reusable native SQL fragments) |
| `dao/access` | Persistence-facing services over repositories: mechanics, no business rules |
| `service` | Business logic, one sub-package per feature (`notification`, `statistic`, `user`, ...) |
| `mapper` | Konvert mappers, the only place where one model shape is turned into another |
| `model/entity` | JPA entities |
| `model/base` | Enums and other shared value types |
| `model/dto` | Data carriers, see below |
| `scheduler` | Scheduled jobs |
| `util` | Helpers, constant holders and foreign wire schemas, grouped by domain (`util/telegram`, `util/water`, ...) |
| `exception` | Application exceptions |

### Layer boundaries

Every shape stops at its layer:

- `dao/repository` types (entities, projections) do not leave `dao` - the access layer hands out internal DTOs instead.
- Services take and return DTOs. A service never returns an HTTP response type.
- The response is assembled in the controller: a Konvert mapper when the shape is non-trivial, the constructor when it is two or three fields copied as is.
- A request DTO is unpacked in the controller as well - services take named parameters or an internal DTO, never the request type itself.

### DTO packages

Exactly three packages under `model/dto`, no nesting, nothing in the root:

| Package | Contents | Marker |
|---|---|---|
| `request` | What the API accepts | `@Schema` + Jackson annotations |
| `response` | What the API returns | `@Schema` |
| `internal` | Everything else: dao ↔ service ↔ controller | no serialization annotations at all |

The question to ask is binary: **is the type part of our HTTP contract?** Yes - `request`/`response`. No - `internal`. There is no other place to put it.

A schema owned by an external system is not our contract and stays out of `model/dto` entirely: it lives next to the code that parses it (`util/telegram/TelegramInitDataUser` beside `TelegramWebAppDataHelper`), keeps its serialization annotations there, and is converted into an `internal` DTO right away. Nothing beyond the parser sees the foreign shape.

Naming: what an endpoint returns carries the `*Response` suffix and what it accepts carries `*Request`; both suffixes belong to their package and are used nowhere else. A shape nested inside a response has no suffix (`WaterEntry`, `ShortUserInfo`, `UserCounts`). Internal carriers end with `*Dto`, except message contexts, which end with `*Context`.

### Constants

Hardcoded values do not live in services or controllers. Domain constants go to `util/<domain>/<Feature>Constants.kt`; a value that only makes sense inside a single annotation (`@Max(100)`) stays inline.

### Enforcement

The rules above are not left to review attention - most of them fail the build.

| Rule | Enforced by |
|---|---|
| Repositories and projections stay inside `dao` | detekt `ForbiddenImport/repositoryOutsideDao` |
| Entities stay inside `dao` and `mapper` | detekt `ForbiddenImport/entityOutsideDao` |
| HTTP types (`request` and `response`) stay in the web layer | detekt `ForbiddenImport/httpTypesOutsideWeb` |
| A foreign wire schema stays at its parser | detekt `ForbiddenImport/foreignSchemaOutsideParser` |
| Tests mock through mockito-kotlin, not the raw Mockito API | detekt `ForbiddenImport/rawMockitoInTests` |
| No `@Suppress` switches off a boundary or a whole ruleset | `architecture/CodeLayoutTest` |
| Three packages under `model/dto`, empty root | `architecture/CodeLayoutTest` |
| No serialization annotations in `internal` | `architecture/CodeLayoutTest` |
| `@Schema` on every request and response DTO | `architecture/CodeLayoutTest` |
| `*Response`, `*Request` and `*Constants` suffixes reserved for their own packages | `architecture/CodeLayoutTest` |
| Internal carriers end with `*Dto` or `*Context` | `architecture/CodeLayoutTest` |
| `@Konverter`, `@Entity` and `@RestController` each stay in one package | `architecture/CodeLayoutTest` |
| Tests stay out of `impl` packages, each carries one tag | `architecture/CodeLayoutTest` |
| Account state flags are written in one place only | `architecture/CodeLayoutTest` |
| One Liquibase version for tests and for the CI runner | `architecture/ToolingConsistencyTest` |
| One Gradle version for the wrapper and the CI images | `architecture/ToolingConsistencyTest` |
| Every declared test tag is executed by the `test` task | `architecture/ToolingConsistencyTest` |
| Every CI job running gradle is pinned to the shared image | `architecture/ToolingConsistencyTest` |
| `package` matches the directory | detekt `InvalidPackageDeclaration` |
| Naming, formatting, import order | ktlint |

`deleted`, `is_banned` and `is_admin` are written through `TelegramUserAccessServiceImpl` and nowhere else: every write there
records an audit event in `user_state_events` and drops the cached access flags. A second writer would produce a state
change that the history never saw, so the rule matches both spellings of an assignment and any update statement over
the users table.

Silencing a boundary is caught by the architecture test rather than by detekt's
own `ForbiddenSuppress`: detekt matches suppression ids as literals while
accepting its prefix in any case, so `@Suppress("Detekt.all")` slips through any
list one could write. The test matches the shape instead - the blanket
`all`/`style` forms and every `ForbiddenImport` id, with or without a
`detekt.`/`style.` prefix - and a new rule id needs nothing added.

The reverse of the mapper rule is deliberately not enforced: `mapper` may hold a
hand-written mapper when the conversion carries logic (`SettingMapper` formats
times and reaches into a nested property). What must not happen is a `@Konverter`
appearing outside the package - that is what the rule checks.

Some packages cross a boundary by design, and each rule carries its own exclusion
list: `mapper` is excluded everywhere (turning one shape into another is what it
exists for), `dao` and `model/entity` from the dao-facing rules, `controller` and
`config/web` (interceptors and the exception handler are part of the web layer
despite the package they live in) plus the test fixture factory `test/generator`
from the HTTP-type rule, `util/telegram` and `service/telegram` from the foreign
schema rule (the schema is parsed there), and `util/sql` from the mockito rule
(`CustomP6SpyLoggerTest` needs `mockStatic`, which mockito-kotlin does not wrap).
The lists live in [`config/detekt/detekt.yml`](config/detekt/detekt.yml) next to
each rule.

What stays on review: whether a mapper or a constructor fits a given shape, and
whether a comment earns its place. Neither is expressible as a rule.

Adding a convention means adding its check - see
[`TESTING.md`](TESTING.md#architecture-tests) for how, including the negative
check every new rule ships with.

## Tests

Test layout, tags and coverage live in [`TESTING.md`](TESTING.md).

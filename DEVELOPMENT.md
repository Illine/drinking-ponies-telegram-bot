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
./gradlew lintKotlin detekt   # the exact checks CI runs (formatting + static analysis)
./gradlew check               # full local verify: lint + detekt + tests + coverage
```

CI runs `gradle lintKotlin detekt` as the `lint` job, which the `test` job depends on (`needs`), so formatting/smell regressions fail fast before tests.

### detekt baseline

[`config/detekt/baseline.xml`](config/detekt/baseline.xml) freezes the pre-existing findings on legacy code so CI does not fail on them. New code is checked cleanly. To review what is currently baselined, see [`config/detekt/detekt.yml`](config/detekt/detekt.yml) for the active rules and regenerate the baseline with `./gradlew detektBaseline` after intentionally clearing findings.

### IntelliJ IDEA setup

`.editorconfig` is the only setup the editor needs. Make sure `Settings → Editor → Code Style → Enable EditorConfig support` is on (it is by default) - a *"Settings may be overridden by EditorConfig"* banner on the Code Style page confirms IDEA is reading it.

With that, the native `Reformat Code` (Cmd/Ctrl+Alt+L) picks up `ktlint_official` and the `ij_kotlin_*` rules from `.editorconfig`, so it is already close to ktlint and fine for quick touch-ups. It does **not** match ktlint exactly (trailing commas and some wrapping rules diverge), so the source of truth stays `./gradlew formatKotlin` before pushing - CI runs the same check and fails on any mismatch.

**Optional plugins** (inline feedback, not required):

- **[Ktlint plugin](https://plugins.jetbrains.com/plugin/15057-ktlint)** (by Nikolay Badal) - formats exactly like CI. Caveat: its format-on-save reacts to *explicit* saves only; IDEA's autosave (timer / focus loss) does not trigger it, so on an autosave workflow run `formatKotlin` manually or bind the plugin's format action to a shortcut. Its mode is stored per project - enable it here, leave it `Disabled` elsewhere; global IDE settings are untouched.
- **detekt plugin** - surfaces static-analysis findings inline; point it at `config/detekt/detekt.yml` and `config/detekt/baseline.xml`.

### Caveats

- **Kotlin 2.3.21.** detekt 2.0 (alpha) is compiled against an exact Kotlin compiler version and refuses to run on a mismatch. The project is pinned to the Kotlin version detekt's alpha targets. A Kotlin bump may require a matching detekt alpha (and vice versa) until detekt 2.0 reaches stable.
- **No type resolution (yet).** CI runs the flat `detekt` task, which analyses sources without type resolution. The type-aware variants (`detektMain`/`detektTest`) are intentionally left out for now - they are the most fragile path on the detekt alpha. Rules that require type resolution therefore do not run yet; enabling them is a follow-up once detekt 2.0 stabilises.
- **detekt baseline** currently freezes the existing legacy findings (`config/detekt/baseline.xml`). `MaxLineLength` overlaps with ktlint's line-length rule and is a planned follow-up to disable in detekt (ktlint owns line length).
- Generated KSP/Konvert sources under `build/generated/**` are excluded from ktlint and are not picked up by detekt.

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
| `util` | Helpers and constant holders, grouped by domain (`util/telegram`, `util/water`, ...) |
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

The question to ask is binary: **is the type visible outside over HTTP?** Yes - `request`/`response`. No - `internal`. There is no other place to put it.

Naming: what an endpoint returns carries the `*Response` suffix; a shape nested inside a response has no suffix (`WaterEntry`, `ShortUserInfo`, `UserCounts`). Internal carriers end with `*Dto`, except message contexts, which end with `*Context`.

### Constants

Hardcoded values do not live in services or controllers. Domain constants go to `util/<domain>/<Feature>Constants.kt`; a value that only makes sense inside a single annotation (`@Max(100)`) stays inline.

## Tests

Test layout, tags and coverage live in [`TESTING.md`](TESTING.md).

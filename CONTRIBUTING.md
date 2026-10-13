# Contributing to ServerListPlus

Thanks for helping! This guide gets you from a fresh clone to a pull request.
For how the mod is put together, read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Setup

- **JDK 21 and JDK 25.** Minecraft up to 1.21.11 needs 21, 26.1+ needs 25. Gradle downloads
  missing JDKs by itself.
- **IntelliJ IDEA** with the [Minecraft Development](https://plugins.jetbrains.com/plugin/8327-minecraft-development)
  and Stonecutter plugins is recommended (see the [Stonecutter docs](https://stonecutter.kikugie.dev/)). Open the project folder and let Gradle import it.

```sh
./gradlew :core:test                  # unit tests of the version-independent logic
./gradlew compileJava                 # compile every Minecraft version and loader
./gradlew :26.3-fabric:runClient      # start the game with the mod
./gradlew :26.3-fabric:runClientGameTest   # start the game and click through the screens
./gradlew buildAndCollect             # build all jars into build/libs/<mod version>/
```

## Project layout

| Path | What lives there |
|---|---|
| `core/` | Data model, storage, search, sorting. Plain Java with JUnit tests, **never imports Minecraft**. |
| `src/main/java/.../bridge/` | Connects vanilla objects (`ServerData`, `ServerList`) to the core. |
| `src/main/java/.../compat/` | Small wrappers for APIs that differ between Minecraft versions. |
| `src/main/java/.../mixin/` | Hooks into vanilla classes; kept thin, the logic is elsewhere. |
| `src/main/java/.../ui/` | Widgets, plus `ui/serverlist/` for everything on the multiplayer screen. |
| `src/main/java/.../platform/` | Fabric and NeoForge entry points. |
| `src/gametest/` | Client game tests (Fabric, newest Minecraft version only). |
| `stonecutter.properties.toml` | Dependency versions and compatibility ranges per Minecraft version. |
| `settings.gradle.kts` | The list of Minecraft versions that are built. |

## One source tree, many versions

The project uses [Stonecutter](https://stonecutter.kikugie.dev/): the same files are built for
every Minecraft version and both loaders. Code that differs is switched with comments:

```java
//? if >=1.21.9 {
list.updateSizeAndPosition(width, height, x, top);
//?} else {
/*list.updateSizeAndPosition(width, height, top);
list.setX(x);
*///?}
```

The files in `src/` always show the code of the **active version** (set in
`stonecutter.gradle.kts`, currently `26.3-fabric`); the other branches are commented out.
To work on another version, run `./gradlew "Set active project to 1.21.1-neoforge"`, and
switch back with `./gradlew "Reset active project"` before committing. CI runs that task
and fails if it changes any file.

Some rules that keep this manageable:

- Prefer one version check in `compat/` over the same check in many places.
- Write the active version's branch uncommented and every other branch commented out, as above.
- After changing version-specific code, run `./gradlew compileJava` to compile all versions.
- Loader-specific code uses `//? if fabric` and `//? if neoforge` the same way.
- Plain renames are handled by `replacements` in `stonecutter.gradle.kts` instead of version
  comments, e.g. `GuiGraphics` becomes `GuiGraphicsExtractor` from 26.1 on. Write the active
  version's name; Stonecutter rewrites it for the others.

## Code style

- Match the surrounding code: naming, comment density and idioms. Use imports, not fully
  qualified class names.
- Comments explain *why*, not *what*. Every class has a short Javadoc saying what it is for.
- Mixin members are prefixed with `serverlistplus$` and marked `@Unique`.
- Text shown to players comes from `src/main/resources/assets/serverlistplus/lang/`.
  Add every new key to both `en_us.json` and `de_de.json`; other languages are welcome.
- `.editorconfig` sets indentation and line endings; most editors pick it up automatically.

## Tests

- **Core logic** gets JUnit tests in `core/src/test`. Every bug fix there comes with a test
  that fails without the fix.
- **Screens** are covered by client game tests in `src/gametest`. They start the real game
  in a window, change data, click through the screens and check the result. Screenshots of
  every step land in `versions/26.3-fabric/build/run/clientGameTest/screenshots/`, which is
  the quickest way to see a UI change. Extend them when you change a screen.
- **README screenshots** are taken the same way with real public servers:
  `./gradlew :26.3-fabric:runClientGameTest -PreadmeScreenshots`, then copy the files from the
  screenshot folder to `docs/images/`. This needs the network, so it is not part of CI.
- Changing the **storage format** (`servers.json`): raise `Store.CURRENT_VERSION`, add a
  step to `Migrations`, and add a test in `JsonStoreTest` that loads a file in the old format.
  Never change an existing migration step; players may still have files that need it.

## Adding a Minecraft version

1. Add the version to `settings.gradle.kts` and its dependencies and Java version (`deps.java`)
   to `stonecutter.properties.toml`.
2. Run `./gradlew compileJava` and fix what breaks with version comments, preferably in `compat/`.
3. Start the game for the new version and check the multiplayer and Direct Connection screens.

## Releasing

1. Raise `mod.version` in `stonecutter.properties.toml` and the `/*$ mod_version*/` constant in
   `ServerListPlus.java` (CI fails if they differ).
2. Turn `[Unreleased]` in `CHANGELOG.md` into `[x.y.z] - date`; its entries become the release notes.
3. Commit, then push a tag `vx.y.z`. The release workflow builds every version, uploads the jars
   to Modrinth (and CurseForge once `publish.curseforge_id` is set) and creates the GitHub release.

`./gradlew publishMods` without `MODRINTH_TOKEN` is a dry run that checks the upload setup.

If an upload failed or a platform was added later, the **Publish** workflow (Actions tab →
Run workflow) uploads an existing tag again, limited to one platform, loader or some versions.

## Pull requests

- One topic per pull request. Describe what changes and why; add a screenshot for UI changes.
- Commit messages are one line in the imperative, like the existing history:
  `Add a preview to the direct connection screen`.
- Before opening it, run `./gradlew :core:test compileJava` and, for screen changes,
  `./gradlew :26.3-fabric:runClientGameTest`. CI runs the build for every version.

## Reporting bugs

Use the [issue templates](https://github.com/PrexorJustin/ServerListPlus/issues/new/choose).
Mention your Minecraft version, loader and other mods, and attach `logs/latest.log` or the
crash report if there is one.

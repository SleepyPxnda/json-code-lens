# JSON Log Lens

An IntelliJ plugin that makes JSON log lines in the Run/Debug console readable.
Every console line that is a complete JSON object is shown through a template of your choice.
All other lines stay as they are.

```
{"time":"2026-11-25T16:16:28+01:00","level":"INFO","source":{"file":"http/http.go","line":145},"msg":"finished request"}
```

with the template `{time} {level} {msg} ({source.file}:{source.line})` becomes

```
2026-11-25T16:16:28+01:00 INFO finished request (http/http.go:145)
```

## Installation

1. Download `json-log-lens-<version>.zip` from the [latest release](https://github.com/SleepyPxnda/json-code-lens/releases/latest).
2. In IntelliJ: **Settings | Plugins | ⚙ | Install Plugin from Disk…** and pick the ZIP.

Works with IntelliJ-based IDEs from version 2025.3 on.

## Configuration

**Settings | Tools | JSON Log Lens**

- A checkbox turns the formatting on and off.
- The template field defines the output.
- The preview shows what example console output looks like with the current template, while you type.

## Template syntax

| Syntax | Meaning |
|---|---|
| `{level}` | Value of the field. Missing or null fields become empty, objects and arrays are printed as JSON. |
| `{log.level}` | Key named exactly `"log.level"` if present, otherwise the nested field `"log": {"level": ...}`. |
| `{#logger}[{logger}] {/logger}` | Section: its content is only shown if the field is present and not empty. Sections can be nested. |
| `\t` `\n` `\\` | Tab, line break, backslash (the template field is single-line). Other backslashes stay as they are. |

Example: `{time} {level}\t{#logger}[{logger}] {/logger}{msg}`

## Good to know

- The console receives process output in chunks that do not always end at a line break. A trailing piece
  that starts with `{` is held back until the rest of its line arrives, so long JSON lines are still formatted.
  Such a piece appears once its line break arrives or the process ends.
- Formatting applies to new output. Lines already in the console do not change when you edit the settings.

## Development

Requires JDK 21.

- `./gradlew runIde`: start a sandbox IDE with the plugin
- `./gradlew test`: run the tests
- `./gradlew buildPlugin`: build the installable ZIP into `build/distributions/`

## Releasing

1. Set `version` in `build.gradle.kts`, e.g. `1.1.0`, and commit.
2. Tag and push: `git tag v1.1.0 && git push origin v1.1.0`
3. The [release workflow](.github/workflows/release.yml) runs the tests, builds the ZIP and attaches it to a
   new GitHub release for that tag. It fails if the tag does not match the version in `build.gradle.kts`.

## License

[MIT](LICENSE)

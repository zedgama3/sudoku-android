# Contributing

Thanks for your interest! Bug reports, puzzle/hint issues and pull requests are all welcome.

## One change per pull request

**Each pull request should contain exactly one logical change**: one bug fix, one feature or
one refactor. Small, focused PRs get reviewed and merged quickly. Large or mixed ones will be
asked to split.

- Don't bundle unrelated fixes, renames, formatting sweeps or dependency bumps into a feature PR.
  Open separate PRs for them.
- If a change needs groundwork (e.g. a refactor before a feature), send the groundwork as its own
  PR first.
- Keep the diff to what the change needs. No drive-by reformatting of untouched code.
- For anything bigger than a bug fix, **open an issue first** so we can agree on the approach
  before you spend time on it.

## Reporting bugs

Open an issue with:

- what you did, what you expected, and what happened
- device / Android version and app version (Settings → About, or the Play Store listing)
- for a wrong or confusing hint: the puzzle (a screenshot is fine) and which cell the hint was for

## Development setup

Requirements: Android Studio (or JDK 17+) and the Android SDK with platform 37.

```powershell
.\gradlew.bat :app:testDebugUnitTest   # unit tests
.\gradlew.bat :app:installDebug        # build and install on a connected device
```

If you have [Task](https://taskfile.dev) installed, `task --list` shows shortcuts. See
[DEPLOYING.md](DEPLOYING.md) for installing to a phone.

## Before you open the PR

- [ ] `:app:testDebugUnitTest` passes
- [ ] New engine logic (`engine/`) has unit tests. That layer is pure Kotlin and fully tested.
- [ ] You ran the change on a device or emulator if it touches the UI
- [ ] The code follows the style of the surrounding code (Kotlin official style, existing naming
      and comment density)
- [ ] The PR description says **what** changed and **why** in a few sentences

## Architecture notes

- `engine/` must stay free of Android imports.
- The project uses manual DI (`AppContainer`). Please don't add Hilt/Dagger, Room or other
  frameworks without discussing it in an issue first.
- This project is on AGP 9, which has built-in Kotlin support. **Do not** apply the
  `org.jetbrains.kotlin.android` plugin; it breaks the build.
- The app has no network access and collects no data. Changes that add permissions, analytics,
  ads or network calls need an issue and discussion first, because they change the app's Play
  Store privacy declarations.

## License

By contributing, you agree that your contributions are licensed under the
[Mozilla Public License 2.0](LICENSE), the same license as the project.

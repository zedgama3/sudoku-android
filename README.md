# Sudoku

A full-featured native Android sudoku game built with Kotlin and Jetpack Compose (Material 3).

## Features

- **On-device puzzle generator** — every puzzle has a unique solution, graded into
  Easy / Medium / Hard / Expert by the human techniques required to solve it
  (singles, locked candidates, pairs). One puzzle per difficulty is pre-generated
  in the background so "New Game" is instant.
- **Pencil notes** with automatic cleanup when a digit is placed
- **Two input styles** — cell-first (tap cell, then digit) or digit-first
  (arm a digit, then paint cells), selectable in settings
- **Tiered hints** — fixes a wrong entry first, otherwise teaches the next logical
  step (with technique name and involved cells highlighted), falling back to a reveal
- **Mistake checking** with optional 3-mistake limit
- **Undo/redo**, timer with pause (board hidden while paused), win detection
- **Save/resume** — the game auto-saves on every action and survives process death
- **Statistics** per difficulty: games, win rate, best/average time, streaks
- **Light/dark theme** following the system, with manual override

## Building

Open in Android Studio, or from the command line:

```powershell
.\gradlew.bat :app:testDebugUnitTest   # engine + serialization unit tests
.\gradlew.bat :app:installDebug        # build and install to a connected device
```

Requires JDK 17+ and the Android SDK with platform 37 (`platforms;android-37.0`).

## Architecture

Single `:app` module, three layers:

- `engine/` — pure Kotlin, no Android imports: bitmask MRV backtracking solver with
  uniqueness counting, dig-based generator, human-technique logical solver, grader,
  and hint engine. Fully unit-tested.
- `data/` — repositories over DataStore (JSON via kotlinx.serialization for the
  saved game, puzzle cache, and stats; Preferences DataStore for settings).
- `ui/` — Compose + ViewModels (StateFlow), Navigation Compose with type-safe
  routes. The board is a single `Canvas` composable (one draw pass for highlights,
  grid, digits, and notes; tap hit-testing by integer division).

Manual DI via `AppContainer` — no Hilt, no Room.

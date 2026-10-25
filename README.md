# Sudoku

A full-featured native Android sudoku game built with Kotlin and Jetpack Compose (Material 3).
No ads, no accounts, no network access: everything runs and stays on your device.

## Features

- **On-device puzzle generator** — every puzzle has a unique solution, graded into
  Easy / Medium / Hard / Expert by the human techniques required to solve it
  (singles, locked candidates, pairs). One puzzle per difficulty is pre-generated
  in the background so "New Game" is instant.
- **Pencil notes** with automatic cleanup when a digit is placed
- **Two input styles** — cell-first (tap cell, then digit) or digit-first
  (arm a digit, then paint cells), selectable in settings. A tap marks and a
  long-press enters (or the reverse, in settings); the toolbar toggle swaps them.
  Entries are only removed by erase, overwriting is opt-in, and arming a digit can
  jump the selection to the nearest cell holding it
- **Hint walkthroughs** — one button walks you through the easiest cell on the board
  step by step: it opens with the techniques involved (free, as tap-through chips
  that open explainer screens with worked examples), then lights up the house, the
  supporting cells and the digit as the reasoning unfolds, and ends by naming the
  digit — it never enters it for you. One charge per cell, taken the first time you
  ask for more than the opening step, and the card stays put through taps, notes and
  entries (and across process death) until you fill the cell yourself. Wrong entries
  keep their own count → highlight → correct flow when mistake checking is off.
  Earlier steps stay on the card below the current one. Debug builds can log each
  hint with its board (Settings → Developer → Hint logging, ≤10 MB rolling) for
  review; `task hintlog` pulls it over USB
- **First-run setup**: one screen asks how you like to enter numbers, what a tap does, and
  whether mistakes are flagged, so the defaults never surprise a new player
- **Mistake checking** with optional 3-mistake limit
- **Undo/redo**, timer with pause (board hidden while paused), win detection
- **Save/resume** — the game auto-saves on every action and survives process death
- **Statistics** per difficulty: games, win rate, best/average time, streaks
- **Light/dark theme** following the system, with manual override

## Privacy

The app requests no permissions, makes no network connections and collects no data. Games,
settings and statistics are stored only on the device (and in Android's own device backup, if
you have it enabled). See the full [privacy policy](PRIVACY.md).

## Building

Open in Android Studio, or from the command line:

```powershell
.\gradlew.bat :app:testDebugUnitTest   # engine + serialization unit tests
.\gradlew.bat :app:installDebug        # build and install to a connected device
```

Requires JDK 17+ and the Android SDK with platform 37 (`platforms;android-37.0`).
See [DEPLOYING.md](DEPLOYING.md) for step-by-step instructions on installing to a phone.

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

## Contributing

Issues and pull requests are welcome. Please keep each pull request to **one change**, and
read [CONTRIBUTING.md](CONTRIBUTING.md) first.

## License

Copyright © 2026 Jared Fowkes.

Licensed under the [Mozilla Public License 2.0](LICENSE). You're free to use, modify and
distribute this code, including in your own apps. If you distribute modified versions of
these files, the MPL requires you to make those modified files' source available under the
same license and to keep the copyright notices.

# Deploying to a phone

How to build the app and install it on an Android phone over USB. All commands are
PowerShell, run from the repository root (`c:\git\sudoku`).

## Prerequisites (one-time setup)

1. **JDK 17+** and the **Android SDK** with platform 37 (`platforms;android-37.0`) —
   both come with Android Studio. `adb` lives in the SDK's platform-tools folder
   (typically `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`); make sure it's
   on your `PATH` or call it by full path.
2. **Enable Developer options on the phone:** Settings → About phone → tap
   *Build number* seven times.
3. **Enable USB debugging:** Settings → System → Developer options → *USB debugging*.
4. Connect the phone over USB. The first time, the phone shows an
   **"Allow USB debugging?"** prompt — check *Always allow from this computer* and
   tap Allow.

## Deploy

1. Verify the phone is visible:

   ```powershell
   adb devices
   ```

   The phone should be listed with the state `device`. If it says `unauthorized`,
   accept the debugging prompt on the phone; if the list is empty, try another
   cable/port or re-enable USB debugging.

2. Build and install in one step:

   ```powershell
   .\gradlew.bat :app:installDebug
   ```

   This compiles the debug APK and installs it on **every** connected
   device/emulator. Upgrading an existing install keeps its data (saved game,
   stats, settings).

3. Launch it — tap the Sudoku icon, or from the terminal:

   ```powershell
   adb shell am start -n com.zedgama.sudoku/.MainActivity
   ```

## Alternative: install a pre-built APK

If you already ran a build (or want to copy the APK somewhere):

```powershell
.\gradlew.bat :app:assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

`-r` reinstalls while keeping app data. With more than one device connected, pick
the target with `adb -s <serial> install ...` (serials come from `adb devices`).

## Troubleshooting

- **`INSTALL_FAILED_UPDATE_INCOMPATIBLE`** — the phone has a build signed with a
  different debug key (e.g. from another machine). Uninstall first (this wipes the
  app's data): `adb uninstall com.zedgama.sudoku`.
- **Gradle can't find the SDK** — create `local.properties` in the repo root with
  `sdk.dir=C:\\Users\\<you>\\AppData\\Local\\Android\\Sdk`.
- **Phone not detected at all** — use a data (not charge-only) USB cable, and on
  the phone set the USB mode to *File transfer* if it's stuck on *Charging only*.

## Release builds

Day-to-day testing uses the **debug** build above. The Play Store gets a **release** bundle
signed with the upload key. Without the key configured, release builds still compile but come
out unsigned, so contributors don't need it.

### One-time signing setup (maintainer only)

1. Create the upload key (keep it **outside** the repo; `*.jks` is gitignored anyway):

   ```powershell
   mkdir $HOME\keys -Force
   & "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v `
     -keystore "$HOME\keys\sudoku-upload.jks" -alias upload -keyalg RSA -keysize 4096 -validity 10000
   ```

2. Add to `%USERPROFILE%\.gradle\gradle.properties` (your user-level Gradle config, never
   committed):

   ```properties
   SUDOKU_UPLOAD_STORE_FILE=C:/Users/<you>/keys/sudoku-upload.jks
   SUDOKU_UPLOAD_STORE_PASSWORD=...
   SUDOKU_UPLOAD_KEY_ALIAS=upload
   SUDOKU_UPLOAD_KEY_PASSWORD=...
   ```

3. Back up the `.jks` and both passwords (e.g. in a password manager). Play App Signing holds
   the real app signing key, so a lost upload key can be reset through Play Console support,
   but that takes time.

### Building a release

```powershell
task release-apk       # minified release APK; install it and play a game to catch R8 issues
task install-release   # same, then installs it (uninstall the debug build first: different key)
task bundle            # app\build\outputs\bundle\release\app-release.aab → upload to Play Console
```

Before each upload, bump `versionCode` (and `versionName`) in `app/build.gradle.kts`; Play
rejects a `versionCode` it has already seen.

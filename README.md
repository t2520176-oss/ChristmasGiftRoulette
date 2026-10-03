# Christmas Gift Roulette 🎄🎁

An offline Android party app. Enter 2–50 gifts (name, optional amount, currency), spin a festive
Canvas-drawn roulette wheel, and reveal the gifts one by one — **a gift can never be picked twice**
in the same game. Works on phones and tablets, portrait and landscape.

## Stack

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · ViewModel + StateFlow · DataStore
(Preferences) · Gradle Kotlin DSL. `minSdk 26`, `targetSdk/compileSdk 35`. No network access, no
third-party libraries beyond AndroidX/Compose.

## Open / build / install

* **Android Studio:** *File ▸ Open…* the project root, let Gradle sync, press ▶.
* **Command line** (JDK 17+ and Android SDK installed, `ANDROID_HOME` set):
  ```bash
  ./gradlew testDebugUnitTest      # unit tests
  ./gradlew assembleDebug          # builds the APK
  ```
  The APK is written to `app/build/outputs/apk/debug/ChristmasGiftRoulette-debug.apk` and copied to
  `output/ChristmasGiftRoulette-debug.apk`. Windows: use `gradlew.bat`.
* **No local SDK?** Every push runs the *Build APK* GitHub Actions workflow
  (`.github/workflows/build-apk.yml`); download `ChristmasGiftRoulette-debug-apk` from the run's
  *Artifacts* section.
* **Install on a tablet:** copy the APK over, open it, and allow "Install unknown apps" for your
  file manager/browser when asked (or `adb install -r ChristmasGiftRoulette-debug.apk`).

## Architecture

```
com.example.christmasgiftroulette
├─ model/       GiftItem, GiftDraft, CurrencyType, CurrencyFormatter, AmountParser   (pure Kotlin)
├─ game/        RouletteEngine, GameState, WheelMath, GiftValidator, GameRules       (pure Kotlin)
├─ data/        DataStore, SettingsRepository, GameRepository, PersistedSession (JSON)
├─ feedback/    FeedbackController (SoundPool + vibration, crash-safe)
├─ viewmodel/   GiftRouletteViewModel + GiftRouletteUiState (single owner of all state)
├─ navigation/  AppNavigation (phase ➜ destination)
└─ ui/          theme/ components/ setup/ roulette/ result/ settings/
```

MVVM with unidirectional data flow: Composables render `GiftRouletteUiState` and forward intents; all
rules live in `model/` and `game/` (no Android imports) and are unit-tested on the JVM.
The ViewModel survives rotation; the whole session (gift setup, remaining ids, selected ids and
order, current phase) is saved to DataStore, so it also survives process death and app restarts.

## Gift selection & the no-duplicate algorithm

Every `GiftItem` has a unique `id` (UUID) — names are never used for identity, so duplicate names
are fine. `GameState` keeps `gifts`, `remainingGifts`, `selectedGifts`, `currentWinner`.

1. `RouletteEngine.pickWinner` draws uniformly (`Random.nextInt(remaining.size)`) from
   **`remainingGifts` only**. The `Random` is injectable, so tests are deterministic.
2. The ViewModel stores a `SpinRequest(winnerId, extraTurns, offset)` and sets `isSpinning`
   (further taps are ignored).
3. `WheelMath.targetRotation` computes the exact wheel angle that puts the winner's segment under
   the pointer (5–7 full turns plus a random in-segment offset). The wheel animates there with a
   long ease-out (~5.2 s) — the result is *not* faked afterwards; the same geometry is verified in
   tests via `WheelMath.indexAtPointer`.
4. Only when the animation ends does `onSpinFinished` call `RouletteEngine.select`, which removes
   the id from `remainingGifts` and appends it to `selectedGifts`. Selecting an id that is no longer
   in the pool is a no-op, so a gift can never win twice. The next wheel is built from the new
   `remainingGifts`; the segment count always equals the remaining gifts.
5. When `remainingGifts` is empty the final screen lists the selection order. *Play again* restores
   all gifts; *New gift list* returns to setup.

## Currency system

`CurrencyType` is an enum (`code`, `symbol`, `displayName`, `fractionDigits`, `flag`). Amounts are
`BigDecimal` (no floating-point drift). `CurrencyFormatter` prints e.g. `₱500.00`, `$25.00`,
`¥3,000`, `€20.00`. No conversion happens — the currency is just part of the prize text.
Blank amounts are valid; `AmountParser` rejects bad input and too many decimals (KRW/JPY have none).

### Adding a currency

Add one line to `model/CurrencyType.kt`, e.g.

```kotlin
CHF("CHF", "CHF ", "Swiss Franc", 2, "🇨🇭"),
```

Currency chips, formatting, validation and persistence pick it up automatically.

## Sound & vibration

Settings (⚙) let users toggle sound, vibration and "confirm before reset"; they are stored in
DataStore. Synthesized effects are bundled in `app/src/main/res/raw/`:
`sfx_tick.wav` (segment tick), `sfx_spin.wav` (spin start), `sfx_win.wav` (winner).
To use your own sounds, replace those files keeping the names (`.wav`, `.ogg` or `.mp3`).
To add a new effect add an entry to `SoundEffect` in `feedback/FeedbackController.kt` and drop a
file with that `rawName` into `res/raw`. Missing files or devices without a vibrator are ignored
silently. `python3 tools/generate_sounds.py` regenerates the bundled placeholders.

## Layout notes

Content is centred with maximum widths on large screens; ≥ 840 dp wide the setup and result screens
switch to two panes, and the roulette screen switches to side-by-side in landscape. All lists scroll,
touch targets are ≥ 48 dp, and icons carry content descriptions.

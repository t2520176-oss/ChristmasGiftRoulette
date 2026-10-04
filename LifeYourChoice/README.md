# LIFE: YOUR CHOICE

*"Every decision builds the life you will live."* Same world. Different story.

A fully offline, choice-based life simulator for Android (Kotlin + Jetpack Compose). No account, no internet,
no ads, no purchases, no AI service. Progress is stored on the device.

## Get the APK
Every push builds it on GitHub Actions (workflow **Build LIFE - YOUR CHOICE APK**):
open the run → **Artifacts** → `LifeYourChoice-debug-apk` → unzip → `LifeYourChoice-debug.apk`.

Install: copy the APK to your phone/tablet, open it, and allow "Install unknown apps" for your file
manager/browser when asked (or `adb install -r LifeYourChoice-debug.apk`). Needs Android 8.0+.

Local build (JDK 17 + Android SDK): `./gradlew :app:assembleDebug` → `app/build/outputs/apk/debug/`
(a copy goes to `output/`).

## Layout
```
core/   pure Kotlin (no Android): engine, story data, saves, lessons + motto engine, tests
app/    Android app: ui/ (shared Compose screens + art, no Android imports), platform/ (Activity, audio)
tools/  generate_audio.py (synthesised sfx/music), ui-typecheck/ (compile-check the UI on a plain JVM)
```
* `core/.../story/packs/*` holds the 160+ scenarios as Kotlin DSL; add a `StoryPack` to `StoryContent`
  to add stories. The engine never hard-codes story.
* `StoryDirector` is the seam for a future optional AI director; Version 1 uses `RuleBasedDirector`.
* Tests: `./gradlew -PcoreOnly :core:test` (JVM only, no Android SDK needed). They validate all content,
  audit story flags for typos, and play thousands of simulated lives.

## Your Life's Motto (offline)
At the end of a life the report shows: report card → YOUR STORY → IMPORTANT CHOICES → WHAT YOU LEARNED
(3 lessons chosen from stats, hidden traits and story flags) → YOUR LIFE'S MOTTO. `MottoEngine` scores 22
categories from the life and picks one of 132 original mottos, avoiding recently awarded ones. The motto is
stored with each record in LIFE RECORDS and drawn on a shareable Life Card (no statistics shown).

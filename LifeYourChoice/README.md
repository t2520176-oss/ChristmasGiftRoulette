# LIFE: YOUR CHOICE  ·  Version 2A "The Cinematic Life"

*"Every decision builds the life you will live."* Same world. Different story.

A fully offline, choice-based life simulator for Android (Kotlin + Jetpack Compose) that plays like an
interactive life movie: characters walk, turn, sit, gesture and speak; the camera cuts and pushes in; the
choices appear at the natural pause of a scene and the scene carries on from what you said.
No account, no internet, no ads, no purchases and **no AI service of any kind**: every scene is hand-written
data performed by a built-in animation engine. Progress is stored on the device.

## Get the APK
Every push builds it on GitHub Actions (workflow **Build LIFE - YOUR CHOICE APK**):
open the run → **Artifacts** → `LifeYourChoice-debug-apk` → unzip → `LifeYourChoice-debug.apk`.

Install: copy the APK to your phone/tablet, open it, and allow "Install unknown apps" for your file
manager/browser when asked (or `adb install -r LifeYourChoice-debug.apk`). Needs Android 8.0+.

Local build (JDK 17 + Android SDK): `./gradlew :app:assembleDebug` → `app/build/outputs/apk/debug/`
(a copy goes to `output/`).

## What Version 2A adds
* **Cinematic scenes** – every story scene is a script of beats: place/enter/exit/move/turn/sit/stand, look,
  15 emotions, 20 gestures, props, dialogue, camera shots (establishing, wide, medium, close-up,
  over-the-shoulder, two-shot, reaction, follow, slow push-in), cuts, fades, captions ("3 YEARS LATER") and
  chapter cards (CHAPTER 1 · AGE 15 · THE SCHOOL YEARS … CHAPTER 7 · AGE 60 · THE LIFE YOU BUILT).
* **Choices inside the scene** – the player's choice is spoken by the player character, the others react and
  the scene resumes (and random outcomes of a choice can be staged differently).
* **Memory** – earlier choices change who comes back and what they say (`whenever(condition)` lines).
* **Characters** – drawn in code: customisable face, hair, outfit colour and voice type; the same rig is used
  for every character, ages from teen to elderly (greying hair, glasses) and dresses for the place.
* **Voice** – `VoiceManager` abstraction: bundled recordings (`res/raw/voice_<clipKey>`), else the device's own
  *offline* text-to-speech, else subtitles only. Subtitles are on by default and always shown when no voice
  is available. Voice / music / effects volumes and a cinematic-mode switch are in Settings.
* **Ending movie** – a montage built only from what actually happened (no wedding if you never married, no
  children if you had none, no business triumph if the business failed), the older player in a meaningful
  place, fade to black, **YOUR LIFE'S MOTTO**, then the logo. Then the report: Life Report → YOUR STORY →
  IMPORTANT CHOICES → WHAT YOU LEARNED → MOTTO, a Life Card and Life Records.
* Version 1 is still inside: the classic text screen is used when cinematic mode is switched off, and any
  scenario without a script is auto-staged (player + the character the text mentions + narration).

## Layout
```
core/   pure Kotlin (no Android): engine, story data, cinematic engine + scripts, saves, lessons, motto, tests
app/    Android app: ui/ (shared Compose screens, figure rig, stage, no Android imports), platform/ (Activity, audio, voice)
tools/  generate_audio.py (synthesised sfx/music), ui-typecheck/ (compile-check the UI on a plain JVM)
```
* `core/.../story/packs/*` – the 165+ story scenarios as Kotlin DSL (stats, hidden traits, NPC trust, flags,
  delayed consequences). The engine never hard-codes story.
* `core/.../cinema/` – `CineDirector` (deterministic scene sequencer, runs headless in tests), `CameraMath`,
  `AutoDirector`, `MontageBuilder`, `ChapterCards`; `cinema/scripts/*Cine.kt` are the scene scripts (data).
  Add a `CinePack` to `CineContent` to add scenes; the validator checks them against the story.
* `app/.../ui/art` – `Figure` (pose rig + `PoseSolver`), `Head` (expressions), `ScenePainter` (25 environments).
* `StoryDirector` is the seam for a future optional AI director; Version 2A still uses `RuleBasedDirector`.
* Tests: `./gradlew -PcoreOnly :core:test` (JVM only, no Android SDK needed). They validate all story and
  cinematic content, audit flags, check that speakers stay in frame, play every scene through every choice and
  outcome, simulate hundreds of lives, and check old (Version 1) save files still load.

## Adding a voice pack later
Drop `voice_<clipKey>.ogg` files into `app/src/main/res/raw/` (clip keys are the `audio = "..."` argument of
`say(...)`; lines without a key use the device voice). No code or scene changes are needed.

## Your Life's Motto (offline)
`MottoEngine` scores 22 categories from the life and picks one of 132 original mottos, avoiding recently
awarded ones. The motto is stored with each record in LIFE RECORDS and drawn on a shareable Life Card
(no statistics shown).

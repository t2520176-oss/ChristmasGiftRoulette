# Bake Your Way 🧁

An offline Android baking app. Pick what you want to bake, say how much flour you want to use (½ cup to
6 cups, or any custom amount, in cups or grams), choose Less Sugar or Normal Sugar, add optional
toppings and pick a cooking method (oven, air fryer, steam, stovetop). The app calculates the recipe.

* **40 recipes** in 12 categories, stored as JSON in `app/src/main/assets/data/`.
* **No internet, no account, no API keys, no paid services.** The manifest declares no permissions
  (a unit test enforces that there is no `INTERNET` permission).
* Kotlin · Jetpack Compose · Material 3 · MVVM · Navigation Compose · DataStore (favorites / recents).
* `minSdk 26` (Android 8.0), `targetSdk/compileSdk 35`. Phones and tablets.

## Build

```bash
cd BakeYourWay
./gradlew :core:test            # unit tests for the recipe data and the calculator (JVM only)
./gradlew :app:assembleDebug    # builds app/build/outputs/apk/debug/app-debug.apk
```

Needs JDK 17+ and the Android SDK (`ANDROID_HOME`). Opening the `BakeYourWay` folder in Android Studio
works too. Every push also runs the **Build Bake Your Way APK** GitHub Actions workflow, which uploads
`app-debug.apk` as the `BakeYourWay-debug-apk` artifact.

To work on the pure-Kotlin part without the Android toolchain: `./gradlew -PcoreOnly=true :core:test`.

Install on a device: copy `app-debug.apk` over, open it and allow "Install unknown apps", or
`adb install -r app-debug.apk`.

## Architecture

```
core/   (pure Kotlin, no Android)             app/   (Android, Compose)
  Models.kt          data model + enums         BakeApp, MainActivity
  Catalog.kt         JSON loader + validator    data/UserPrefsRepository   DataStore
  MeasureFormatter   fractions, cups/g/ml, eggs ui/MainViewModel           catalog + prefs state
  RecipeCalculator   scaling, sweetness,        ui/BakeNavHost             routes carry every choice
                     yield, steps, conclusion   ui/screens/*               Home, Customize, Method, Result ...
```

* **Recipe content is data, not code.** `ingredients.json` (each ingredient has its *own* grams-per-cup),
  `addins.json`, `toppings.json` and `recipes/<category>.json`. A recipe lists its ingredients at a
  reference flour amount, its own Less-Sugar multiplier, the cooking methods it supports (each with its
  own temperature/time/prep/doneness text) and its steps. Shared cooking profiles can be defined once per
  file (`profileSets`) and overridden per recipe.
* **Scaling.** `factor = chosen flour / recipe's reference flour`. Every scalable ingredient is multiplied
  by it; `SUGAR` ingredients are additionally multiplied by the recipe's sweetness multiplier. Eggs are
  shown as whole eggs plus beaten egg (never "1.33 eggs"). Measures are rounded to real kitchen sizes.
* **Cooking time never scales with flour.** Time and temperature come only from the recipe's profile for
  the selected method. More flour means more portions, shown as extra *batches* (or a bigger pan
  recommendation) with the same time per batch.
* **Instructions follow the method:** preheat/prepare, cook and doneness-check steps come from the selected
  method's profile; the mixing steps come from the recipe.

### Adding a recipe

1. Add an object to a file in `app/src/main/assets/data/recipes/` (or create a file and list it in
   `data/index.json`). Copy a similar recipe as a starting point.
2. Run `./gradlew :core:test`. `CatalogValidator` checks references, units, temperatures, sweetness
   multipliers, add-ins and more, and the calculator tests run every recipe in every method.

## Not in version 1 (by design)

AI assistant, recipe/shopping APIs, cloud sync, user-created recipes, substitutions, shopping list,
serving-size calculator, sharing, other languages. The data model and `RecipeCalculator` are kept free of
UI code so these can be added later.

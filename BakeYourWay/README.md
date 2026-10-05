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

### Missing ingredients & smart substitutions (fully offline)

On the Ingredients tab, **Missing an Ingredient?** (or **Don't have this?** on any ingredient row) looks the
ingredient up in the *current* recipe, shows the best substitute with the exact amount calculated from the
*scaled* quantity, what will change (texture / flavor / browning), a tip and a rating (BEST MATCH / GOOD
ALTERNATIVE / TEXTURE WILL CHANGE). **Use this substitute** updates the ingredient list ("↻ Substituted"),
the instructions and the "Your recipe summary" in the notes.

* `data/ingredient_substitutions.json` - the rules (`IngredientSubstitution`): original ingredient, conversion
  rule (ratios of the original amount, per-piece amounts, "fill to total"), compatible/incompatible
  categories, recipes and cooking methods, quantity limits, effects, warning, tip, confidence level
  (RECOMMENDED / ACCEPTABLE / LIMITED / NOT_RECOMMENDED) and structured `instructionOverrides`.
* `data/ingredient_aliases.json` - typed names ("AP flour", "icing sugar", "BUTTER" ...) and groups.
* `SubstitutionEngine` (core) - lookup, option ranking, amount math and applying a choice to the calculation.
  Recipe steps carry tokens such as `{L|=flour;#butter|butter}` and tags (`CREAM_FAT`, `MIX_DRY`, `MIX_WET`) that
  `StepText` renders, so a substitution rewrites the right steps (no "cream the butter" when oil is used) instead of
  doing text replacement. When no rule is suitable the app says so rather than inventing something.
* `SubstitutionEngineTest` and `CatalogValidator` check the rules, aliases, tokens and every applicable
  substitution in every recipe.

### Adding a recipe

1. Add an object to a file in `app/src/main/assets/data/recipes/` (or create a file and list it in
   `data/index.json`). Copy a similar recipe as a starting point.
2. Run `./gradlew :core:test`. `CatalogValidator` checks references, units, temperatures, sweetness
   multipliers, add-ins and more, and the calculator tests run every recipe in every method.

## Not in version 1 (by design)

AI assistant, recipe/shopping APIs, cloud sync, user-created recipes, shopping list,
serving-size calculator, sharing, other languages. The data model and `RecipeCalculator` are kept free of
UI code so these can be added later.

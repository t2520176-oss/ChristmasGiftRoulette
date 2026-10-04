# ui-typecheck

Compile-only desktop check for the shared Compose UI.

```bash
cd tools/ui-typecheck
gradle -Pkotlin.compiler.execution.strategy=in-process compileKotlin
```

It compiles `core/` and `app/src/main/java/com/lifeyourchoice/app/ui/**` together using JetBrains Compose
(Maven Central). Anything under `app/.../platform/` (Activity, audio) is Android-only and is *not* covered.

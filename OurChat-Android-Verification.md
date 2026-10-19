# OurChat Android release verification

- App: OurChat 1.0.0 (version code 1), package `app.ourchat.privatechat`.
- APK: `OurChat.apk`, 3,760,703 bytes.
- APK SHA-256: `0442b33d92dd310d00e513914cb915a86ec0d3030d72d1f75a8f3e1a87a08350`.
- Public source: `OurChat-android-source.zip`, 73,199 bytes, SHA-256 `eb20415ec12b075fc0ff912a937c97b3af0c7b4a9d29e58bbc3ee40174923b72`.
- Signing certificate SHA-256: `3B:4E:97:8F:A2:67:C2:3E:C3:07:C7:B9:4C:09:2B:DF:23:CE:55:72:F8:61:BD:1E:E7:1B:35:B9:7B:9E:63:22`.
- `apksigner verify --verbose --print-certs` passed with one RSA-3072 signer and APK Signature Scheme v2. `zipalign -c -P 16 4` passed.
- Release build and `lintRelease` passed using JDK 17, Gradle 8.13, AGP 8.13.1, SDK/build tools 36 and Android Browser Helper 2.7.4. Lint: 0 errors, 3 warnings (redundant activity label, Android 12 backup metadata recommendation, legacy launcher icon shape).
- APK manifest inspection confirms Android 7.0/API 24 minimum, API 36 target, the expected package, production HTTPS home URL, notification permission and delegation service, no debuggable flag, and cleartext disabled. ABH 2.7.4 requires API 24; API 23 was rejected by its manifest and is not supported by this release.
- The launcher accepts incoming deep links only on the configured HTTPS host at the default HTTPS port or port 443, without URL user information. Other inbound URLs fall back to home. The app uses browser Trusted Web Activity/Custom Tabs, with no configured WebView activity.
- The source ZIP uses an explicit 19-file allowlist. It includes the public certificate and association file; it excludes private signing keys/passwords, local SDK configuration, toolchains, dependencies and build caches. The original private signing key remains outside public source and is required for future updates.

The HTTPS website must serve the matching `assetlinks.json` at `/.well-known/assetlinks.json` for the browser to verify fullscreen trust. Before enabling notifications, users must tap **Enable notifications** inside OurChat and grant the browser/Android permissions. There is no native permission request on app launch.

Compilation and static checks do not test installation or phone behavior. Physical-device testing is still required for verified fullscreen launch, Android 16 notification permission handoff, foreground/background/locked message delivery, notification taps, sound, popups, denied permission and battery restrictions. The website and its existing account system provide messaging and Web Push. A current TWA-capable browser and network connection are required.

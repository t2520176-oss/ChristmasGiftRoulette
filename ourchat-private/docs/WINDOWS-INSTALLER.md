# Windows installer

`OurChat-Setup.cmd` is a single double-clickable installer with an embedded source ZIP. It targets Windows PowerShell 5.1 on 64-bit Windows 10/11, x64 or ARM64. Internet access to nodejs.org and registry.npmjs.org is needed on first installation. It does not request elevation or change the system PATH.

The installer verifies the embedded ZIP, rejects archive paths outside its destination, and downloads portable Node.js 24.22.0 from the official HTTPS distribution. Both architecture-specific Node ZIP SHA256 values are pinned from the official SHASUMS256.txt. Dependency integrity uses the supplied npm lockfile and `npm ci`.

Files live under `%LOCALAPPDATA%\OurChat`. The root and shared configuration folder restrict inherited permissions to the current user and SYSTEM before creating secrets. VAPID keys are generated directly into the private settings file and retained on retries. A partial existing key pair causes an error instead of silently rotating it. Source releases use separate hash-named folders; shared settings survive an update. Previous release folders are retained rather than automatically removed.

The desktop shortcuts start the private Node runtime. Configuration changes trigger a rebuild because public Next.js values are compiled into the client. The server binds only to `127.0.0.1:3000`. Startup checks the login route before opening the browser. A busy port or an existing OurChat instance is reported without terminating unrelated processes. Keep the console open while using the local app.

The optional settings dialog accepts a Supabase project URL and publishable key, with an optional masked service-role key and administrator email for push. It does not create or modify an external project, users, SQL, or a hosted deployment. Skipping connection settings gives a setup preview. Follow README.ko.md for the database, exactly two users, membership, and deployment prerequisites. Use a hosted HTTPS deployment for access from phones or other computers.

Build the release from the source checkout with Python 3:

```sh
python scripts/build-windows-installer.py --output ../downloads
```

Generated `OurChat.zip`, `OurChat-Setup.cmd`, and `SHA256SUMS.txt` must be delivered together. The installer embeds exactly the archive whose checksum it verifies. Never add `.env.local`, dependencies, or build output to the source archive.

Run the helper checks using an available PowerShell and Node.js installation:

```powershell
powershell -NoProfile -File tests/installer/check-installer.ps1
```

The helper checks have passed on Linux PowerShell 7: 20 installer assertions and 17 private configuration assertions. They test valid ZIP extraction, rejection of traversal/absolute/symbolic-link entries, mismatched SHA256 rejection, architecture selection, valid matching VAPID generation, repeat preservation, and refusing incomplete existing keys without changing the file. The generated CMD also passed embedded ZIP, source checksum manifest, CRLF framing, and extracted PowerShell parsing checks. Lint and TypeScript checks passed after the installer was added.

Windows PowerShell 5.1, Windows Forms interaction, desktop shortcuts, Windows folder permissions, and actual double-click installation require a real Windows test; Linux PowerShell 7 parsing alone does not validate them.

> **Working repository:** https://github.com/vikaskunjbiharishah-ui/GUD.git — branch `main` only. `shahvikask/kosha` is the frozen original reference, not the repository to commit to.

# Deliberate GitHub Actions regression triggers

Normal code pushes to isolated `GUD/main` must **not** consume build credits.

- `native-android.yml` is runnable manually and also watches **only**
  `.github/ci-trigger/native-regression.txt`. The trigger file is
  intentionally absent after a development milestone.
- During the **next separate regression request**, create/update that exact
  marker with the approved source commit SHA. This produces one new commit
  and one GitHub-hosted native build.
- `native-device-matrix.yml` is manual because it runs three emulator jobs.
- `ci.yml` (React/Tauri legacy tests) is manual. Production signing is manual,
  SHA-pinned and secret-protected.

Do not add secrets, financial data or local passwords to the trigger file.

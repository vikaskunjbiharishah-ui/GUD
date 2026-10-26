> **Working repository:** https://github.com/vikaskunjbiharishah-ui/GUD.git — branch `main` only. `shahvikask/kosha` is the frozen original reference, not the repository to commit to.

# GrowUrDhan Android (`GUD-Android`)

Commercial phone app. Windows stays on `GUD-Windows`. Daily UI work stays on `Grok-Improvised-FrontEnd`, then merge into this branch.

## What ships

- Android App Bundle (`.aab`) for Google Play — never a store APK
- `targetSdk` / `compileSdk` **36**
- Application id `in.growurdhan.app`
- Play App Signing (upload key in CI secrets; Google holds the app signing key)
- 30-day trial, then Play Billing only

## Licence SKU (create in Play Console)

| Product id | Type | Price |
|---|---|---|
| `gud_annual` | Subscription, yearly, recurring | ₹99 |

There is one paid product. No lifetime licence, no Gmail add-on, and no separate support SKU.

The app is free to install. A genuine Play install starts a 30-day trial on the device. After that the vault file may remain, but the app locks until `gud_annual` is active. While the year is paid, the vault and Gmail sync stay on. Cancel in Play; access lasts until that period ends. A sideloaded APK stays locked unless the same Play account already has the year.

## Encryption

- WebView snapshot is AES-GCM (`GUDENC01`)
- On the phone the file is `EncryptedFile` + Android Keystore
- `allowBackup=false` and data-extraction rules exclude the vault
- No exported `ContentProvider`

Rooted spyware can still photograph the screen while unlocked. That is out of scope. At rest, other apps do not get a readable database.

## CI

- `android-aab.yml` — AAB artifact; draft upload to Play internal if `PLAY_SERVICE_ACCOUNT_JSON` is set
- `android-enhancements.yml` — notes for support subscribers. Play cannot ship a different binary per subscription; enhancements are gated in the same AAB
- `marketing.yml` — privacy, deletion, listing copy to GitHub Pages

### Secrets to add

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`
- `PLAY_SERVICE_ACCOUNT_JSON`

First AAB upload to Play must be manual so App Signing can enrol.

## Reviewer login (Play Console → App access)

The vault is on-device. Reviewers create one with:

- Email: `play.review@growurdhan.in`
- Password: `PlayReview#2026`
- PAN: `AAAPG1234A`
- DOB: `1985-01-15`
- Questions: first pet `Lotus`, city of birth `Pune`

Trial is 30 days, so review does not need a purchase. Add licence testers in Play Console for purchase SKUs.

## Support vs everyone

Security patches go to every licensed install (Play policy and user safety). Annual support unlocks enhancement flags in the app (`enhancementsEnabled`).

## Play reviews

After 7 days from first open, an unlocked vault may show a rating sheet. Submit opens Google Play In-App Review so the rating posts to the Play listing. Close / Not now snoozes 2 days. Family → Rate on Google Play is always available and stops the auto-prompt.

Play may hide the in-app sheet (quota, already reviewed). The app then opens the store listing. Sideloaded test APKs skip the in-app sheet and open the listing. GrowUrDhan does not collect the rating.

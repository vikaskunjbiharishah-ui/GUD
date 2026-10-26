> **Working repository:** https://github.com/vikaskunjbiharishah-ui/GUD.git — branch `main` only. `shahvikask/kosha` is the frozen original reference, not the repository to commit to.

# Play Console checklist (GrowUrDhan)

Complete these in the console. The repo cannot tick them for you.

## Technical

- [ ] Create app `in.growurdhan.app`
- [ ] Enrol **Play App Signing** (first AAB upload — Google holds the app signing key; CI holds only the upload keystore)
- [ ] Upload AAB from Actions artifact `GrowUrDhan-Android-AAB` (not an APK)
- [ ] Confirm target API 36 on the bundle details page
- [ ] Closed testing with testers if the developer account is new (Play requires ~14 days of closed testing before production)

## Store

- [ ] Paste `marketing/play-listing-en.txt` and `play-listing-hi.txt`
- [ ] Feature graphic 1024×500 + phone screenshots (at least 2)
- [ ] Privacy policy URL: `https://shahvikask.github.io/kosha/privacy.html`
- [ ] Terms URL (optional extra): `https://shahvikask.github.io/kosha/terms.html`
- [ ] Category: Finance. Ads: **No**

## App content

- [ ] Data safety from `marketing/data-safety.yml` — no ads; financial info stays on device; Play Billing is Google’s collection
- [ ] Target audience: **18+ only**. Not Designed for Families
- [ ] Content rating questionnaire (finance, no violence, no user-generated public chat)
- [ ] App access: reviewer credentials in `docs/ANDROID.md`
- [ ] Account deletion URL: `https://shahvikask.github.io/kosha/delete-account.html`
- [ ] Sensitive permissions: none expected (no SMS, contacts, location). If the CAS file picker needs photos/files, declare “files and docs the user picks”

## Products

- [ ] Subscription `gud_annual` — ₹99 / year, recurring. This is the only paid product.
  - One base plan, billed yearly. Do not publish `gud_lifetime`, `gud_gmail`, `gud_gmail_upgrade`, or `gud_support_annual`.
- [ ] Licence testers for `play.review@growurdhan.in`

## Trial (read this)

The app is free to install. A genuine Play install starts a 30-day trial on that device. Play does not run that trial: a Play free-trial offer would auto-charge, and this trial must not.

1. During the 30 days every feature is on, including Gmail sync.
2. After 30 days the vault file may stay on the phone, but the app locks until `gud_annual` is active.
3. Checkout is Google Play Billing only. No Stripe, UPI, or “pay on our website”.
4. While the year is paid, the vault stays open and Gmail sync stays on. It is not a lifetime licence. Cancel in Play; access continues until that period ends, then the app locks.
5. A copied APK stays locked unless that same Play account already has an active `gud_annual`.

## Payments policy

- [ ] No Stripe, UPI deep links, or “pay on our website” inside the app
- [ ] Trial and the ₹99 yearly price stated on onboarding and on the licence screen
- [ ] The subscription can be cancelled in Play; there is no second product left behind

## Reviews

- [ ] The app uses Play In-App Review (`play:review-ktx`) after a 7-day wait. That only works on a genuine Play install from an internal / closed / production track.
- [ ] Leave the store listing public enough for testers to write a review
- [ ] Ratings land on this app’s Play listing — there is no GrowUrDhan feedback server

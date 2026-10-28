# GrowUrDhan Native — AUTONOMOUS BUILD PLAYBOOK for Grok (x86 emulator edition)

Version 1.1 · 2026-10-28 · written by Claude for the owner (Vikas) after reviewing `vikaskunjbiharishah-ui/GUD` `main` @ `3488730` · agent: Grok
Install as `docs/PLAYBOOK.md`. You will also create `docs/STATE.md` (live state), `docs/RUN-REPORT.md`, `docs/PHONE-TEST.md`.
READ THIS WHOLE FILE FIRST. It is self-contained and the owner's current instruction. If it conflicts with your habits or with older text in the repo, this file wins (except the repo-boundary and frozen-parser rules, which it repeats).

## 0. Mission, starting state, honest limits
Finish the native Android app **GrowUrDhan** (local-first Indian household portfolio tracker) in `android-native/`: Kotlin, Jetpack Compose, Material 3, Hilt, SQLCipher. Target: every requirement in section 7 (P01–P39, E01–E18) and the queue in section 6.

**Observed starting state (verify, do not trust blindly):**
- Modules: `:app` (82 Kotlin files), `:domain` (108, pure Kotlin: FIFO lots, XIRR, tax rules, goldens), `:data` (22: Keystore-wrapped SQLCipher vault, encrypted backup, DataStore), `:core:model` (scaled money/quantity), `:casparser-runtime` (Chaquopy wheels). About 50 test files. Application id `in.growurdhan.app`, minSdk 26, compile/targetSdk 36, Kotlin 2.2.20, AGP 8.13.2, Compose BOM 2025.09, Billing 7.1.1, play-services-auth.
- Existing screens: Home, Portfolio, Insights (read-only), Vault, Local login, Password recovery/change, Welcome, Family member/administration, Custody-holding, Manual trade, Statement review, Settings, Privacy, Licence, Emergency export, Legacy backup, Portable recovery, Backup folder, Tax-owner scope.
- The README says the latest `main` is **not Gradle- or device-verified**. Treat the build as possibly broken (Case C below). Open per README: app-password login parity, full transaction editing, Gmail OAuth, NPS import, official price refresh, Play Billing, AppSec, physical-device sign-off.
- Observed gaps: no WorkManager (Gmail sync scheduling), NSE/BSE price adapters partial, owner-lens/tax-rule code thin, Hindi minimal.
- The original React/Tauri app in `src/` and `src-tauri/` is the **as-built functional reference** (types, formulas, screens). Read it for the exact field/formula of the task at hand only. Do not migrate it wholesale.

You work alone for about an hour per run and NEVER ask the owner questions. Decide, record in `docs/STATE.md`, continue. A smaller tested thing beats a larger untested thing.

You CANNOT do these. Build everything around them, mark `BLOCKED-OWNER`, never claim done: real Gmail OAuth consent; real Play Billing purchases; production signing keys; installing on the owner's phone; genuine CAS PDFs (owner's private statements); independent penetration test; owner visual sign-off.

## 1. Operating loop (repeat until time or unblocked work runs out)
1. `date` at start; Budget = 60 min unless the prompt says otherwise. At T-10 min: stop new work, final commit, report (section 9).
2. Pick the next unblocked task from section 6. Do whole vertical slices (domain + data + UI + tests), not scaffolding.
3. Build and test on the x86 emulator (section 4). Fix until green. If one problem takes >15 min, take the simpler route or revert to the last green commit and record why.
4. Commit after EVERY green slice with ids in the message (`P10: 8-category holding form + restart tests`). Never commit a red build. Push to `main` at checkpoints and at the end.
5. Update `docs/STATE.md` at every commit. It is your memory; a re-run of the same prompt must continue from it.
6. Do not touch `.github/ci-trigger/native-regression.txt` or any workflow: a push to `main` starts no paid build unless that marker changes. You do not rely on CI; your emulator is the test bed.

## 2. Rules (non-negotiable)
- **Repo boundary:** commit only to `vikaskunjbiharishah-ui/GUD`, branch `main`. Never commit to `shahvikask/kosha` or other branches. Keep the original notes in `docs/`; add files, do not delete them.
- **AGENTS.md:** it currently says not to follow "the removed native-redevelopment instruction set". That refers to an old Kosha spec; do not read or follow it. Your FIRST commit replaces AGENTS.md with: working repo/branch, "frozen parser under `src/casparser-main` must not be edited", "original app in `src/` is the functional reference", and "follow `docs/PLAYBOOK.md` (owner-authorized 2026-10-28) and keep `docs/STATE.md` current".
- **Stack:** Kotlin + Compose + Material 3 + Hilt. Keep existing architecture, package `in.growurdhan.*`, module names. No WebView finance UI, no cloud book of finances. Python only as the frozen CAS parser via Chaquopy.
- **Money/quantity:** signed scaled integers, money scale 10,000, quantity scale 100,000,000; overflow-checked; never Double/Float/REAL for stored amounts. ISO dates. XIRR/tax deterministic with injected dates and prices.
- **Never invent data:** no made-up buys, prices, costs, tax law, ownership. Unknown cost stays UNKNOWN and looks different from zero. Tax output labelled "estimate, not advice" with FY and rule version.
- **Ledger:** append-only (DB triggers reject UPDATE/DELETE). Fixes are new CORRECTION/supersession events. Imports idempotent by source fingerprint. Money writes recheck the authorized session inside the same SQL transaction. Crashes never leave half-imported books.
- **Security:** random SQLCipher key wrapped by Keystore (not derived from the app password); lock on background; FLAG_SECURE on sensitive screens; reject stale-session results; wipe secrets after use; parameterized SQL; input limits; `android:allowBackup="false"`. Never log PAN, DOB, emails, tokens, account numbers, PDF bytes, keys, statements. No secrets in git (keystores, `local.properties` ignored).
- **Backup:** this repo already has encrypted portable backup/recovery, legacy-backup import and a backup-folder screen. KEEP them, verify them with tests (restore round-trip, wrong-passphrase, tamper), and show the permanent-data-loss warning. Do not remove them.
- **Licence (decision):** the current truth is `src/lib/license.ts`, `Licence.kt` and `docs/ANDROID.md`: 30-day trial, then ONE yearly subscription `gud_annual` (₹99). Older docs and some old tests mention `gud_lifetime`, `gud_gmail`, `gud_support_annual`; treat those as superseded, keep old tests passing or update them with a recorded reason (STATE.md DECISIONS).
- **Frozen parser:** `src/casparser-main/**` and its invocation scripts are never edited. Record SHA-256 in `docs/frozen-hashes.txt` and verify in a test. Wrap with the existing typed adapter/validator.
- **Brand:** keep the CURRENT theme (green / gold / indigo in `GrowUrDhanTheme.kt`, light and dark). Do not re-theme. Improve only consistency, contrast and accessibility.
- **Honesty (most important):** no fake green. Never delete, skip or weaken a failing test. Never report a result you did not observe. Stage labels, exactly: `SOURCE` → `LOCAL` (JVM passed) → `EMULATOR` (passed on x86 emulator) → `DEVICE` (real phone) → `OWNER`. A compiling screen is not finished.
- **Test data:** only synthetic families/folios/PANs (obviously fake). Never commit real statements.

## 3. First 15 minutes: make the baseline real
1. `git log -5`, read `android-native/README.md`, `docs/ANDROID.md`, `settings.gradle.kts`, `app/build.gradle.kts`, `gradle/libs.versions.toml`. Need JDK 17, Android SDK 36, Gradle 8.13 (no wrapper jar is committed; use `gradle`, install 8.13 if missing; do not commit a generated wrapper).
2. Run the README commands (section 4). Record exact results per module in STATE.md: compiles? JVM tests pass? instrumented tests pass? Fix the build and failing tests first. A failing existing test is a finding: decide whether code or test is wrong, justify in STATE.md; never loosen to pass.
3. Run `python3 android-native/scripts/validate_schema.py` and `security_preflight.py` (+ its unittest); keep them green.
4. Write STATE.md MATRIX from what actually exists vs section 7 (file/test names as evidence). Print an 8-line receipt: build status, test baseline numbers, emulator status, AGENTS.md replaced, top 3 gaps, next task.

## 4. x86 emulator harness (adapt; verify every command)
```
ls /dev/kvm                                    # KVM needed for usable speed; fallback below
sdkmanager "platform-tools" "emulator" "platforms;android-35" "platforms;android-36" "system-images;android-35;google_apis;x86_64"
avdmanager create avd -n gud35 -k "system-images;android-35;google_apis;x86_64" -d pixel_6 --force
emulator -avd gud35 -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot-save &
adb wait-for-device; until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = 1 ]; do sleep 3; done
for s in window_animation_scale transition_animation_scale animator_duration_scale; do adb shell settings put global $s 0; done
# from repo root:
gradle -p android-native :domain:test :app:testDebugUnitTest
python3 android-native/scripts/validate_schema.py; python3 android-native/scripts/security_preflight.py
gradle -p android-native :app:assembleDebug :app:assembleDebugAndroidTest :data:assembleDebugAndroidTest
gradle -p android-native :data:connectedDebugAndroidTest :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=<comma list>
```
- API 35 matches the project's CI. Run only the classes you touched; full suites at milestone ends.
- Default ABI in `app/build.gradle.kts` is `x86_64`. **The `pypdfium2` wheel exists only for arm64**, so the real PDF parser cannot run on the x86 emulator. On the emulator test the CAS flow with synthetic parser-shaped JSON; real PDF parsing is `DEVICE: pending owner`. Never fabricate an x86 wheel and never edit the parser.
- Save JUnit XML summaries, screenshots and APK SHA-256 in `evidence/<yyyy-mm-dd-hhmm>/` (commit summaries/PNGs, not huge logs).
- No KVM/emulator: JVM tests + Robolectric/Compose tests + `assembleDebug`, and label every UI/DB claim `LOCAL`, not `EMULATOR`.
- Speed: `org.gradle.caching=true`, `--configuration-cache` if stable, no lint in test loops.

## 5. Gates (a slice is done only when all apply)
- Domain: JVM tests with golden vectors (FIFO, realized P&L, XIRR, interest/EMI, FY tax, look-through); source of each golden documented (original `src/lib/*.ts` or hand-checked).
- Data: SQLCipher create → write → close → reopen → read; FK/trigger/rollback tests; migration test per schema version; ledger mutation rejected.
- UI: instrumented Compose test of the real flow (enter → review → save → reopen) plus negatives (bad date, negative quantity, cross-owner access).
- Design gate for UI slices: screenshots at 360dp, 200% font, light and dark; TalkBack labels; touch targets ≥48dp; no clipped numbers; INR lakh/crore grouping. Save under `evidence/…/screens/`.
- Security per milestone: grep for secrets/PAN patterns, FLAG_SECURE present, no sensitive logging, `allowBackup=false`.
- A requirement is done only when STATE.md lists its test names and an `EMULATOR` result you observed.

## 6. Work queue (in order; build around BLOCKED-OWNER parts)
| Q | Work | Exit evidence |
|---|---|---|
| Q0 | Section 3: green build, baseline numbers, AGENTS.md replaced, STATE.md | build + baseline tests recorded |
| Q1 | Schema parity vs `src/db/schema.ts` (all legacy fields, triggers, migrations); app-password login parity: register (email, name, DOB, security questions, consent), login, lock/timeout, 180-day rotation, offline recovery, Keystore session binding | DB + auth tests on emulator |
| Q2 | Family & custody completeness: members (primary, HUF, minor/guardian, nominee), accounts (demat/folio/bank), goals + holding tags, one owner lens across all screens | owner-scoped CRUD tests, no cross-owner leakage |
| Q3 | Holdings & events: 8 categories, all specialist forms and `HoldingDetails` fields, add/review/save wizard, detail/edit/archive/close, ALL user event types incl. SPLIT/CORRECTION/OPENING_BALANCE/VALUATION_UPDATE UI, FIFO lots, search/sort/filter | per-category create→restart→negative tests; event goldens |
| Q4 | Statements: CAMS/KFINTECH/NSDL/CDSL/**NPS** (PDF/CSV/TXT), staged review → atomic commit, idempotency, history, issuer identity (no email/name-only auto-match) | synthetic-contract tests; real PDF BLOCKED-OWNER |
| Q5 | Engines and UI: interest/EMI schedules, returns (XIRR, absolute, annual), gains + FY tax estimate, growth, allocation look-through, Home dashboard + books-health; make Portfolio/Insights drill-downs real | golden suite + screenshots |
| Q6 | Prices: NSE/BSE alongside existing AMFI gateway, official close/NAV, as-of dates, offline cache, manual override, stale labels | mock-HTTP tests |
| Q7 | Settings, privacy, opt-in diagnostics, emergency dossier (masked by default, consent for plaintext), onboarding Hindi + English, terms/privacy acceptance, full a11y | a11y checks, screenshot matrix |
| Q8 | Gmail multi-account: OAuth interface, WorkManager monthly + manual sync, cursors, quarantine/review, member↔mailbox mapping, fake-gateway tests | mock tests; real OAuth BLOCKED-OWNER |
| Q9 | Licence: 30-day trial clock, `gud_annual` via Billing 7.1.1 behind an interface, restore purchases, fail-closed entitlement, fake gateway tests | logic tests; real purchases + signing BLOCKED-OWNER |
| Q10 | Full regression, offline/restart/low-storage, release-mode (minified) build, ARM64 phone APK (section 10) | all suites green; artifacts |

Realistic hour: Q0 plus 2–3 slices of Q1–Q3. Do not rush quality to cover more. Order matters: data and security, then holdings, engines, integrations.

## 7. Requirement digest (acceptance floor)
### 7.1 Data domains (encrypted, versioned)
identity/account, household entity, custody/folio account, instrument, holding, `ledger_event` (append-only), lot projection, price observation, source document, import batch, statement position, snapshots/history, tax rule set, loan/liability, goal + tag links, nominee/guardian/ownership evidence, settings, auth metadata, diagnostics/egress audit, security catalog, Gmail mailbox/link/cursor/dedup, licence/entitlement. Preserve all source fields and unknown-but-valid metadata; composite FKs, scoped uniqueness for member/account/folio/ISIN.
### 7.2 Eight categories
`EQUITY` (Large/Mid/Small cap, Multi&flexi, Sector&thematic, Other) · `MUTUAL_FUND` (Equity, Debt, Hybrid, Index/ETF, ELSS, Other) · `GOLD_SGB` (SGB, Physical 24K, Digital, Other) · `FIXED_INCOME` (PPF, EPF, NPS, SSY, Bank FD, SCSS, NSC, KVP, POMIS, Post Office RD, RBI Floating Rate Bond, Bond/GOI, Other) · `REAL_ESTATE` (Residential, Commercial, Land, REIT, InvIT, Other) · `SPECIALISED` (PMS, AIF, Unlisted equity, ESOP, Other) · `CASH_BANK` (Savings, Current, Liquid/overnight, Other) · `LIABILITY` (Home, Car, Education, Personal loan, Other). Enrolled holding with zero events is valid. Each category has its own fields and validation. Cross-check against `src/types.ts`.
### 7.3 `HoldingDetails` fields to preserve (where applicable)
goldHoldMode, goldPurity, goldHallmark, earlyExitDate, annualContributionMinor, contributionOn, compounding, tdsApplicable, npsTier, npsCra, npsPfm, faceValueMinor, areaSqFt, ownership, ownershipPercent, aifCategory, commitmentMinor, capitalCalledMinor, ifsc, strategyName, custodian, originalTenorMonths, rateType, linkedHoldingId, mfPlan, mfOption, mfHeldAs, depository, dpId, clientId, boId, altHoldMode, monthlyIncomeMinor, incomeDay, incomeStart, incomeEnd. Plus base fields: name, symbol/folio, ISIN, category/subcategory, owner, quantity, average cost, current price + source/as-of/staleness, review status, SGB issue/coupon/maturity, loan rate/institution/masked account/EMI/status/remaining tenor, purchase date, 2018 grandfathering FMV, notes, location, goal tags, AMC/RTA/AMFI, scheme type, UCC, exchange, demat/DP and custody links.
### 7.4 Eleven event types
`OPENING_BALANCE, BUY, SELL, BONUS, SPLIT, DIVIDEND, INTEREST, VALUATION_UPDATE, REPAYMENT, CORRECTION` (user-creatable, validated, explained) and `SNAPSHOT_ADJUSTMENT` (registrar reconciliation only; never a guessed BUY). Keep quantity, unit price, amount, charges, date, source fingerprint, owner/holding, notes, `supersedesEventId`, created-at. Fees applied in a documented order; check lots and negative positions.
### 7.5 Parity register
P01 register (email, name, DOB, security questions, consent) · P02 login/lock/timeout/180-day rotation/offline recovery · P03 encrypted backup import/restore (EXISTING here: verify, keep) · P04 lossless schema + migrations · P05 11 event types, append-only audit · P06 family CRUD, guardian/HUF/minor · P07 custody accounts, identifiers, nominees · P08 goals + tags · P09 eight categories/search · P10 add-holding wizard with review · P11 specialist holdings (SGB, gold, FD, PPF/SSY/EPF, NPS, REIT/InvIT, AIF, PMS, unlisted, property, loans) · P12 detail/edit/archive/close, dated valuation · P13 BUY/SELL/DIVIDEND/INTEREST/BONUS/REPAYMENT entry · P14 SPLIT/CORRECTION/OPENING_BALANCE/VALUATION_UPDATE · P15 holdings search/sort/filter, owner lens · P16 FIFO cost + sale P&L · P17 Documents screen · P18 CAS PDF/CSV/TXT import, passwords, staged review · P19 CAMS/KFINTECH/NSDL/CDSL/NPS issuer identity · P20 import history, snapshots, reconciliation, duplicate suppression · P21 FY gains STCG/LTCG, grandfathering, tax estimate · P22 interest/coupons (EPF/PPF/SSY/SCSS/NSC/KVP/POMIS/FD/SGB) · P23 EMI + schedule + states · P24 XIRR/absolute/annual returns, allocation, look-through · P25 Home dashboard + books health · P26 security catalog + official close/NAV · P27 price provenance, offline cache, manual override · P28 encrypted vault backup/export (EXISTING: verify, keep) · P29 backup folder/scheduler (EXISTING: verify, keep) · P30 emergency dossier (masked, consent) · P31 settings (theme, privacy, SGB RBI-vs-market, IBJA) · P32 opt-in diagnostics · P33 brand, Hindi onboarding, TalkBack, dark/light · P34 Gmail connect/disconnect, multi-account scan, dedup · P35 member↔mailbox mapping, quarantine, no cross-owner imports · P36 onboarding, terms/privacy acceptance, feedback/rating · P37 30-day trial + `gud_annual` entitlement, restore purchases · P38 Play Billing, acknowledgement, signed AAB · P39 final regression, ARM64 parser, device, AppSec, owner sign-off.
### 7.6 Enterprise register
E01 consistent M3 design system (current green/gold/indigo) · E02 accessibility (TalkBack, 200% font, 48dp, contrast, reduced motion) · E03 one household/owner lens everywhere · E04 legal/beneficial ownership evidence, no guessed fractions · E05 secure device admission + offline account-password recovery · E06 issuer support with confidence + quarantine + diagnostics · E07 multi-Gmail household workflow · E08 unknown cost / estimated / stale / unverified clearly distinct · E09 analytics drill-down, goals, contributions/income series · E10 market-data adapters with provenance, no PII in requests · E11 consented redacted diagnostics · E12 backup format/version interoperability (existing legacy-backup import: keep tested) · E13 onboarding and data-quality coaching · E14 modular, testable, typed errors, documented dependency graph · E15 SBOM, pinned dependencies, secret scan, threat model · E16 fail-closed licensing · E17 measurable release pipeline · E18 privacy-first, network egress inventory.

## 8. `docs/STATE.md` (≤150 lines; edit in place; never append logs)
`HEADER` (date, run number, commit, baseline numbers) · `MATRIX` (ids grouped: state SOURCE/LOCAL/EMULATOR/DEVICE/OWNER/BLOCKED-OWNER + test class names + evidence folder) · `QUEUE` (next 10) · `DECISIONS` (one line each: assumption and why) · `BLOCKERS` · `KNOWN GAPS`. Each run starts by reading ONLY this playbook and STATE.md. History lives in git.

## 9. End-of-run report (≤25 lines; also saved as `docs/RUN-REPORT.md`, overwritten)
```
RUN: <n> · TIME USED: <min> · COMMITS: <count, last short sha>
DONE (stage): <ids → LOCAL/EMULATOR> · TESTS: <passed/total by module>
PARTIAL / NOT DONE: <ids>     BLOCKED-OWNER: <list>
FAILED & FIXED: <one line each>     OPEN RISKS: <list>
NEXT RUN STARTS AT: <queue item>   ARTIFACTS: <paths>
```
Never write "complete" or "accepted" unless STATE.md proves it at `EMULATOR` or higher and the item is not blocked.

## 10. Phone build (ARM64) — at the end of EVERY run once the build is green
1. The project already switches ABI by environment: `GUD_PHONE_PARSER=1 gradle -p android-native :app:assembleDebug` builds an `arm64-v8a` APK (default is `x86_64`). Use a locally generated debug keystore, never committed. Copy to `release-artifacts/gud-arm64-debug.apk` with `SHA256.txt` and the commit SHA. Check with `unzip -l` that `lib/arm64-v8a/` holds the parser's native libraries (pydantic_core, pypdfium2).
2. Release/minified build if it works: `gud-arm64-release-unsigned.apk`. Production signing and AAB are owner-run (`native-signed-aab.yml`, protected secrets) and stay BLOCKED-OWNER.
3. Write `docs/PHONE-TEST.md` in plain language for a non-technical owner: copy the APK to the phone, allow install from that source, tap to install (or `adb install -r gud-arm64-debug.apk`). Then a 10–15 minute checklist: create account → lock/unlock → add family member → add one holding in each of the 8 categories → add a BUY and a SELL → check Home → import the owner's OWN CAS PDF (stays on the phone; never shared) → test backup + restore → dark mode, 200% font, TalkBack basics → note problems with screenshots.
4. The x86 emulator cannot prove ARM64 behavior or real PDF parsing: mark those `DEVICE: pending owner`.

## 11. Anti-patterns
- Ignoring this file or STATE.md and inventing a new plan; rewriting working modules, renaming packages, re-theming, or adding frameworks without need.
- Screens with hard-coded fake data called done; writing many files and compiling once at the end.
- Weakening assertions, deleting tests, or reporting results from runs you did not do.
- Touching the CI trigger file or workflows; committing keystores, real statements or `local.properties`.
- Spending the hour on documentation. Code + tests + evidence first; STATE.md and the report stay short.

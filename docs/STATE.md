# STATE

## HEADER
- Date: 2026-10-29 02:58–03:20 UTC. Run 3. Code head `b153968` (docs commit follows).
- Emulator `gud_x86` API 35. Gradle was stopped before `adb instrument`.
- Observed: data fixture + eight-category class 10/10; first-run handoff 1/1; owner lens 8/8.
- Full suites not repeated. Run 2 was app 77/77 and data 81/82. ARM64 APK rebuilt. Phone not used.

## MATRIX
- P01 handoff: EMULATOR `FirstRunIdentityHandoffInstrumentedTest`. `FirstRunIdentityCommit.saveIfAbsent` requires a fresh device credential, then writes identity. Logcat contained a marker and not the email, date of birth, or either answer.
- P09/P12: EMULATOR `EightCategoryRestartInstrumentedTest` 9/9. Each of the 8 categories creates specialist fields, closes, reopens, and refuses a bad date, negative units, and a cross-owner edit. Equity edit/archive/close reopens with no ledger event.
- Holding details: EMULATOR `allOriginalHoldingDetailKeysPersistWithTheirHistoricalValueTypes`. Was the only data failure in run 2.
- P15 owner lens: EMULATOR `OwnerLensShellTest` 8/8. Home, Portfolio, Custody, Tax, Statements, Family administration, plus Insights and Manual trade from run 2.
- Untouched instrumented classes from run 2 were not executed again.
- Evidence: `evidence/2026-10-29-0317/junit-summary.txt`.
- BLOCKED-OWNER: real Gmail consent, Play purchases, production signing, phone install, real CAS PDFs, pen test, owner visual sign-off. DEVICE/OWNER none.

## QUEUE
1. Q1 schema: `ledger_profile` still lacks settlement_date, unit_balance_after, description, entered_by.
2. Q4 NPS/CDSL staged import with synthetic contracts.
3. Q5 Insights drill-down is still read-only outside the owner filter.
4. Q6 NSE/BSE fetch beside AMFI.
5. Q7 Hindi onboarding and 200% font screenshots.
6. Q8 fake Gmail gateway.
7. Q9 `gud_annual` trial clock.
8. Q10 full regression and release minify.
9. Screenshot matrix at 360dp, 200% font, light and dark.
10. Activity UI test of the credential screen itself (the handoff after grant is what this run proved).

## DECISIONS
- `allOriginalHoldingDetailKeys…` no longer writes `"synthetic"` into enums. Values are the `src/types.ts` sets (NSDL/CDSL, DIRECT/GROWTH/FOLIO, 24K, CUMULATIVE, NPS I/CAMS, SELF, AIF II, LUMPSUM, FLOATING). NSDL and CDSL stay on two equity rows. `linkedHoldingId` stays on a residential property pointing at a loan. Validation was not loosened.
- Parser libraries remain inside `assets/chaquopy/*.imy`, not `lib/arm64-v8a`.
- PAN stays on first-run. Consent stays `registration_consent` in `extra_json`. Licence stays one yearly `gud_annual`.
- Vault, Settings, Privacy, Licence, and Emergency export do not list another person's holdings, so they have no owner filter.
- Home hides household category mix and the other person's tiles when one person is selected. Statements list only that person.

## BLOCKERS
- Host RAM: assemble with the emulator killed. The emulator raises `-memory 2048` to 2560 MB.
- `pypdfium2` is arm64-only. x86 CAS tests stay synthetic.

## KNOWN GAPS
- Full instrumented suites were not repeated after these slices.
- 360dp / 200% / dark screenshot matrix not saved.
- ARM64 parser behaviour is DEVICE: pending owner.

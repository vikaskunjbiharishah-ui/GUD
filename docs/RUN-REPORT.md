RUN: 2 · TIME USED: 29 min · COMMITS: 2, last 3b9e8c0 (docs commit follows)
DONE (stage): P01 form+SQL EMULATOR (FirstRunIdentityShellTest, FirstRunIdentityInstrumentedTest; JVM FirstRunRegistrationTest 3/3) · P02 copy+existing rotation/recovery EMULATOR (LocalLoginShell 5/5, LocalAppPassword 7/7) · Q2 Insights+ManualTrade owner lens EMULATOR (OwnerLensShellTest) · Q3 category menu EMULATOR · same-owner ISIN EMULATOR · app 77/77 · data 81/82
PARTIAL / NOT DONE: Q1 activity credential→identity write not instrumented. Q2 other screens. Q3 create→restart→negative. Q4–Q10. Holding-detail fixture test still red.
BLOCKED-OWNER: Gmail consent, Play purchase, production signing, phone install, real CAS PDF, pen test, owner sign-off
FAILED & FIXED: five shell tests (off-screen submit, EQUITY listed twice, loan menu matched the button, price below the fold, stale loan sentence). Amounts unchanged. Duplicate ISIN was allowed; same owner is now rejected and the emulator test passed.
OPEN RISKS: Gradle beside the emulator kills adb. x86 cannot load PDFium. allOriginalHoldingDetailKeys still fails because the fixture uses illegal enums; validation was kept. ARM64 .so files for the parser are in assets/chaquopy/*.imy.
NEXT RUN STARTS AT: Q3 per-category create → restart → negative, then owner lens on the remaining screens
ARTIFACTS: evidence/2026-10-28-2052/junit-summary.txt · release-artifacts/gud-arm64-debug.apk · release-artifacts/SHA256.txt sha256 fd1be32b168df7b434dc4c9866805541de4677a68605ae1483652f20a1713d31

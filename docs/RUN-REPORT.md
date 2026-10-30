RUN: 7 · TIME USED: 55 min (04:32:51Z–05:27Z) · COMMITS: code 5f062e5; docs follow
DONE (stage): P22/P23 EMULATOR stored interest and EMI, restart, wrong owner, not posted · ViewModel LOCAL 11/11 · security_preflight and schema PASS (not a pen test)
TESTS: domain 273/273 (56 classes, 05:08:23Z) · data 94/94 (487.447s) · app 97/97 (130.96s) · VaultFinanceReviewTest 2/2 · VaultFinanceReviewInstrumentedTest 1/1 · FinanceReviewInstrumentedTest 2/2 · HouseholdDashboardViewModelTest 11/11 at 04:56:39Z
PARTIAL / NOT DONE: P13/P14 remaining save-restarts · NSDL enrolled import · P27 manual price override · P16 sale screen · Compose semantics / TalkBack · credential Activity UI test
BLOCKED-OWNER: Gmail consent, Play purchase, production signing, phone install, real CAS PDF, pen test, owner sign-off
FAILED & FIXED: Test worker died with connection refused before any assertion. Cause: unset user.variant under C.UTF-8 plus a cold daemon. Type tag 71 (ASCII G) still prints on the green run and is not the failure. gud-gradle.sh now sets variant=POSIX, country=US, preferIPv4Stack, and reuses the daemon. Vault test had put nextEmiDate in detailsJson; the vault stores it as instrument metadata. No test was weakened.
OPEN RISKS: Do not assemble beside the emulator. Do not pass --no-daemon. x86 cannot load PDFium. Release APK is the test keystore, not an upload key. TalkBack was not run.
NEXT RUN STARTS AT: P13/P14 save-restart for DIVIDEND, INTEREST, BONUS, REPAYMENT, SPLIT, CORRECTION, OPENING_BALANCE
ARTIFACTS: debug sha256 0f6cbb0944f168831758d71e711703dac17ee52094f6c000e5c0bd4aea2a38a8 · release sha256 17ea5386636a58b1c1a99d357be202050025cde755ba12d07d5c532edd92999e · both commit 5f062e5 · parser .so sizes unchanged

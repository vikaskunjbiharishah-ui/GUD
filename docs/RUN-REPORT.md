RUN: 12 · TIME USED: 2026-11-05 · COMMITS: code 91cb2e8; docs follow
DONE (stage): LOCAL household copy. Home has one next step. Chips match the tiles. Dates are a picker. Questions are questions. Hindi welcome ends with शुरू करें. Interest and EMI say they are not in the book until save.
TESTS: compileDebugKotlin, unit-test Kotlin, and androidTest Kotlin BUILD SUCCESSFUL in 31s. OnboardingTest 2/2, RemainingLibraryTest 2/2, VaultViewModelTest 20/20, BackupFolderViewModelTest 2/2 at 02:23:12Z. Instrumented tests were not executed. Full 284/99/112 suites were not re-run.
PARTIAL / NOT DONE: emulator tap-through of the new copy · ARM64 rebuild · NSE/BSE live fetch · Insights vault screen · books health on Home · TalkBack
BLOCKED-OWNER: own CAS PDF, Gmail consent, ₹99 purchase, production signing, phone install, pen test, owner sign-off
FAILED & FIXED: androidTest compile missed HouseholdDashboard.byMember. The three constructors now pass emptyList(). LegacyBackupScreen lost its padding import and was restored. No test was deleted.
OPEN RISKS: Do not assemble beside the emulator. Digests in SHA256.txt are still 3fe140c. Sale still says it does not write a ledger event. Licence sentences still say vault. Do not publish.
NEXT RUN STARTS AT: emulator Compose tests for Home, first-run, interest, and legacy restore
ARTIFACTS: no new APK · previous debug sha256 d5de1b9de581819223acdb3c828ada0725fbc674148524373d2716c179476a70 · previous release sha256 f0eb55e04131df9046e85dd5d40fba889a542ba5de52bda25e9d8e316229aa2d · commit 91cb2e8

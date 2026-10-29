RUN: 6 · TIME USED: 51 min (22:25:42Z–23:16Z) · COMMITS: code 407bcf5; docs follow
DONE (stage): P21 EMULATOR FY 2024-25 gains with unknown tax · P33 EMULATOR slide scrolls, last sentence visible · P22/P23 EMULATOR review screens, not posted · Q8 already covered by MailboxSyncSchedulerInstrumentedTest
TESTS: domain 271/271 (55 classes, 23:09Z) · app 97/97 (126.883s) · data 93/93 (475.981s) at 23:08:29Z · FyTaxEstimateTest inside the 271 · HouseholdDashboardViewModelTest not observed
PARTIAL / NOT DONE: interest and EMI Home routes pass a null plan · P13/P14 remaining save-restarts · NSDL · manual price override · TalkBack · credential Activity UI test · P16 sale screen
BLOCKED-OWNER: Gmail consent, Play purchase, production signing, phone install, real CAS PDF, pen test, owner sign-off
FAILED & FIXED: Tax instrumented test missed assertIsDisplayed. First 200% shots were taken before the slide reached the end; the recapture shows the last sentence. EMI test matched three identical notes; the assertion now counts them. No test was weakened.
OPEN RISKS: Gradle beside the emulator kills the host. The unit-test worker dies on type tag 71. x86 cannot load PDFium. Home interest/EMI do not read the vault. security_preflight.py was not re-run. Release APK is the test keystore, not an upload key.
NEXT RUN STARTS AT: load interest and EMI from the open vault, then P13/P14 save-restarts
ARTIFACTS: evidence/2026-10-29-2247/ · debug sha256 b2ce9de727cbec82caaa0353d10dc3b19df0117598abaa407ff024ecbb5cb2ab · release sha256 3b2cdd29e68322c3837f94a02c341a859b9078739b4fa2f248ba469edaa37a38 · both commit 407bcf5

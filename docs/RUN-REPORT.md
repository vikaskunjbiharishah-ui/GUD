RUN: 1 · TIME USED: 20 min · COMMITS: 6, last 2253375 (docs commit follows)
DONE (stage): Q0 baseline LOCAL (domain 253/253, app 110/110, scripts 35/35, schema PASS) · P02 tripwire SOURCE · P25 home amounts + shell wording EMULATOR NativeShellTest 16/16
PARTIAL / NOT DONE: Q1–Q10. Full instrumented suite not run.
BLOCKED-OWNER: Gmail consent, Play purchase, production signing, phone install, real CAS PDF, pen test, owner sign-off
FAILED & FIXED: security preflight ignored a removed dashboard binding because the floor was 5 and the code has 6. NativeShellTest expected old "4.5000 INR" / "Family breakdown" copy; assertions now match the rupee book text. Numbers were not changed.
OPEN RISKS: emulator is killed if Gradle runs beside it. x86 cannot load PDFium. ARM64 .so files sit in the Chaquopy archive, not lib/arm64-v8a/.
NEXT RUN STARTS AT: Q1 first-run register (name, DOB, security questions still on Family administration)
ARTIFACTS: evidence/2026-10-28-1140/junit-summary.txt · release-artifacts/gud-arm64-debug.apk · release-artifacts/SHA256.txt · docs/PHONE-TEST.md

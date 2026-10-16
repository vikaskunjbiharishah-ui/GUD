# GrowUrDhan — Mandatory Repository Operating Instructions (ChatGPT / GUD main)

> **READ THIS BEFORE EVERY USER PROMPT, MILESTONE, DIAGNOSIS, REPOSITORY WRITE, CODE REVIEW, AND CI ACTION.** This file is committed project context, not optional prose or a substitute for verified tests.

## Authority and adaptation

- **Active implementation repository:** https://github.com/vikaskunjbiharishah-ui/GUD ; **owner-authorized working branch: `main` only**. Confirm remote/main/HEAD from GitHub on every session. The owner explicitly requested this repository to adopt the same methodology as Grok.
- **Full common requirements:** [docs/GROK-FULL-NATIVE-REDEVELOPMENT-SPEC.md](docs/GROK-FULL-NATIVE-REDEVELOPMENT-SPEC.md), **unaltered v1.2**. Treat **G01–G15, P01–P39, E01–E18, Section 6A, and Section 17** as binding where applicable, subject only to the specific branch/repository adaptation below. Read this full document again at the start of **each new user prompt** and relevant sections before every task; do not trust chat memory.
- **Critical G01/G08 adaptation:** The Grok specification describes a *different AI* starting from the frozen old application on its own branch and explicitly forbids Grok from changing this repository. **That prohibition is directed to Grok, not to the owner-authorized ChatGPT work on this GUD mainline.** Here, continue the pre-existing Kotlin/Compose/SQLCipher code on `main` and never reset, rebootstrap, clone/merge Grok's branch, or rewrite the project's history. Current work is **not M00 fresh-code migration**.
- **Immutable reference:** `shahvikask/kosha` `GUD-Android-Gmail@af96dcc8967ab595e1d9acee78e29e500cb99a9e`; old React/Tauri sources retained read-only; `src/casparser-main/**` and original parser invocation scripts/dependency pins frozen; preserve the approved logo artwork at `public/brand/`.
- Record this G01 adaptation as the explicitly owner-authorized decision **D-001** in [DECISION-LOG.md](docs/implementation/DECISION-LOG.md). If later instructions conflict with safety, data integrity, commercial release rules or authorization, ask for a clear owner choice and log it.

## Required session startup — G15 Section 17.3

1. **Re-open and actually read** this `AGENTS.md`, the complete specification v1.2 (applicable sections and Section 6A for any UI work), [STATUS.md](docs/implementation/STATUS.md), [SESSION-HANDOFF.md](docs/implementation/SESSION-HANDOFF.md), [CI-RUNS.md](docs/implementation/CI-RUNS.md), [DECISION-LOG.md](docs/implementation/DECISION-LOG.md), [parity-register.csv](docs/implementation/parity-register.csv), applicable issues and architectural checkpoints. If absent, say **NOT ACCESSIBLE**. Do not report READ for files not opened.
2. Verify GitHub repository, `main` current 40-character SHA and most recent **tested source SHA**, issue status and required checks. File-only GitHub access cannot establish a local working-tree state: use **UNVERIFIED**, not CLEAN.
3. Print the **STARTUP COMPLIANCE RECEIPT** specified in Section 17.3 **before** changing source or diagnosing CI. Identify the exact bounded task and whether expensive Actions runs were authorized.
4. Follow issue dependencies and check all requirement IDs (original P01–P39, enterprise E01–E18), including deviations and test coverage. Existing implementation ≠ acceptance.

## Hard development, quality and security invariants

- **True native Kotlin + Jetpack Compose + Material 3, SQLCipher and Keystore**. Frozen Chaquopy CASPARSER; never replace with edited engine or WebView UI. Domain/test/secure data-layer separation.
- **No user data cloud backend.** Money/quantity scaled integers; append-only ledger with corrections; owner isolation; atomic/idempotent imports; exact fiscal/financial golden tests; unknown cost and stale prices transparently disclosed. Never fabricate history, security approval, Gmail OAuth or Play purchase validation.
- **Heritage Modern Wealth UI** (spec §6A): exact original navy/gold/teal logo assets, light/dark bespoke tokens, premium functional design, adaptive Compose, 200% font scaling, 48dp targets, TalkBack, real data states; screenshot G14 and owner visual signoff are gates. No copied competitor UI.
- For each milestone: source audit → full checklist → implement → local static/lint/tests → diff/AI review → integrated verification → coherent commit → **owner-approved** exact-SHA GitHub Actions → monitor terminal jobs/artifacts → fix → retest new SHA. **Never call older CI green for newer code or loosen assertions to make a build pass.**
- **Do not auto-trigger or dispatch paid CI on documentation or ordinary source commits**. The repository already has native workflows; see [WORKFLOW-GATES.md](docs/implementation/WORKFLOW-GATES.md). Preserve the owner's explicit-build budget preference, exact-SHA pinning, and protected Play signing. Never represent proposed G00–G14 names as implemented actions unless files and actual run evidence exist.
- No actual PII, PDFs, PAN, passwords, access tokens, live secrets, unmasked portfolios or private keys in Git/CI/AI prompts. Real ARM64 phones, independent MASVS AppSec, signing, OAuth/Play legal approvals remain separately evidenced; physical-device acceptance is owner-deferred to **M34**.

## Required session end — G15 Section 17.4

- Commit accurate changes to `STATUS.md`, `SESSION-HANDOFF.md`, `CI-RUNS.md` as applicable and approved entries to `DECISION-LOG.md`. Preserve historical failed CI runs.
- Return the full **21-field A–U delivery report**, stating exact latest GitHub SHA, paths/commits, verified test commands, run IDs and **tested SHA**, remaining deficits and stage-specific acceptance. `NOT RUN`, `PENDING`, and `UNVERIFIED` are valid truthful results.
- **Last field U is a ready-to-paste NEXT PROMPT** that explicitly begins with “Re-read AGENTS.md…” and requires fresh repository checks and updated handoff. Every subsequent user prompt must rehydrate instructions again, even when it merely says “continue.”

## Current live state pointer

Read [SESSION-HANDOFF.md](docs/implementation/SESSION-HANDOFF.md) for the newest checkpoint and **resolve live HEAD**, rather than trusting a previously embedded SHA. As of this documentation initialization, M24 is scoped-closed, M25/M26 await a clean combined regression, and M27 has substantial source but open scope/build parity. **M27 issue #5 must stay OPEN until its specific requirements and exact-SHA tests pass.** Physical phone validation belongs to M34, not a fake emulator checkbox.

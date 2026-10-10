# Isolated GUD/main consolidated Portfolio + Insights milestone

**Date:** 10 October 2026  
**Delivery model:** staging multiple code/test files in Git trees, then **one**
final commit to `vikaskunjbiharishah-ui/GUD/main`. No writes to old repositories.

## E0 — Source and route inventory

Existing native code already implements vault session checks, an atomic
`readBook()`, enrolled family/registrar identity mapping, manual reviewed
CAS import (ARM64 release packaging still device-unverified), portable
encrypted backups, Home household totals and a searchable 30-at-a-time
recorded holdings list. It does **not** implement native app-password parity,
NPS registration, Gmail OAuth, official-price refresh or Play billing. The
dated E0 matrix remains in `20-ENTERPRISE-FEATURE-PARITY-MATRIX.md`.

## E1 — Native navigation and privacy

Main navigation: Home, Portfolio, Statements, Insights, Settings. Vault and
Privacy remain secondary routes reachable from Home and Settings, preserving
existing secure enrolment and backup workflows. Locked Portfolio and Insights
pages show no names, balances, identities or charts. Filters live only in
ephemeral Compose state and are dropped with the authorized page.

## E2 — Real read-only portfolio and insights slice

Portfolio filters the already-authorized `HouseholdDashboard` data by
member nickname and asset category. Search and progressive display work on
the filtered rows. No independent SQLite query, insecure cache, or network
request has been introduced.

Insights projects that same snapshot into family allocation and debt-to-assets
percentages, using BigInteger for scaled-INR ratios. It checks household
assets-minus-liabilities, member totals, category sums, recorded holdings
and warning status. Inconsistency **withholds percentages**, avoiding false
precision. Zero assets never produces a fabricated ratio. Historical cost
gaps remain explicitly unresolved; no returns or tax projections are claimed.

## Regression coverage added

- Eight JUnit5 domain cases: allocation rounding, warning and mismatch
  suppression, debt-only books, maximum signed-64-bit assets, negative values,
  real financial-engine integration, inconsistent member totals and unknown cost.
- Five new Compose screen cases: local category filtering, vault-lock clearance,
  valid allocation/debt ratio, suppressed unreliable percentages and locked
  insights. Existing navigation coverage gains a sixth Compose assertion for
  the new locked destinations.
- Four Python tests prevent accidental restoration of per-commit build
  triggers or unprotected signing; `native-android.yml` discovers all tests
  with `test_*.py`.
- Historical native schema, repository authorization, CAS and recovery
  regressions remain unchanged.

## Verification gates

**Executed before commit:** isolated `kotlinc` compilation and synthetic
smoke cases for `PortfolioInsightsProjector`; GitHub-side inspection of staged
files and CI triggers. This does not constitute a full Android build.

**Not yet executed:** complete domain/app Gradle tests, connected Compose and
SQLCipher instrumentation, API 26/30/36 matrix, ARM64 parser physical tests,
security penetration testing, Play Billing/licensing, or signed release AAB.
The next dedicated build/regression turn must execute those available
GitHub-hosted tests and address any actual failures before additional
development starts.

**Remaining E2/E3/E4 commercial blockers:** app-specific login compatibility,
native ledger transaction editing, full CAS/NPS identity breadth and migration,
multi-account Gmail OAuth consent, official-source EOD prices, store licences
and policy acceptance, device-loss recovery testing, accessibility/branding
signoff, independent mobile AppSec and release approval. These are not
represented as complete.

> **Working repository:** https://github.com/vikaskunjbiharishah-ui/GUD.git — branch `main` only. `shahvikask/kosha` is the frozen original reference, not the repository to commit to.

# M26 P0 — Native family, account, nomination and goal administration (as-built source)

**Date:** 10 October 2026  
**Repository:** `vikaskunjbiharishah-ui/GUD`  
**Branch:** `main` only  
**Status:** SOURCE IMPLEMENTED FOR SCOPED ADMINISTRATION; AUTOMATED/DEVICE REGRESSION **PENDING**, full issue acceptance **OPEN**. M25 and M26 are to be tested in one combined, deliberately triggered GitHub Actions build.

## Scope of the implementation

The pre-existing encrypted family setup screen still registers the primary Self and additional Spouse/Parent/Child/HUF/Joint members. A new `FamilyAdministrationScreen` reached from Home or the family setup page provides a family-scoped member/owner selector and an accounts/goals view. A member can be edited with relationship, optional DOB, a registered guardian for a minor child, color, notes, and a consent-entered PAN. PAN is persisted inside SQLCipher; the administration projection returns a masked value only. Clearing PAN is explicit.

Family deletion is **reversible deactivation**, not SQL DELETE. The original primary owner cannot be deactivated. Any member with a custody account, holding, event, goal, statement snapshot or guardian dependency cannot be deactivated; a denied operation changes nothing. Inactive member identities remain in the encrypted vault for historical consistency.

Account administration permits creating and editing an owner-scoped financial account with a nickname, institution/registrar, nominee, DP/client/BO identifiers, account opening date and close date. Sensitive IDs are masked in the read UI. Blank input preserves the existing encrypted sensitive identifiers/nominee; separate explicit clear controls remove them. Account closure changes `custody_profile.closed_on` and retains all holdings, immutable events and CAS identities. Subsequent new CAS holding enrollment into a closed account is rejected. Cross-member account ownership reassignment is explicitly forbidden. Account search uses nickname, type, custodian and **masked** identifiers only.

A member-specific or household-wide goal records a name, nullable scaled-INR target, due date and priority. Existing goals can be edited or removed, and holdings tagged to multiple goals. Owner-scoped goals accept only matching owner's holdings; household-wide goals can tag multiple members' holdings. Tagged exposure is calculated from already-stored scaled quantities and unit prices, without online price requests or fabricated BUY events. Removing a goal clears only its tags and goal row, not the holdings, positions, tax data or ledger.

The Home screen now shows a read-only **individual-owner lens**, clearly separate from the whole-family totals. Existing Portfolio/Statements screens retain their own family filters. A broader consistent global selector across tax and document workflows awaits those screens' later milestones.

## Authorization and data guarantees

- Public repository methods `familyAdministration()` and `applyFamilyAdministration(change)` are guarded in the production `DefaultVaultRepository`; the former uses the live session-bound read, and the latter uses `approvedWrite` with captured device credential revision.
- `FamilyAdministrationViewModel` stages an ephemeral command before launching a NEW Android credential challenge, and checks the original monotonic session revision on completion; lock/background clears the state.
- `FamilyAdministrationSql.apply` executes under the existing single SQLCipher `mutate` transaction. FKs, owner checks, name constraints, age/guardian checks and goal ownership are revalidated in the transaction, not trusted from UI state.
- No new schema migration required: the native v4 `family_member`, `family_profile`, `custody_account`, `custody_profile`, `family_goal` and `holding_goal` tables were already included in the v4 portable encrypted backup whitelist.
- Original ledger_event is neither updated nor deleted by any new M26 operation; no cascading financial records are removed.
- Every new sensitive business input is kept in on-device UI/SQLCipher only; no network calls or telemetry.

## Test fixtures and open acceptance work

`FamilyAdministrationInstrumentedTest` uses synthetic two-member and HUF account/book fixtures to check ownership, masked identifiers, institution/nominee, scoped goals/exposure, cross-owner refusal, closed accounts, historical ledger stability, reversible deactivation, minor guardian rule, and v4 portable-backup roundtrip.

`test_security_preflight.py` now checks that an unclassified/unapproved family write and unauthenticated family read are rejected by static policy. The existing M25 v2 `.kosha` restore synthetic suite is not modified and must also run during the combined regression.

**Not yet accepted:** corrected M25 restore path (last `ce63c25` bug fix is untested), M26 Kotlin compilation and Android instrumentation, shared co-ownership beyond a single accountable household member (the current schema has one owner_id per custody account), permanent member deletion (deliberately disallowed to preserve history), portfolio-wide tax/document selector parity, accessibility/device form verification and a physical fresh-device migration roundtrip.

## Next release gate

Run **ONE manually approved combined M25 + M26 exact-`main`-SHA regression**, after source review, using the existing `.github/ci-trigger/native-regression.txt` file or manual `workflow_dispatch`. Avoid CI on every ordinary source commit. Keep M25 #3 and M26 #4 **OPEN** until the actual combined CI result and remaining checklist are satisfied. M24 #2 remains closed for its scoped foundation tests.

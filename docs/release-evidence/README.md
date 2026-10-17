> **Working repository:** https://github.com/vikaskunjbiharishah-ui/GUD.git — branch `main` only. `shahvikask/kosha` is the frozen original reference, not the repository to commit to.

# GrowUrDhan release acceptance evidence

This directory is intentionally absent of approved release artifacts.

A production-signed Play-upload AAB requires
`release-approval.json` referring to the *exact* source commit SHA,
with `format: 1`, `approved_commit`, and a `gates` map. Every required
gate is listed in `android-native/scripts/release_gate.py`. Each gate
must contain:
- `accepted: true` only after actual owner/reviewer acceptance
- `reviewer`: reviewer identifier or organization
- `evidence_file`: a real, sanitized file under `docs/release-evidence/`
- `sha256`: SHA-256 digest of that exact file

Never store passwords, production keys, private financial PDFs, a real PAN,
Google OAuth refresh tokens or confidential third-party assessment details
that are not authorized for this private repository.

The script only detects absent or tampered evidence files. It cannot
authenticate a reviewer or assess the substantive findings; GitHub
`play-release` environment approval and independent human review remain
mandatory. The signed AAB workflow does not publish to Google Play.

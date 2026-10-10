#!/usr/bin/env python3
"""Owner release-evidence policy gate, NOT an AppSec certification.

Signed Play upload artifacts are refused until all explicit acceptance
documents exist, match SHA-256, and name the exact approved source commit.
No acceptance manifest is checked in during ordinary development.
"""
from __future__ import annotations

import hashlib
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
REQUIRED = (
    "native_app_password_parity",
    "legacy_data_and_backup_migration",
    "manual_cas_nps_device_parity",
    "gmail_oauth_verification",
    "official_prices_and_staleness",
    "play_billing_verification",
    "legal_terms_and_privacy",
    "cross_device_recovery",
    "device_accessibility_matrix",
    "independent_mobile_security_assessment",
    "production_signing_approval",
)


def validate_manifest(manifest: dict, source_sha: str, root: pathlib.Path) -> list[str]:
    problems: list[str] = []
    if not re.fullmatch(r"[a-f0-9]{40}", source_sha):
        return ["Commit SHA must have exactly 40 lowercase hexadecimal characters."]
    if manifest.get("format") != 1 or manifest.get("approved_commit") != source_sha:
        problems.append("Release evidence is not bound to the exact approved commit.")
    items = manifest.get("gates")
    if not isinstance(items, dict):
        return problems + ["Release acceptance gates are missing."]
    for name in REQUIRED:
        item = items.get(name)
        if not isinstance(item, dict) or item.get("accepted") is not True:
            problems.append(name + ": acceptance evidence is missing.")
            continue
        if not isinstance(item.get("reviewer"), str) or not item["reviewer"].strip():
            problems.append(name + ": owner/reviewer approval is missing.")
        relative = item.get("evidence_file")
        expected_hash = item.get("sha256")
        if not isinstance(relative, str) or not relative.startswith(
            "docs/release-evidence/"
        ) or ".." in pathlib.PurePosixPath(relative).parts:
            problems.append(name + ": evidence must be a local release-evidence file.")
            continue
        if not isinstance(expected_hash, str) or not re.fullmatch(
            "[a-f0-9]{64}", expected_hash
        ):
            problems.append(name + ": evidence SHA-256 is invalid.")
            continue
        file = root / relative
        if file.is_symlink() or not file.is_file():
            problems.append(name + ": evidence file is not present.")
            continue
        actual_hash = hashlib.sha256(file.read_bytes()).hexdigest()
        if actual_hash != expected_hash:
            problems.append(name + ": evidence content does not match its checksum.")
    return problems


def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: release_gate.py <exact-approved-commit-sha>", file=sys.stderr)
        return 2
    path = ROOT / "docs/release-evidence/release-approval.json"
    if not path.is_file():
        print("FAIL: owner-reviewed release-approval.json is not present.", file=sys.stderr)
        return 1
    try:
        document = json.loads(path.read_text(encoding="utf-8"))
        failures = validate_manifest(document, sys.argv[1], ROOT)
    except (ValueError, OSError) as error:
        print("FAIL: unreadable release evidence manifest.", file=sys.stderr)
        return 1
    for failure in failures:
        print("FAIL: " + failure, file=sys.stderr)
    if failures:
        return 1
    print("PASS: owner-accepted release evidence files match approved source SHA.")
    print("NOTE: this confirms evidence integrity, not the substance of security audits.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

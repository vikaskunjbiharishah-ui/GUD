#!/usr/bin/env python3
"""The frozen CAS parser tree must match docs/frozen-hashes.txt. No parser edits."""
import hashlib
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RECORD = ROOT / "docs" / "frozen-hashes.txt"
TREE = ROOT / "src" / "casparser-main"


def current_lines() -> list[str]:
    files = sorted(
        path for path in TREE.rglob("*")
        if path.is_file() and "__pycache__" not in path.parts
    )
    lines = []
    for path in files:
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        lines.append(f"{digest}  {path.relative_to(ROOT).as_posix()}")
    return lines


class FrozenParserHashTests(unittest.TestCase):
    def test_recorded_hashes_match_the_tree(self):
        recorded = [line for line in RECORD.read_text().splitlines() if line.strip()]
        self.assertEqual(current_lines(), recorded)


if __name__ == "__main__":
    unittest.main()

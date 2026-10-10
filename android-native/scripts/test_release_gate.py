#!/usr/bin/env python3
import hashlib
import pathlib
import tempfile
import unittest

from release_gate import REQUIRED, validate_manifest


class ReleaseGateTests(unittest.TestCase):
    SHA = "a" * 40

    def fixture(self, root):
        relative = "docs/release-evidence/synthetic-proof.txt"
        path = root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text("synthetic release gate unit-test proof", encoding="utf-8")
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        return {
            "format": 1, "approved_commit": self.SHA,
            "gates": {key: {
                "accepted": True, "reviewer": "test-fixture-only",
                "evidence_file": relative, "sha256": digest,
            } for key in REQUIRED},
        }

    def test_evidence_gate_requires_all_categories_and_exact_commit(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            document = self.fixture(root)
            self.assertEqual([], validate_manifest(document, self.SHA, root))
            self.assertTrue(validate_manifest(document, "b" * 40, root))
            del document["gates"]["independent_mobile_security_assessment"]
            self.assertTrue(any("independent_mobile_security_assessment" in issue
                                for issue in validate_manifest(document, self.SHA, root)))

    def test_tampered_missing_or_traversal_evidence_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            document = self.fixture(root)
            path = root / "docs/release-evidence/synthetic-proof.txt"
            path.write_text("tampered evidence", encoding="utf-8")
            self.assertTrue(validate_manifest(document, self.SHA, root))
            path.unlink()
            self.assertTrue(validate_manifest(document, self.SHA, root))
            document["gates"]["production_signing_approval"]["evidence_file"] = "../escape"
            self.assertTrue(validate_manifest(document, self.SHA, root))

    def test_unverified_or_blank_reviewer_never_counts_as_accepted(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            document = self.fixture(root)
            document["gates"]["play_billing_verification"]["accepted"] = False
            document["gates"]["legal_terms_and_privacy"]["reviewer"] = ""
            errors = validate_manifest(document, self.SHA, root)
            self.assertTrue(any("play_billing_verification" in e for e in errors))
            self.assertTrue(any("legal_terms_and_privacy" in e for e in errors))


if __name__ == "__main__":
    unittest.main()

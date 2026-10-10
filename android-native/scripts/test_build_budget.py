#!/usr/bin/env python3
"""Credit-preservation checks for the isolated GUD/main repository.

Run with: python3 -m unittest discover -s android-native/scripts -p 'test_*.py'
No dependency on PyYAML or other network-installed software.
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]
WORKFLOWS = ROOT / ".github/workflows"


class BuildCreditPolicyTests(unittest.TestCase):
    def text(self, name):
        return (WORKFLOWS / name).read_text(encoding="utf-8")

    def test_native_workflow_only_pushes_for_explicit_regression_marker(self):
        body = self.text("native-android.yml")
        header = body.split("permissions:", 1)[0]
        self.assertIn("workflow_dispatch:", header)
        self.assertIn("branches: [main]", header)
        self.assertIn("'.github/ci-trigger/native-regression.txt'", header)
        self.assertNotIn("'android-native/**'", header)
        self.assertNotIn("'.github/workflows/native-android.yml'", header)
        self.assertIn("python3 -m unittest discover", body)
        self.assertIn("-p 'test_*.py'", body)

    def test_emulator_matrix_cannot_run_on_push(self):
        header = self.text("native-device-matrix.yml").split("permissions:", 1)[0]
        self.assertIn("workflow_dispatch:", header)
        self.assertNotRegex(header, r"(?m)^  push:")
        self.assertNotRegex(header, r"(?m)^  pull_request:")

    def test_legacy_typescript_ci_is_manual_only(self):
        header = self.text("ci.yml").split("jobs:", 1)[0]
        self.assertIn("workflow_dispatch:", header)
        self.assertNotRegex(header, r"(?m)^  push:")
        self.assertNotRegex(header, r"(?m)^  pull_request:")

    def test_play_signing_stays_manual_and_requires_owner_approval(self):
        body = self.text("native-signed-aab.yml")
        header = body.split("permissions:", 1)[0]
        self.assertIn("workflow_dispatch:", header)
        self.assertNotRegex(header, r"(?m)^  push:")
        self.assertIn("approved_commit_sha:", header)
        self.assertIn("APPROVED_COMMIT_SHA", body)


if __name__ == "__main__":
    unittest.main()

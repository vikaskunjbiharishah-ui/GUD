#!/usr/bin/env python3
"""M24 source-frozen React v9 -> Kotlin/SQLCipher contract tests.

Run: python3 -m unittest discover -s android-native/scripts -p 'test_*.py'
This is a source comparison; it does NOT assert operational legacy backup import.
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]
TS = (ROOT / "src/types.ts").read_text(encoding="utf-8")
OLD_SCHEMA = (ROOT / "src/db/schema.ts").read_text(encoding="utf-8")
MODEL = (ROOT / "android-native/core/model/src/main/kotlin/in/growurdhan/core/model/ReactV9ParityContract.kt").read_text(encoding="utf-8")
NEW_SCHEMA = (ROOT / "android-native/data/src/main/kotlin/in/growurdhan/data/vault/VaultSchema.kt").read_text(encoding="utf-8")
BACKUP = (ROOT / "android-native/data/src/main/kotlin/in/growurdhan/data/vault/VaultBackupTables.kt").read_text(encoding="utf-8")

def ts_union(name: str) -> set[str]:
    return set(re.findall(r"'([A-Z_]+)'",
        TS.split(f"export type {name} =", 1)[1].split(";", 1)[0]))

def kotlin_enum(name: str) -> set[str]:
    body = re.search(r"enum class " + name + r"\s*\{([^}]+)\}", MODEL)
    if not body:
        raise AssertionError(f"Missing Kotlin enum {name}")
    return set(re.findall(r"\b[A-Z][A-Z_]+\b", body.group(1)))

def field_keys(text: str, typename: str) -> set[str]:
    block = text.split("export interface " + typename + " {", 1)[1].split("\n}", 1)[0]
    return set(re.findall(r"^\s*([A-Za-z][A-Za-z0-9]*)\??:", block, re.M))

class ReactV9ParityContractTests(unittest.TestCase):
    def test_reference_is_frozen_v9(self) -> None:
        self.assertRegex(OLD_SCHEMA, r"SCHEMA_VERSION\s*=\s*9")
        self.assertIn("SOURCE_SCHEMA_VERSION = 9", MODEL)

    def test_all_eight_legacy_categories_are_present(self) -> None:
        self.assertEqual(ts_union("AssetCategory"), kotlin_enum("ReactV9AssetCategory"))
        self.assertEqual(8, len(kotlin_enum("ReactV9AssetCategory")))

    def test_all_eleven_legacy_ledger_kinds_are_present(self) -> None:
        self.assertEqual(ts_union("EventType"), kotlin_enum("ReactV9LedgerEventType"))
        self.assertEqual(11, len(kotlin_enum("ReactV9LedgerEventType")))
        self.assertIn("deny_ledger_update", NEW_SCHEMA)
        self.assertIn("deny_ledger_delete", NEW_SCHEMA)

    def test_every_original_asset_specific_field_is_preserved_by_contract(self) -> None:
        old = field_keys(TS, "HoldingDetails")
        captured = MODEL.split("val fields: Set<String> = setOf(", 1)[1].split("\n    )", 1)[0]
        mapped = set(re.findall(r'"([A-Za-z][A-Za-z0-9]*)"', captured))
        self.assertEqual(old, mapped)
        self.assertEqual(36, len(old))
        self.assertIn("details_json", NEW_SCHEMA)
        self.assertIn("legacy_field_extension", NEW_SCHEMA)

    def test_all_extension_tables_are_encrypted_and_backed_up(self) -> None:
        tables = [
            "family_profile", "custody_profile", "instrument_profile", "holding_profile",
            "ledger_profile", "family_goal", "holding_goal", "import_batch_profile",
            "source_document_profile", "statement_snapshot_profile", "statement_position_profile",
            "price_observation_profile", "tax_rule_profile", "liability_profile",
            "legacy_account_identity", "legacy_field_extension", "setting_profile",
            "listed_security_profile", "egress_log_profile", "gmail_seen_profile",
        ]
        for table in tables:
            with self.subTest(table=table):
                self.assertIn(f"CREATE TABLE {table} (", NEW_SCHEMA)
                self.assertIn(f'"{table}" to listOf(', BACKUP)
        self.assertIn('.put("format", 4)', BACKUP)
        self.assertIn('(format == 3 && databaseVersion == 3)', BACKUP)
        self.assertIn('if (format < 4 && table in parityTables) continue', BACKUP)

    def test_native_field_storage_uses_integer_money_and_unknown_cost(self) -> None:
        for col in ["average_cost_per_unit_scaled INTEGER", "target_scaled INTEGER",
                    "principal_scaled INTEGER", "invested_scaled INTEGER"]:
            self.assertIn(col, NEW_SCHEMA)
        self.assertIn("cost_unknown INTEGER NOT NULL DEFAULT 1", NEW_SCHEMA)
        self.assertNotRegex(NEW_SCHEMA, r"\bREAL\b")

if __name__ == "__main__":
    unittest.main()

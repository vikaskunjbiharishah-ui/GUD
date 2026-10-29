#!/usr/bin/env python3
"""Compile native SQL and test v1/v2/v3/v4->v5, fresh installs and rollback offline.

The SQL statements are extracted from VaultSchema.kt itself (no duplicated
schema). This preflight tests ordinary SQLite constraints and atomic migration.
SQLCipher encryption, Android Keystore and emulator behavior are tested by
the separate Android instrumentation suite.
"""
from __future__ import annotations

import json
from pathlib import Path
import re
import sqlite3

SCHEMA_FILE = (
    Path(__file__).resolve().parents[1]
    / "data/src/main/kotlin/in/growurdhan/data/vault/VaultSchema.kt"
)
EXPECTED_V2_TABLES = {
    "family_member", "custody_account", "holding", "ledger_event",
    "snapshot_import", "statement_position", "vault_meta",
}
EXPECTED_V3_TABLES = EXPECTED_V2_TABLES | {"holding_cas_identity"}
PARITY_TABLES = {
    "family_profile", "custody_profile", "instrument_profile", "holding_profile",
    "ledger_profile", "family_goal", "holding_goal", "import_batch_profile",
    "source_document_profile", "statement_snapshot_profile", "statement_position_profile",
    "price_observation_profile", "tax_rule_profile", "liability_profile",
    "legacy_account_identity", "legacy_field_extension", "setting_profile",
    "listed_security_profile", "egress_log_profile", "gmail_seen_profile",
}
EXPECTED_TABLES = EXPECTED_V3_TABLES | PARITY_TABLES
V1_STATEMENT_COUNT = 14
MIGRATION_STATEMENT_COUNT = 6
V3_MIGRATION_STATEMENT_COUNT = 4
V4_MIGRATION_STATEMENT_COUNT = 26
V5_MIGRATION_STATEMENT_COUNT = 5
RICH_COLUMNS = {
    "financial_kind", "unit_price_scaled", "charges_scaled", "supersedes_event_id",
}


def statements_from_kotlin(source: str, name: str, expected: int) -> list[str]:
    anchor = f"{name} = listOf("
    if source.count(anchor) != 1:
        raise AssertionError(f"Cannot uniquely locate {name} in VaultSchema.kt")
    body = source.split(anchor, 1)[1].split("\n    )", 1)[0]
    literals = re.compile(
        r'"""(.*?)"""(?:\.trimIndent\(\))?|("(?:\\.|[^"\\])*")',
        flags=re.DOTALL,
    )
    statements = [
        (
            match.group(1)
            if match.group(1) is not None
            else json.loads(match.group(2))
        ).strip()
        for match in literals.finditer(body)
    ]
    if len(statements) != expected:
        raise AssertionError(f"{name}: expected {expected} statements, got {len(statements)}")
    return statements


def run_statements(db: sqlite3.Connection, statements: list[str], tag: str) -> None:
    for i, statement in enumerate(statements, 1):
        try:
            db.execute(statement)
        except sqlite3.DatabaseError as exc:
            raise AssertionError(
                f"{tag}: statement {i}/{len(statements)} failed: {statement[:180]!r}"
            ) from exc


def assert_rejected(db: sqlite3.Connection, sql: str) -> None:
    try:
        db.execute(sql)
    except sqlite3.IntegrityError:
        return
    raise AssertionError(f"Constraint should reject: {sql[:150]}")


def prepare_v1(db: sqlite3.Connection, statements: list[str]) -> None:
    run_statements(db, statements, "legacy v1")
    db.execute("PRAGMA user_version = 1")
    db.execute(
        "INSERT INTO family_member(id,display_name,relationship,is_primary) "
        "VALUES('self','Synthetic user','Self',1)"
    )
    db.execute(
        "INSERT INTO family_member(id,display_name,relationship,is_primary) "
        "VALUES('spouse','Synthetic spouse','Spouse',0)"
    )
    db.execute(
        "INSERT INTO holding(id,owner_id,display_name,asset_category,"
        "quantity_scaled,price_scaled) "
        "VALUES('holding','self','Synthetic asset','FUND',0,10000)"
    )
    db.execute(
        "INSERT INTO holding(id,owner_id,display_name,asset_category,"
        "quantity_scaled,price_scaled) "
        "VALUES('other','spouse','Another asset','EQUITY',0,20000)"
    )
    db.execute(
        "INSERT INTO ledger_event(id,owner_id,holding_id,kind,event_date,"
        "quantity_delta_scaled,amount_scaled) "
        "VALUES('original','self','holding','BUY','2026-01-01',100000000,10000)"
    )
    db.execute(
        "INSERT INTO snapshot_import("
        "id,owner_id,source_sha256,as_of,is_complete"
        ") VALUES('snapshot','self',?,'2026-01-01',1)",
        ("a" * 64,),
    )
    db.execute(
        "INSERT INTO statement_position("
        "snapshot_id,owner_id,holding_id,quantity_scaled,value_scaled"
        ") VALUES('snapshot','self','holding',100000000,10000)"
    )
    db.commit()


def check_v2(db: sqlite3.Connection) -> None:
    tables = {
        name for (name,) in db.execute(
            "SELECT name FROM sqlite_master WHERE type = 'table'"
        ) if not name.startswith("sqlite_")
    }
    if tables != EXPECTED_V2_TABLES:
        raise AssertionError(f"Unexpected v2 tables: {tables}")
    columns = {
        row[1] for row in db.execute("PRAGMA table_info('ledger_event')")
    }
    if not RICH_COLUMNS.issubset(columns):
        raise AssertionError(f"Missing rich financial columns: {RICH_COLUMNS - columns}")
    if db.execute("PRAGMA user_version").fetchone() != (2,):
        raise AssertionError("Native vault schema version is not 2")
    if db.execute("PRAGMA foreign_keys").fetchone() != (1,):
        raise AssertionError("Foreign keys disabled")
    assert_rejected(db, "UPDATE ledger_event SET amount_scaled = 0 WHERE id = 'original'")
    assert_rejected(db, "DELETE FROM ledger_event WHERE id = 'original'")
    if db.execute(
        "SELECT financial_kind,unit_price_scaled,charges_scaled,supersedes_event_id "
        "FROM ledger_event WHERE id='original'"
    ).fetchone() != ("", None, 0, None):
        raise AssertionError("Legacy v1 financial event was modified by migration.")

    # A rich event's actual kind survives while a v1-constrained carrier remains
    # ADJUSTMENT. No silent unsupported kind coercion is allowed.
    db.execute(
        "INSERT INTO ledger_event("
        "id,owner_id,holding_id,kind,financial_kind,event_date,"
        "quantity_delta_scaled,amount_scaled,unit_price_scaled,charges_scaled"
        ") VALUES('bonus','self','holding','ADJUSTMENT','BONUS','2026-03-01',"
        "100000000,0,0,0)"
    )
    assert_rejected(
        db, "INSERT INTO ledger_event("
        "id,owner_id,holding_id,kind,financial_kind,event_date,"
        "quantity_delta_scaled,amount_scaled) "
        "VALUES('forged','self','holding','BUY','BONUS','2026-03-02',0,0)"
    )
    assert_rejected(
        db, "INSERT INTO ledger_event("
        "id,owner_id,holding_id,kind,financial_kind,event_date,"
        "quantity_delta_scaled,amount_scaled,supersedes_event_id) "
        "VALUES('cross','spouse','other','CORRECTION','CORRECTION',"
        "'2026-04-01',100000000,10000,'original')"
    )
    db.execute(
        "INSERT INTO ledger_event("
        "id,owner_id,holding_id,kind,financial_kind,event_date,"
        "quantity_delta_scaled,amount_scaled,unit_price_scaled,supersedes_event_id"
        ") VALUES('correction','self','holding','CORRECTION','CORRECTION',"
        "'2026-04-01',100000000,12000,12000,'original')"
    )
    assert_rejected(
        db, "INSERT INTO ledger_event("
        "id,owner_id,holding_id,kind,financial_kind,event_date,"
        "quantity_delta_scaled,amount_scaled,unit_price_scaled,supersedes_event_id"
        ") VALUES('duplicate','self','holding','CORRECTION','CORRECTION',"
        "'2026-04-02',100000000,13000,13000,'original')"
    )
    if db.execute("PRAGMA integrity_check").fetchone() != ("ok",):
        raise AssertionError("SQLite integrity check failed")
    if db.execute("PRAGMA foreign_key_check").fetchall():
        raise AssertionError("Foreign key check failed")


def check_v3(db: sqlite3.Connection) -> None:
    tables = {
        name for (name,) in db.execute(
            "SELECT name FROM sqlite_master WHERE type = 'table'"
        ) if not name.startswith("sqlite_")
    }
    if tables != EXPECTED_V3_TABLES:
        raise AssertionError(f"Unexpected v3 vault tables: {tables}")
    if db.execute("PRAGMA user_version").fetchone() != (3,):
        raise AssertionError("Native vault schema version is not 3")
    if db.execute("PRAGMA foreign_keys").fetchone() != (1,):
        raise AssertionError("Foreign keys disabled")
    if db.execute("PRAGMA foreign_key_check").fetchall():
        raise AssertionError("CAS mapping foreign keys invalid")

    # Enrolment must be exact, scoped and immutable by SQL constraints;
    # no SQLite BLOB, PAN or user-supplied SQL is used for these tests.
    db.execute(
        "INSERT INTO custody_account(id,owner_id,account_type,identifier_masked) "
        "VALUES('account','self','CAMS','Synthetic account')"
    )
    db.execute("UPDATE holding SET account_id='account',asset_category='MUTUAL_FUND' WHERE id='holding'")
    db.execute(
        "INSERT INTO holding_cas_identity(holding_id,owner_id,account_id,isin,folio) "
        "VALUES('holding','self','account','INF000000001','12345')"
    )
    assert_rejected(
        db, "INSERT INTO holding_cas_identity(holding_id,owner_id,account_id,isin,folio) "
            "VALUES('other','self','account','INF000000002','54321')"
    )
    assert_rejected(
        db, "INSERT INTO holding_cas_identity(holding_id,owner_id,account_id,isin,folio) "
            "VALUES('other','spouse','account','INF000000002','54321')"
    )
    assert_rejected(
        db, "INSERT INTO holding_cas_identity(holding_id,owner_id,account_id,isin,folio) "
            "VALUES('other','spouse','missing','INF000000002','54321')"
    )
    assert_rejected(
        db, "UPDATE holding_cas_identity SET folio='forged' WHERE holding_id='holding'"
    )
    assert_rejected(
        db, "DELETE FROM holding_cas_identity WHERE holding_id='holding'"
    )
    if db.execute("PRAGMA integrity_check").fetchone() != ("ok",):
        raise AssertionError("SQLite v3 integrity check failed")


def check_v4(db: sqlite3.Connection) -> None:
    tables = {
        name for (name,) in db.execute("SELECT name FROM sqlite_master WHERE type='table'")
        if not name.startswith("sqlite_")
    }
    if tables != EXPECTED_TABLES:
        raise AssertionError(f"Unexpected v4 vault tables: {tables ^ EXPECTED_TABLES}")
    if db.execute("PRAGMA user_version").fetchone() != (4,):
        raise AssertionError("Expected schema version 4")
    if db.execute("PRAGMA foreign_keys").fetchone() != (1,):
        raise AssertionError("Foreign keys were disabled")
    for table in EXPECTED_TABLES:
        cols = db.execute(f'PRAGMA table_info("{table}")').fetchall()
        if not cols:
            raise AssertionError(f"Empty or missing table: {table}")
        if any(col[2].upper() == "REAL" for col in cols):
            raise AssertionError(f"Floating-point financial storage in {table}")
    trigger_names = {row[0] for row in db.execute(
        "SELECT name FROM sqlite_master WHERE type='trigger'"
    )}
    if not {
        "deny_ledger_update", "deny_ledger_delete", "validate_rich_ledger_insert",
        "cas_holding_account_match", "deny_cas_mapping_update",
        "deny_cas_mapping_delete", "validate_ledger_profile_owner",
        "deny_ledger_profile_update", "deny_ledger_profile_delete",
        "validate_holding_goal_owner", "deny_tax_rule_profile_update",
        "deny_tax_rule_profile_delete",
    }.issubset(trigger_names):
        raise AssertionError("Lost immutable ledger or owner triggers")
    # Original event rows must survive upgrades bit-for-bit.
    row = db.execute("SELECT kind,quantity_delta_scaled,amount_scaled,source_fingerprint "
                     "FROM ledger_event WHERE id='original'").fetchone()
    if row != ("BUY", 100000000, 10000, None):
        raise AssertionError("Original financial record mutated")
    db.execute(
        "INSERT INTO family_profile(owner_id,entity_type,pan_masked,date_of_birth,extra_json) "
        "VALUES('self','INDIVIDUAL','*****1234*','1990-01-01','{}')"
    )
    assert_rejected(db, "INSERT INTO family_profile(owner_id) VALUES('unknown')")
    db.execute(
        "INSERT INTO custody_profile(account_id,owner_id,custodian,dp_id,nominee) "
        "VALUES('account','self','Synthetic Registrar','TESTDP','Synthetic Nominee')"
    )
    assert_rejected(
        db, "INSERT INTO custody_profile(account_id,owner_id) VALUES('account','spouse')"
    )
    db.execute(
        "INSERT INTO instrument_profile(id,name,category,sub_category,isin) "
        "VALUES('ins','Synthetic Mutual Fund','MUTUAL_FUND','Open ended','INF000000001')"
    )
    db.execute(
        "INSERT INTO holding_profile(holding_id,owner_id,instrument_id,details_json,"
        "average_cost_per_unit_scaled,cost_unknown,tags_json) "
        """VALUES('holding','self','ins','{"mfPlan":"DIRECT","mfOption":"GROWTH"}',"""
        "NULL,1,'[\"Retirement\"]')"
    )
    saved = db.execute(
        "SELECT details_json,average_cost_per_unit_scaled,cost_unknown,tags_json "
        "FROM holding_profile WHERE holding_id='holding'"
    ).fetchone()
    if saved != ('{"mfPlan":"DIRECT","mfOption":"GROWTH"}', None, 1, '["Retirement"]'):
        raise AssertionError("Legacy asset-specific metadata or unknown cost lost")
    assert_rejected(db, "INSERT INTO holding_profile(holding_id,owner_id) "
                        "VALUES('holding','spouse')")
    db.execute(
        "INSERT INTO family_goal(id,owner_id,name,target_scaled,created_at) "
        "VALUES('goal','self','Retirement',50000000,'2026-10-10')"
    )
    db.execute("INSERT INTO holding_goal(goal_id,holding_id,owner_id) "
               "VALUES('goal','holding','self')")
    assert_rejected(db, "INSERT INTO holding_goal(goal_id,holding_id,owner_id) "
                        "VALUES('goal','other','spouse')")
    db.execute("INSERT INTO ledger_profile(event_id,owner_id,holding_id,source_ref) "
               "VALUES('original','self','holding','synthetic')")
    assert_rejected(db, "UPDATE ledger_profile SET source_ref='forged'")
    assert_rejected(db, "DELETE FROM ledger_profile")
    assert_rejected(db, "INSERT INTO ledger_profile(event_id,owner_id,holding_id) "
                        "VALUES('bonus','spouse','other')")
    db.execute(
        "INSERT INTO statement_snapshot_profile(snapshot_id,owner_id,issuer,invested_scaled) "
        "VALUES('snapshot','self','CAMS',NULL)"
    )
    db.execute(
        "INSERT INTO statement_position_profile(snapshot_id,owner_id,holding_id,"
        "invested_scaled) VALUES('snapshot','self','holding',NULL)"
    )
    if db.execute("SELECT invested_scaled FROM statement_position_profile").fetchone() != (None,):
        raise AssertionError("Unknown statement purchase cost became zero")
    db.execute(
        "INSERT INTO legacy_field_extension(record_type,record_id,owner_id,"
        "field_name,field_type,field_json,source_schema) "
        "VALUES('holding','holding','self','oldUnknownField','object',"
        "'{\"foo\":123}',9)"
    )
    db.execute(
        "INSERT INTO listed_security_profile(isin,name,category,price_date,source,updated_at) "
        "VALUES('INF000000002','Listed synthetic fund','MUTUAL_FUND',"
        "'2026-10-09','AMFI','2026-10-10')"
    )
    db.execute(
        "INSERT INTO tax_rule_profile(id,rules_json) VALUES('AY2026','{}')"
    )
    assert_rejected(db, "UPDATE tax_rule_profile SET rules_json='{}'")
    assert_rejected(db, "DELETE FROM tax_rule_profile WHERE id='AY2026'")
    db.execute(
        "INSERT INTO gmail_seen_profile(message_id,attachment_id,owner_id,seen_at) "
        "VALUES('synthetic-message','synthetic-attachment','self','2026-10-10')"
    )
    db.execute(
        "INSERT INTO egress_log_profile(id,occurred_at,destination,purpose,outcome) "
        "VALUES('egress','2026-10-10','example.invalid','synthetic','denied')"
    )
    if db.execute("PRAGMA integrity_check").fetchone() != ("ok",):
        raise AssertionError("v4 SQLite integrity failed")
    if db.execute("PRAGMA foreign_key_check").fetchall():
        raise AssertionError("v4 owner references invalid")


def check_v5(db: sqlite3.Connection) -> None:
    if db.execute("PRAGMA user_version").fetchone() != (5,):
        raise AssertionError("Expected schema version 5")
    cols = {row[1] for row in db.execute("PRAGMA table_info('ledger_profile')")}
    needed = {"settlement_date", "unit_balance_after", "description", "entered_by"}
    if not needed.issubset(cols):
        raise AssertionError(f"ledger_profile missing legacy columns: {needed - cols}")
    triggers = {row[0] for row in db.execute(
        "SELECT name FROM sqlite_master WHERE type='trigger'"
    )}
    if "validate_ledger_profile_settlement" not in triggers:
        raise AssertionError("Missing settlement-date trigger")
    row = db.execute(
        "SELECT kind,quantity_delta_scaled,amount_scaled FROM ledger_event WHERE id='original'"
    ).fetchone()
    if row != ("BUY", 100000000, 10000):
        raise AssertionError("Original financial record mutated during v5")
    saved = db.execute(
        "SELECT source_ref,settlement_date,unit_balance_after,description,entered_by "
        "FROM ledger_profile WHERE event_id='original'"
    ).fetchone()
    if saved != ("synthetic", None, None, None, "owner"):
        raise AssertionError(f"Existing ledger profile was rewritten: {saved}")
    db.execute(
        "INSERT INTO ledger_event(id,owner_id,holding_id,kind,event_date,"
        "quantity_delta_scaled,amount_scaled) VALUES('late','self','holding','BUY',"
        "'2026-02-01',1,1)"
    )
    assert_rejected(
        db,
        "INSERT INTO ledger_profile(event_id,owner_id,holding_id,settlement_date) "
        "VALUES('late','self','holding','2026-01-01')",
    )
    db.execute(
        "INSERT INTO ledger_profile(event_id,owner_id,holding_id,settlement_date,"
        "unit_balance_after,description) VALUES('late','self','holding','2026-02-02',"
        "100000000,'Synthetic note')"
    )
    kept = db.execute(
        "SELECT settlement_date,unit_balance_after,description,entered_by "
        "FROM ledger_profile WHERE event_id='late'"
    ).fetchone()
    if kept != ("2026-02-02", 100000000, "Synthetic note", "owner"):
        raise AssertionError(f"New ledger profile columns were not stored: {kept}")
    db.execute(
        "INSERT INTO ledger_event(id,owner_id,holding_id,kind,event_date,"
        "quantity_delta_scaled,amount_scaled) VALUES('clerk','self','holding','BUY',"
        "'2026-03-01',1,1)"
    )
    assert_rejected(
        db,
        "INSERT INTO ledger_profile(event_id,owner_id,holding_id,entered_by) "
        "VALUES('clerk','self','holding','clerk')",
    )
    assert_rejected(db, "UPDATE ledger_profile SET description='changed'")
    if db.execute("PRAGMA integrity_check").fetchone() != ("ok",):
        raise AssertionError("v5 SQLite integrity failed")
    if db.execute("PRAGMA foreign_key_check").fetchall():
        raise AssertionError("v5 references invalid")


def main() -> None:
    source = SCHEMA_FILE.read_text(encoding="utf-8")
    v1 = statements_from_kotlin(source, "ddl", V1_STATEMENT_COUNT)
    upgrade = statements_from_kotlin(source, "migrationToV2", MIGRATION_STATEMENT_COUNT)
    identity_upgrade = statements_from_kotlin(
        source, "migrationToV3", V3_MIGRATION_STATEMENT_COUNT
    )
    parity_upgrade = statements_from_kotlin(
        source, "migrationToV4", V4_MIGRATION_STATEMENT_COUNT
    )
    profile_upgrade = statements_from_kotlin(
        source, "migrationToV5", V5_MIGRATION_STATEMENT_COUNT
    )
    if any(not s.startswith(("CREATE ", "ALTER TABLE "))
           for s in v1 + upgrade + identity_upgrade + parity_upgrade + profile_upgrade):
        raise AssertionError("Unexpected statement in schema migration.")
    # Old v1 data is not rewritten and the v2 operation is crash-atomic.
    with sqlite3.connect(":memory:") as db:
        db.execute("PRAGMA foreign_keys = ON")
        prepare_v1(db, v1)
        db.execute("BEGIN")
        run_statements(db, upgrade[:2], "interrupted v2 migration")
        db.rollback()
        if "unit_price_scaled" in {
            row[1] for row in db.execute("PRAGMA table_info('ledger_event')")
        }:
            raise AssertionError("Interrupted migration left a partial schema")
        db.execute("BEGIN")
        run_statements(db, upgrade, "v1->v2 migration")
        db.execute("PRAGMA user_version = 2")
        db.commit()
        check_v2(db)
        # check_v2 performs synthetic ledger INSERTs. SQLite's implicit write
        # transaction must be committed before the next explicit BEGIN.
        db.commit()
        # A failed v2->v3 migration must not strand partial identity tables.
        db.execute("BEGIN")
        run_statements(db, identity_upgrade[:1], "interrupted v3 migration")
        db.rollback()
        if "holding_cas_identity" in {
            name for (name,) in db.execute(
                "SELECT name FROM sqlite_master WHERE type = 'table'"
            )
        }:
            raise AssertionError("Interrupted v3 migration left a partial table")
        db.execute("BEGIN")
        run_statements(db, identity_upgrade, "v2->v3 migration")
        db.execute("PRAGMA user_version = 3")
        db.commit()
        check_v3(db)
        db.commit()
        # v3->v4 partial CREATE TABLE operations must be rollback-safe.
        db.execute("BEGIN")
        run_statements(db, parity_upgrade[:2], "interrupted v4 migration")
        db.rollback()
        if "family_profile" in {
            name for (name,) in db.execute(
                "SELECT name FROM sqlite_master WHERE type='table'"
            )
        }:
            raise AssertionError("Partial v4 migration survived rollback")
        db.execute("BEGIN")
        run_statements(db, parity_upgrade, "v3->v4 migration")
        db.execute("PRAGMA user_version = 4")
        db.commit()
        check_v4(db)
        db.commit()
        db.execute("BEGIN")
        run_statements(db, profile_upgrade[:1], "interrupted v5 migration")
        db.rollback()
        if "settlement_date" in {
            row[1] for row in db.execute("PRAGMA table_info('ledger_profile')")
        }:
            raise AssertionError("Interrupted v5 migration left a partial column")
        db.execute("BEGIN")
        run_statements(db, profile_upgrade, "v4->v5 migration")
        db.execute("PRAGMA user_version = 5")
        db.commit()
        check_v5(db)

    # Fresh installs follow the production current==0 path: v1 DDL + v2 + v3.
    with sqlite3.connect(":memory:") as db:
        db.execute("PRAGMA foreign_keys = ON")
        db.execute("BEGIN")
        run_statements(db, v1, "fresh vault DDL")
        run_statements(db, upgrade, "fresh vault v2")
        run_statements(db, identity_upgrade, "fresh vault v3")
        run_statements(db, parity_upgrade, "fresh vault v4")
        run_statements(db, profile_upgrade, "fresh vault v5")
        db.execute("PRAGMA user_version = 5")
        db.commit()
        prepare_tables = {name for (name,) in db.execute(
            "SELECT name FROM sqlite_master WHERE type='table'"
        ) if not name.startswith("sqlite_")}
        if prepare_tables != EXPECTED_TABLES:
            raise AssertionError("Fresh v5 vault missing tables.")
        profile_cols = {row[1] for row in db.execute("PRAGMA table_info('ledger_profile')")}
        if not {"settlement_date", "unit_balance_after", "description", "entered_by"}.issubset(profile_cols):
            raise AssertionError("Fresh vault missing ledger profile columns.")
        if not RICH_COLUMNS.issubset({
            row[1] for row in db.execute("PRAGMA table_info('ledger_event')")
        }):
            raise AssertionError("Fresh v4 vault missing rich ledger fields.")
        if "holding_cas_identity" not in prepare_tables:
            raise AssertionError("Fresh vault missing CAS mapping.")
        if db.execute("PRAGMA foreign_key_check").fetchall():
            raise AssertionError("Fresh vault has broken foreign keys.")
        if db.execute("PRAGMA integrity_check").fetchone() != ("ok",):
            raise AssertionError("Fresh v4 vault failed integrity check")

    print(
        f"PASS: {len(v1)} v1 DDL + {len(upgrade)} v2 + "
        f"{len(identity_upgrade)} v3 + {len(parity_upgrade)} v4 + "
        f"{len(profile_upgrade)} v5 migrations; "
        "v1/v2/v3/v4 upgrade, rollback, "
        "CAS identity ownership, immutable ledger and backup-compatible columns."
    )


if __name__ == "__main__":
    main()

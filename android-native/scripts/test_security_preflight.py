#!/usr/bin/env python3
"""Regression tests for static policy routing; never uses actual vault data.

Run: python3 -m unittest discover -s android-native/scripts -p 'test_*.py'
These are SOURCE checks, not Gradle builds or mobile penetration tests.
"""
import unittest

from security_preflight import (
    read, repository_binding_errors, vault_health_boundary_errors,
    cipher_page_integrity_errors, ui_publication_guard_errors, app_password_gate_errors,
)

SOURCE = "android-native/data/src/main/kotlin/in/growurdhan/data/vault/SqlCipherVaultRepository.kt"


class RepositoryPolicyRoutingTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.production = read(SOURCE)

    def assert_rejected(self, original: str, replacement: str, method: str):
        self.assertIn(original, self.production, "Test fixture no longer matches production.")
        mutant = self.production.replace(original, replacement, 1)
        issues = repository_binding_errors(mutant)
        self.assertTrue(
            any(method in problem for problem in issues),
            "Static policy must reject bypass of " + method + ": " + repr(issues),
        )

    def test_every_current_production_entry_point_is_guarded(self):
        self.assertEqual([], repository_binding_errors(self.production))

    def test_private_book_read_cannot_bypass_authorized_session(self):
        self.assert_rejected(
            "sessionBound.read { delegate.readBook() }",
            "delegate.readBook()",
            "readBook",
        )

    def test_financial_approval_cannot_call_sqlcipher_directly(self):
        self.assert_rejected(
            "approvedWrite { delegate.commitReviewedStatement(review) }",
            "delegate.commitReviewedStatement(review)",
            "commitReviewedStatement",
        )

    def test_backup_export_cannot_bypass_recent_approval(self):
        self.assert_rejected(
            "sessionBound.readRecentlyApprovedBytes { delegate.exportEncryptedBackup(passphrase) }",
            "delegate.exportEncryptedBackup(passphrase)",
            "exportEncryptedBackup",
        )

    def test_explicit_close_must_share_open_cleanup_mutex(self):
        self.assert_rejected(
            "override suspend fun close() = sessionBound.close()",
            "override suspend fun close() = delegate.close()",
            "close",
        )

    def test_transaction_must_recheck_authorization_before_commit(self):
        self.assert_rejected(
            "check(gate.sessionRevision() == approvedRevision)",
            "check(true)",
            "transaction",
        )

    def test_production_sqlcipher_must_receive_session_authorization(self):
        self.assert_rejected(
            "SqlCipherVaultRepository(context, mutationAuthorization = authorization)",
            "SqlCipherVaultRepository(context)",
            "transaction authorization",
        )

    def test_new_unclassified_entry_point_is_rejected(self):
        mutant = self.production.replace(
            "override suspend fun status(): VaultStatus = delegate.status()",
            "override suspend fun status(): VaultStatus = delegate.status()\n"
            "    override suspend fun leakUnapproved(): Any = delegate.readBook()",
            1,
        )
        self.assertTrue(
            any("Unclassified production repository method leakUnapproved" in issue
                for issue in repository_binding_errors(mutant)),
        )

    def test_corrupt_health_cannot_be_accepted_as_ready(self):
        model = read(
            "android-native/app/src/main/java/in/growurdhan/app/presentation/VaultViewModel.kt"
        )
        gate = read(
            "android-native/domain/src/main/kotlin/in/growurdhan/domain/vault/VaultHealthGate.kt"
        )
        self.assertEqual([], vault_health_boundary_errors(model, gate))
        mutant = gate.replace('health.integrity == "ok"', "true", 1)
        self.assertTrue(vault_health_boundary_errors(model, mutant))

    def test_failed_health_query_must_close_sqlcipher_and_revoke_session(self):
        model = read(
            "android-native/app/src/main/java/in/growurdhan/app/presentation/VaultViewModel.kt"
        )
        gate = read(
            "android-native/domain/src/main/kotlin/in/growurdhan/domain/vault/VaultHealthGate.kt"
        )
        without_close = model.replace("repository.close()\n                } catch", "Unit\n                } catch", 1)
        self.assertTrue(vault_health_boundary_errors(without_close, gate))
        without_cleanup = model.replace("withContext(NonCancellable)", "withContext(Dispatchers.IO)", 1)
        self.assertTrue(vault_health_boundary_errors(without_cleanup, gate))
        without_ui_route = model.replace(
            "val health = verifiedHealthOrClose()", "val health = repository.health()", 1,
        )
        self.assertTrue(vault_health_boundary_errors(without_ui_route, gate))

    def test_encrypted_page_hmac_must_be_validated_on_open(self):
        schema = read(
            "android-native/data/src/main/kotlin/in/growurdhan/data/vault/VaultSchema.kt"
        )
        self.assertEqual([], cipher_page_integrity_errors(schema))
        without_hmac = schema.replace(
            'db.rawQuery("PRAGMA cipher_integrity_check"',
            'db.rawQuery("PRAGMA wrong_integrity_check"', 1,
        )
        self.assertTrue(cipher_page_integrity_errors(without_hmac))
        ignored_errors = schema.replace("if (pageErrors != 0)", "if (false)", 1)
        self.assertTrue(cipher_page_integrity_errors(ignored_errors))

    def test_async_financial_views_reject_old_device_credential_results(self):
        root = "android-native/app/src/main/java/in/growurdhan/app/presentation/"
        names = (
            "VaultSessionGate.kt", "VaultViewModel.kt",
            "HouseholdDashboardViewModel.kt", "StatementReviewViewModel.kt",
            "FamilyMemberViewModel.kt", "CustodyHoldingViewModel.kt",
            "PortableRecoveryViewModel.kt", "ManualTradeViewModel.kt",
        )
        sources = {name: read(root + name) for name in names}
        self.assertEqual([], ui_publication_guard_errors(sources))
        for name in names[1:]:
            token = ("authenticatedRevision" if name == "PortableRecoveryViewModel.kt"
                     else "revision" if name == "ManualTradeViewModel.kt"
                     else "originalRevision")
            mutant = sources.copy()
            mutant[name] = mutant[name].replace(
                f"session.isCurrentSession({token})", "session.isUnlocked()", 1,
            )
            self.assertTrue(
                any(name in problem for problem in ui_publication_guard_errors(mutant)),
                "A removed device-credential binding must fail for " + name,
            )
        mutant = sources.copy()
        mutant["VaultSessionGate.kt"] = mutant["VaultSessionGate.kt"].replace(
            "credentialRevision == originalRevision && isUnlocked()",
            "isUnlocked()", 1,
        )
        self.assertTrue(ui_publication_guard_errors(mutant))

    def test_manual_trade_must_require_device_challenge_and_checked_lots(self):
        activity = read("android-native/app/src/main/java/in/growurdhan/app/MainActivity.kt")
        self.assertIn("CredentialAction.APPROVE_MANUAL_TRADE", activity)
        self.assertIn("ManualTradeIntent.SaveAfterCredential -> Unit", activity)
        planner = read(
            "android-native/domain/src/main/kotlin/in/growurdhan/domain/finance/ManualTradePlanner.kt"
        )
        self.assertIn("LotEngine.fold", planner)
        self.assertIn('holding.assetCategory != "LIABILITY"', planner)
        self.assertIn("BigInteger.valueOf(quantity)", planner)

    def test_manual_trade_cannot_bypass_existing_guarded_write(self):
        production = self.production
        self.assertIn(
            "approvedWrite { delegate.appendFinancialEvents(events) }", production
        )
        mutant = production.replace(
            "approvedWrite { delegate.appendFinancialEvents(events) }",
            "delegate.appendFinancialEvents(events)", 1,
        )
        self.assertTrue(any(
            "appendFinancialEvents" in problem
            for problem in repository_binding_errors(mutant)
        ))

    def test_atomic_manual_trade_cannot_bypass_recent_device_approval(self):
        self.assertIn("approvedWrite { delegate.commitManualTrade(draft, reviewed) }",
                      self.production)
        mutant = self.production.replace(
            "approvedWrite { delegate.commitManualTrade(draft, reviewed) }",
            "delegate.commitManualTrade(draft, reviewed)", 1,
        )
        self.assertTrue(any("commitManualTrade" in issue
                            for issue in repository_binding_errors(mutant)))

    def test_local_password_gates_are_necessary_for_os_and_financial_access(self):
        root = "android-native/app/src/main/java/in/growurdhan/app/presentation/"
        gate = read(root + "VaultSessionGate.kt")
        binding = read(root + "VaultAuthorizationModule.kt")
        store = read(root + "LocalAppPasswordStore.kt")
        activity = read("android-native/app/src/main/java/in/growurdhan/app/MainActivity.kt")
        self.assertEqual([], app_password_gate_errors(gate, binding, store, activity))
        without_password = gate.replace(
            "localPassword.hasVerifiedPassword() && freshWithin(maxSessionAgeMillis)",
            "freshWithin(maxSessionAgeMillis)", 1,
        )
        self.assertTrue(app_password_gate_errors(
            without_password, binding, store, activity,
        ))
        without_binding = binding.replace(
            "bindAppPassword(store: LocalAppPasswordStore): AppPasswordAuthorization",
            "bindAppPassword(): AppPasswordAuthorization", 1,
        )
        self.assertTrue(app_password_gate_errors(
            gate, without_binding, store, activity,
        ))

    def test_missing_repository_method_is_rejected(self):
        mutant = self.production.replace(
            "override suspend fun readBook(): VaultBook = sessionBound.read { delegate.readBook() }",
            "fun removedReadBookFromProduction() = Unit",
            1,
        )
        self.assertTrue(any("readBook" in x for x in repository_binding_errors(mutant)))


if __name__ == "__main__":
    unittest.main()

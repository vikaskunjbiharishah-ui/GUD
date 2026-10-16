package `in`.growurdhan.app.presentation

import `in`.growurdhan.domain.vault.UnitTestPasswordAdmission

import `in`.growurdhan.core.model.*
import `in`.growurdhan.domain.vault.VaultRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Synthetic records; this VM is not a credential authenticator. */
@OptIn(ExperimentalCoroutinesApi::class)
class CustodyHoldingViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private val owner = OwnerId("self")
    private val member = FamilyMember(owner, "Synthetic", "Self", true)
    private val account = CustodyAccount("account", owner, "CAMS", "Main synthetic")
    private fun book(accounts: List<CustodyAccount> = emptyList()) =
        VaultBook(listOf(member), emptyList(), emptyList(), accounts)

    @Test fun priorDeviceCredentialCannotPublishDelayedCustodyBook() = runTest {
        val repo = mockk<VaultRepository>()
        coEvery { repo.holdingProfiles() } returns emptyList()
        coEvery { repo.familyAdministration() } returns FamilyAdministrationSnapshot(
            family = emptyList(), accounts = emptyList(), goals = emptyList(), holdings = emptyList(),
        )
        val pending = CompletableDeferred<VaultBook>()
        coEvery { repo.readBook() } coAnswers { pending.await() }
        val gate = VaultSessionGate(UnitTestPasswordAdmission).also { it.grantDeviceCredential() }
        val vm = CustodyHoldingViewModel(repo, gate)
        vm.onIntent(CustodyHoldingIntent.Refresh)
        runCurrent()
        gate.lock()
        gate.grantDeviceCredential()
        pending.complete(book(listOf(account)))
        advanceUntilIdle()

        assertEquals(CustodyHoldingUiState.Locked, vm.state.value)
    }

    @Test fun priorDeviceCredentialCannotPublishDelayedCustodySave() = runTest {
        val repo = mockk<VaultRepository>()
        coEvery { repo.holdingProfiles() } returns emptyList()
        coEvery { repo.familyAdministration() } returns FamilyAdministrationSnapshot(
            family = emptyList(), accounts = emptyList(), goals = emptyList(), holdings = emptyList(),
        )
        coEvery { repo.readBook() } returns book()
        val pending = CompletableDeferred<CustodyAccount>()
        coEvery { repo.registerCustodyAccount(any(), any(), any()) } coAnswers {
            pending.await()
        }
        val gate = VaultSessionGate(UnitTestPasswordAdmission).also { it.grantDeviceCredential() }
        val vm = CustodyHoldingViewModel(repo, gate)
        vm.onIntent(CustodyHoldingIntent.Refresh)
        advanceUntilIdle()
        vm.onIntent(CustodyHoldingIntent.StageAccount(owner, "CAMS", "Main"))
        gate.grantDeviceCredential()
        vm.onIntent(CustodyHoldingIntent.SaveAfterCredential)
        runCurrent()
        gate.lock()
        gate.grantDeviceCredential()
        pending.complete(account)
        advanceUntilIdle()

        assertEquals(CustodyHoldingUiState.Locked, vm.state.value)
        coVerify(exactly = 1) { repo.registerCustodyAccount(any(), any(), any()) }
    }

    @Test fun manualNpsAssetStartsAtZeroAfterSeparateCredentialApproval() = runTest {
        val repo = mockk<VaultRepository>()
        coEvery { repo.holdingProfiles() } returns emptyList()
        coEvery { repo.familyAdministration() } returns FamilyAdministrationSnapshot(
            family = emptyList(), accounts = emptyList(), goals = emptyList(), holdings = emptyList(),
        )
        val gate = VaultSessionGate(UnitTestPasswordAdmission).also { it.grantDeviceCredential() }
        coEvery { repo.readBook() } returns book()
        val asset = Holding(
            HoldingId("nps-test"), owner, null, "Synthetic NPS Tier I", "NPS",
            QuantityScaled(0), MoneyScaled(0),
        )
        coEvery { repo.registerManualAsset(owner, "Synthetic NPS Tier I", "NPS") } returns asset
        val vm = CustodyHoldingViewModel(repo, gate)
        vm.onIntent(CustodyHoldingIntent.Refresh)
        advanceUntilIdle()
        vm.onIntent(CustodyHoldingIntent.StageAsset(owner, "Synthetic NPS Tier I", "NPS"))
        assertTrue(vm.state.value is CustodyHoldingUiState.AwaitingCredential)
        vm.onIntent(CustodyHoldingIntent.SaveAfterCredential)
        advanceUntilIdle()
        coVerify(exactly = 0) { repo.registerManualAsset(any(), any(), any()) }
        assertEquals(CustodyHoldingUiState.Locked, vm.state.value)

        vm.onIntent(CustodyHoldingIntent.Refresh)
        advanceUntilIdle()
        vm.onIntent(CustodyHoldingIntent.StageAsset(owner, "Synthetic NPS Tier I", "NPS"))
        gate.grantDeviceCredential()
        vm.onIntent(CustodyHoldingIntent.SaveAfterCredential)
        advanceUntilIdle()
        assertTrue(vm.state.value is CustodyHoldingUiState.Saved)
        coVerify(exactly = 1) { repo.registerManualAsset(owner, "Synthetic NPS Tier I", "NPS") }
        coVerify(exactly = 0) { repo.appendFinancialEvents(any()) }
    }

    @Test fun lockedRefreshNeverReadsEncryptedBook() = runTest {
        val repo = mockk<VaultRepository>()
        coEvery { repo.holdingProfiles() } returns emptyList()
        coEvery { repo.familyAdministration() } returns FamilyAdministrationSnapshot(
            family = emptyList(), accounts = emptyList(), goals = emptyList(), holdings = emptyList(),
        )
        val viewModel = CustodyHoldingViewModel(repo, VaultSessionGate(UnitTestPasswordAdmission))
        viewModel.onIntent(CustodyHoldingIntent.Refresh)
        advanceUntilIdle()
        assertEquals(CustodyHoldingUiState.Locked, viewModel.state.value)
        coVerify(exactly = 0) { repo.readBook() }
    }

    @Test fun validAccountFormNeedsNewCredentialAfterStaging() = runTest {
        val repo = mockk<VaultRepository>()
        coEvery { repo.holdingProfiles() } returns emptyList()
        coEvery { repo.familyAdministration() } returns FamilyAdministrationSnapshot(
            family = emptyList(), accounts = emptyList(), goals = emptyList(), holdings = emptyList(),
        )
        val gate = VaultSessionGate(UnitTestPasswordAdmission).also { it.grantDeviceCredential() }
        coEvery { repo.readBook() } returns book()
        val vm = CustodyHoldingViewModel(repo, gate)
        vm.onIntent(CustodyHoldingIntent.Refresh)
        advanceUntilIdle()
        vm.onIntent(CustodyHoldingIntent.StageAccount(owner, "CAMS", "Main"))
        assertTrue(vm.state.value is CustodyHoldingUiState.AwaitingCredential)
        vm.onIntent(CustodyHoldingIntent.SaveAfterCredential)
        advanceUntilIdle()
        assertEquals(CustodyHoldingUiState.Locked, vm.state.value)
        coVerify(exactly = 0) { repo.registerCustodyAccount(any(), any(), any()) }
    }

    @Test fun freshCredentialAllowsOneExplicitRegistrarRegistration() = runTest {
        val repo = mockk<VaultRepository>()
        coEvery { repo.holdingProfiles() } returns emptyList()
        coEvery { repo.familyAdministration() } returns FamilyAdministrationSnapshot(
            family = emptyList(), accounts = emptyList(), goals = emptyList(), holdings = emptyList(),
        )
        val gate = VaultSessionGate(UnitTestPasswordAdmission).also { it.grantDeviceCredential() }
        coEvery { repo.readBook() } returns book()
        coEvery { repo.registerCustodyAccount(owner, "CAMS", "Main") } returns account
        val vm = CustodyHoldingViewModel(repo, gate)
        vm.onIntent(CustodyHoldingIntent.Refresh)
        advanceUntilIdle()
        vm.onIntent(CustodyHoldingIntent.StageAccount(owner, "CAMS", "Main"))
        gate.grantDeviceCredential()
        vm.onIntent(CustodyHoldingIntent.SaveAfterCredential)
        advanceUntilIdle()
        assertTrue(vm.state.value is CustodyHoldingUiState.Saved)
        vm.onIntent(CustodyHoldingIntent.SaveAfterCredential)
        advanceUntilIdle()
        coVerify(exactly = 1) { repo.registerCustodyAccount(owner, "CAMS", "Main") }
    }

    @Test fun invalidAndCrossOwnerEnrolmentCannotAskForCredential() = runTest {
        val repo = mockk<VaultRepository>()
        coEvery { repo.holdingProfiles() } returns emptyList()
        coEvery { repo.familyAdministration() } returns FamilyAdministrationSnapshot(
            family = emptyList(), accounts = emptyList(), goals = emptyList(), holdings = emptyList(),
        )
        val gate = VaultSessionGate(UnitTestPasswordAdmission).also { it.grantDeviceCredential() }
        coEvery { repo.readBook() } returns book(listOf(account))
        val vm = CustodyHoldingViewModel(repo, gate)
        vm.onIntent(CustodyHoldingIntent.Refresh)
        advanceUntilIdle()
        vm.onIntent(CustodyHoldingIntent.StageHolding(
            OwnerId("other"), account.id, "Synthetic", "INF000000001", "123",
        ))
        assertTrue(vm.state.value is CustodyHoldingUiState.Ready)
        vm.onIntent(CustodyHoldingIntent.StageHolding(
            owner, account.id, "Synthetic", "invalid", "123",
        ))
        assertTrue(vm.state.value is CustodyHoldingUiState.Ready)
        coVerify(exactly = 0) { repo.registerCasHolding(any(), any(), any(), any(), any()) }
    }

    @Test fun approvedHoldingCreatesNoLedgerEventOrAutomaticStatementImport() = runTest {
        val repo = mockk<VaultRepository>()
        coEvery { repo.holdingProfiles() } returns emptyList()
        coEvery { repo.familyAdministration() } returns FamilyAdministrationSnapshot(
            family = emptyList(), accounts = emptyList(), goals = emptyList(), holdings = emptyList(),
        )
        val gate = VaultSessionGate(UnitTestPasswordAdmission).also { it.grantDeviceCredential() }
        coEvery { repo.readBook() } returns book(listOf(account))
        val enrolled = Holding(
            HoldingId("new"), owner, account.id, "Synthetic fund",
            "MUTUAL_FUND", QuantityScaled(0), MoneyScaled(0),
        )
        coEvery {
            repo.registerCasHolding(owner, account.id, "Synthetic fund", "INF000000001", "123")
        } returns enrolled
        val vm = CustodyHoldingViewModel(repo, gate)
        vm.onIntent(CustodyHoldingIntent.Refresh)
        advanceUntilIdle()
        vm.onIntent(CustodyHoldingIntent.StageHolding(
            owner, account.id, "Synthetic fund", "INF000000001", "123",
        ))
        gate.grantDeviceCredential()
        vm.onIntent(CustodyHoldingIntent.SaveAfterCredential)
        advanceUntilIdle()
        assertTrue(vm.state.value is CustodyHoldingUiState.Saved)
        coVerify(exactly = 1) {
            repo.registerCasHolding(owner, account.id, "Synthetic fund", "INF000000001", "123")
        }
        coVerify(exactly = 0) { repo.appendFinancialEvents(any()) }
        coVerify(exactly = 0) { repo.commitReviewedStatement(any()) }
    }

    @Test fun lockClearsStagedHoldingCredentialsAndForbidsPostLogoutWrite() = runTest {
        val repo = mockk<VaultRepository>()
        coEvery { repo.holdingProfiles() } returns emptyList()
        coEvery { repo.familyAdministration() } returns FamilyAdministrationSnapshot(
            family = emptyList(), accounts = emptyList(), goals = emptyList(), holdings = emptyList(),
        )
        val gate = VaultSessionGate(UnitTestPasswordAdmission).also { it.grantDeviceCredential() }
        coEvery { repo.readBook() } returns book(listOf(account))
        val vm = CustodyHoldingViewModel(repo, gate)
        vm.onIntent(CustodyHoldingIntent.Refresh)
        advanceUntilIdle()
        vm.onIntent(CustodyHoldingIntent.StageHolding(
            owner, account.id, "Synthetic", "INF000000001", "123",
        ))
        gate.lock()
        vm.clearSensitiveState()
        gate.grantDeviceCredential()
        vm.onIntent(CustodyHoldingIntent.SaveAfterCredential)
        advanceUntilIdle()
        assertEquals(CustodyHoldingUiState.Locked, vm.state.value)
        coVerify(exactly = 0) { repo.registerCasHolding(any(), any(), any(), any(), any()) }
    }

    @Test fun stagedGoalLinkRequiresFreshCredentialAndNeverMutatesLedger() = runTest {
        val repo = mockk<VaultRepository>()
        val asset = Holding(
            HoldingId("asset-one"), owner, null, "Synthetic property", "REAL_ESTATE",
            QuantityScaled(0), MoneyScaled(0),
        )
        val familyGoal = FamilyGoalProfile(
            id = "goal-one", ownerId = owner, name = "Synthetic retirement",
            target = null, targetDate = null, priority = null,
            holdingIds = emptyList(), taggedExposure = MoneyScaled(0),
        )
        coEvery { repo.readBook() } returns book().copy(holdings = listOf(asset))
        coEvery { repo.holdingProfiles() } returns emptyList()
        coEvery { repo.familyAdministration() } returns FamilyAdministrationSnapshot(
            family = emptyList(), accounts = emptyList(),
            goals = listOf(familyGoal), holdings = listOf(asset),
        )
        coEvery { repo.applyFamilyAdministration(any()) } returns Unit
        val gate = VaultSessionGate(UnitTestPasswordAdmission).also { it.grantDeviceCredential() }
        val vm = CustodyHoldingViewModel(repo, gate)
        vm.onIntent(CustodyHoldingIntent.Refresh)
        advanceUntilIdle()
        vm.onIntent(CustodyHoldingIntent.StageGoalTag(owner, asset.id, familyGoal.id, true))
        assertTrue(vm.state.value is CustodyHoldingUiState.AwaitingCredential)
        vm.onIntent(CustodyHoldingIntent.SaveAfterCredential)
        advanceUntilIdle()
        coVerify(exactly = 0) { repo.applyFamilyAdministration(any()) }
        assertEquals(CustodyHoldingUiState.Locked, vm.state.value)

        vm.onIntent(CustodyHoldingIntent.Refresh)
        advanceUntilIdle()
        vm.onIntent(CustodyHoldingIntent.StageGoalTag(owner, asset.id, familyGoal.id, true))
        gate.grantDeviceCredential()
        vm.onIntent(CustodyHoldingIntent.SaveAfterCredential)
        advanceUntilIdle()
        coVerify(exactly = 1) {
            repo.applyFamilyAdministration(FamilyAdminChange.TagHolding(
                familyGoal.id, asset.id, owner, true,
            ))
        }
        assertTrue(vm.state.value is CustodyHoldingUiState.Saved)
        coVerify(exactly = 0) { repo.appendFinancialEvents(any()) }
    }

    @Test fun crossOwnerGoalsNeverStageProtectedWrites() = runTest {
        val repo = mockk<VaultRepository>()
        val asset = Holding(HoldingId("owned"), owner, null, "Synthetic", "EQUITY",
            QuantityScaled(0), MoneyScaled(0))
        coEvery { repo.readBook() } returns book().copy(holdings = listOf(asset))
        coEvery { repo.holdingProfiles() } returns emptyList()
        coEvery { repo.familyAdministration() } returns FamilyAdministrationSnapshot(
            family = emptyList(), accounts = emptyList(),
            goals = listOf(FamilyGoalProfile(
                "other-goal", OwnerId("other"), "Other member goal",
                null, null, null, emptyList(), MoneyScaled(0),
            )), holdings = listOf(asset),
        )
        val vm = CustodyHoldingViewModel(repo,
            VaultSessionGate(UnitTestPasswordAdmission).also { it.grantDeviceCredential() })
        vm.onIntent(CustodyHoldingIntent.Refresh)
        advanceUntilIdle()
        vm.onIntent(CustodyHoldingIntent.StageGoalTag(owner, asset.id, "other-goal", true))
        assertTrue(vm.state.value is CustodyHoldingUiState.Ready)
        coVerify(exactly = 0) { repo.applyFamilyAdministration(any()) }
    }
}

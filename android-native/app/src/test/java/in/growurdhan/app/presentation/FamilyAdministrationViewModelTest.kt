package `in`.growurdhan.app.presentation

import `in`.growurdhan.core.model.FamilyAdminChange
import `in`.growurdhan.core.model.FamilyAdministrationSnapshot
import `in`.growurdhan.core.model.OwnerId
import `in`.growurdhan.domain.vault.UnitTestPasswordAdmission
import `in`.growurdhan.domain.vault.VaultRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.just
import io.mockk.Runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** New credential epoch is required even when a user has an unlocked family screen. */
@OptIn(ExperimentalCoroutinesApi::class)
class FamilyAdministrationViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    private val empty = FamilyAdministrationSnapshot(
        emptyList(),emptyList(),emptyList(),emptyList(),
    )

    @Test fun stagedFamilyWriteCannotBorrowEarlierDeviceUnlock() = runTest {
        val gate = VaultSessionGate(UnitTestPasswordAdmission)
        gate.grantDeviceCredential()
        val repo = mockk<VaultRepository>()
        coEvery { repo.familyAdministration() } returns empty
        val vm = FamilyAdministrationViewModel(repo,gate)
        vm.onIntent(FamilyAdminIntent.Refresh)
        advanceUntilIdle()
        assertTrue(vm.state.value is FamilyAdminUiState.Ready)
        vm.onIntent(FamilyAdminIntent.Stage(
            FamilyAdminChange.SetMemberActive(OwnerId("spouse"),false)))
        assertTrue(vm.state.value is FamilyAdminUiState.AwaitingApproval)
        vm.onIntent(FamilyAdminIntent.CommitAfterCredential)
        assertTrue(vm.state.value is FamilyAdminUiState.Locked)
        coVerify(exactly = 0) { repo.applyFamilyAdministration(any()) }
    }

    @Test fun cancelledIdentityOnboardingWipesPlaintextSecurityAnswers() = runTest {
        val gate = VaultSessionGate(UnitTestPasswordAdmission)
        gate.grantDeviceCredential()
        val repo = mockk<VaultRepository>()
        coEvery { repo.familyAdministration() } returns empty
        val vm = FamilyAdministrationViewModel(repo,gate)
        vm.onIntent(FamilyAdminIntent.Refresh)
        advanceUntilIdle()
        val request = FamilyAdminChange.RegisterOwnerIdentity(
            "Synthetic","Owner","owner@example.test","ABCDE1234F","1990-01-01",
            "school","first answer".toCharArray(),"city","second answer".toCharArray(),
        )
        vm.onIntent(FamilyAdminIntent.Stage(request))
        assertTrue(vm.state.value is FamilyAdminUiState.AwaitingApproval)
        vm.clearSensitiveState()
        assertTrue(request.firstAnswer.all { it == '\u0000' })
        assertTrue(request.secondAnswer.all { it == '\u0000' })
        coVerify(exactly = 0) { repo.applyFamilyAdministration(any()) }
    }

    @Test fun freshlyConfirmedWriteUsesRepositoryAndPublishesOnlyCurrentSession() = runTest {
        val gate = VaultSessionGate(UnitTestPasswordAdmission)
        gate.grantDeviceCredential()
        val repo = mockk<VaultRepository>()
        coEvery { repo.familyAdministration() } returns empty
        coEvery { repo.applyFamilyAdministration(any()) } just Runs
        val vm = FamilyAdministrationViewModel(repo,gate)
        vm.onIntent(FamilyAdminIntent.Refresh)
        advanceUntilIdle()
        val change = FamilyAdminChange.SetMemberActive(OwnerId("spouse"),true)
        vm.onIntent(FamilyAdminIntent.Stage(change))
        gate.grantDeviceCredential()
        vm.onIntent(FamilyAdminIntent.CommitAfterCredential)
        advanceUntilIdle()
        assertTrue(vm.state.value is FamilyAdminUiState.Saved)
        coVerify(exactly = 1) { repo.applyFamilyAdministration(change) }
        vm.clearSensitiveState()
        assertTrue(vm.state.value is FamilyAdminUiState.Locked)
    }
}

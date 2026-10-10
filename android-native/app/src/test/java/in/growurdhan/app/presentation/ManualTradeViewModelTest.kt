package `in`.growurdhan.app.presentation

import `in`.growurdhan.domain.vault.UnitTestPasswordAdmission

import `in`.growurdhan.core.model.*
import `in`.growurdhan.domain.finance.*
import `in`.growurdhan.domain.vault.VaultRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ManualTradeViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private val owner = OwnerId("owner")
    private val holding = Holding(
        HoldingId("holding"), owner, null, "Synthetic index fund",
        "MUTUAL_FUND", QuantityScaled(0), MoneyScaled(50_000),
    )
    private val book = VaultBook(
        members = listOf(FamilyMember(owner, "Primary", "Self", true)),
        holdings = listOf(holding), events = emptyList(),
    )
    private val draft = ManualTradeDraft(
        holding.id, ManualTradeKind.BUY, "2026-10-10", "2", "50", "0",
    )

    @Test fun lockedScreenCannotReadBookOrWriteTrade() = runTest {
        val repo = mockk<VaultRepository>()
        val vm = ManualTradeViewModel(repo, VaultSessionGate(UnitTestPasswordAdmission))
        vm.onIntent(ManualTradeIntent.Refresh)
        vm.onIntent(ManualTradeIntent.Stage(draft))
        vm.onIntent(ManualTradeIntent.SaveAfterCredential)
        advanceUntilIdle()
        assertEquals(ManualTradeUiState.Locked, vm.state.value)
        coVerify(exactly = 0) { repo.readBook() }
        coVerify(exactly = 0) { repo.commitManualTrade(any(), any()) }
    }

    @Test fun stagingAndOldCredentialCannotWrite() = runTest {
        val repo = mockk<VaultRepository>()
        coEvery { repo.readBook() } returns book
        val gate = VaultSessionGate(UnitTestPasswordAdmission).also { it.grantDeviceCredential() }
        val vm = ManualTradeViewModel(repo, gate)
        vm.onIntent(ManualTradeIntent.Refresh)
        advanceUntilIdle()
        vm.onIntent(ManualTradeIntent.Stage(draft))
        assertTrue(vm.state.value is ManualTradeUiState.AwaitingCredential)
        vm.onIntent(ManualTradeIntent.SaveAfterCredential)
        advanceUntilIdle()
        assertEquals(ManualTradeUiState.Locked, vm.state.value)
        coVerify(exactly = 0) { repo.commitManualTrade(any(), any()) }
    }

    @Test fun newDeviceCredentialWritesExactlyOneApprovedBuy() = runTest {
        val repo = mockk<VaultRepository>()
        coEvery { repo.readBook() } returns book
        coEvery { repo.commitManualTrade(any(), any()) } returns Unit
        val gate = VaultSessionGate(UnitTestPasswordAdmission).also { it.grantDeviceCredential() }
        val vm = ManualTradeViewModel(repo, gate)
        vm.onIntent(ManualTradeIntent.Refresh)
        advanceUntilIdle()
        vm.onIntent(ManualTradeIntent.Stage(draft))
        gate.grantDeviceCredential()
        vm.onIntent(ManualTradeIntent.SaveAfterCredential)
        advanceUntilIdle()
        assertEquals(ManualTradeUiState.Saved, vm.state.value)
        coVerify(exactly = 1) {
            repo.commitManualTrade(any(), match {
                it.event.kind == FinancialEventKind.BUY &&
                it.event.quantityScaled == 200_000_000L
            })
        }
        vm.onIntent(ManualTradeIntent.SaveAfterCredential)
        advanceUntilIdle()
        coVerify(exactly = 1) { repo.commitManualTrade(any(), any()) }
    }

    @Test fun changedBookAfterDeviceChallengeCannotSilentlyChangeReviewedBuy() = runTest {
        val repo = mockk<VaultRepository>()
        coEvery { repo.readBook() } returnsMany listOf(
            book, book.copy(holdings = listOf(holding.copy(name = "Changed name"))),
        )
        val gate = VaultSessionGate(UnitTestPasswordAdmission).also { it.grantDeviceCredential() }
        val vm = ManualTradeViewModel(repo, gate)
        vm.onIntent(ManualTradeIntent.Refresh)
        advanceUntilIdle()
        vm.onIntent(ManualTradeIntent.Stage(draft))
        gate.grantDeviceCredential()
        vm.onIntent(ManualTradeIntent.SaveAfterCredential)
        advanceUntilIdle()
        assertTrue(vm.state.value is ManualTradeUiState.Error)
        coVerify(exactly = 0) { repo.commitManualTrade(any(), any()) }
    }

    @Test fun lockThenReauthenticationCannotShowStaleCompletedEntry() = runTest {
        val repo = mockk<VaultRepository>()
        coEvery { repo.readBook() } returns book
        val pending = CompletableDeferred<Unit>()
        coEvery { repo.commitManualTrade(any(), any()) } coAnswers { pending.await() }
        val gate = VaultSessionGate(UnitTestPasswordAdmission).also { it.grantDeviceCredential() }
        val vm = ManualTradeViewModel(repo, gate)
        vm.onIntent(ManualTradeIntent.Refresh)
        advanceUntilIdle()
        vm.onIntent(ManualTradeIntent.Stage(draft))
        gate.grantDeviceCredential()
        vm.onIntent(ManualTradeIntent.SaveAfterCredential)
        runCurrent()
        gate.lock()
        gate.grantDeviceCredential()
        pending.complete(Unit)
        advanceUntilIdle()
        assertEquals(ManualTradeUiState.Locked, vm.state.value)
    }

    @Test fun cancelledDraftCannotBeSavedAfterUnlockingAgain() = runTest {
        val repo = mockk<VaultRepository>()
        coEvery { repo.readBook() } returns book
        val gate = VaultSessionGate(UnitTestPasswordAdmission).also { it.grantDeviceCredential() }
        val vm = ManualTradeViewModel(repo, gate)
        vm.onIntent(ManualTradeIntent.Refresh)
        advanceUntilIdle()
        vm.onIntent(ManualTradeIntent.Stage(draft))
        vm.onIntent(ManualTradeIntent.Cancel)
        gate.grantDeviceCredential()
        vm.onIntent(ManualTradeIntent.SaveAfterCredential)
        advanceUntilIdle()
        assertEquals(ManualTradeUiState.Locked, vm.state.value)
        coVerify(exactly = 0) { repo.commitManualTrade(any(), any()) }
    }
}

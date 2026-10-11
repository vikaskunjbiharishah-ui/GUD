package `in`.growurdhan.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.growurdhan.core.model.FamilyAdminChange
import `in`.growurdhan.core.model.FamilyAdministrationSnapshot
import `in`.growurdhan.domain.vault.VaultRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface FamilyAdminUiState {
    data object Locked : FamilyAdminUiState
    data object Loading : FamilyAdminUiState
    data class Ready(val data: FamilyAdministrationSnapshot, val warning: String? = null) : FamilyAdminUiState
    /** Ephemeral, never written to saved instance state or preference storage. */
    class AwaitingApproval(
        val change: FamilyAdminChange, val epoch: Long,
    ) : FamilyAdminUiState
    data object Saving : FamilyAdminUiState
    data object Saved : FamilyAdminUiState
    data class Error(val message: String) : FamilyAdminUiState
}
sealed interface FamilyAdminIntent {
    data object Refresh : FamilyAdminIntent
    class Stage(val change: FamilyAdminChange) : FamilyAdminIntent
    /** ActivityResult.RESULT_OK callback only. Never dispatched from Compose. */
    data object CommitAfterCredential : FamilyAdminIntent
    data object Cancel : FamilyAdminIntent
}

/** Session-epoch bound, explicitly approved M26 household administration. */
@HiltViewModel
class FamilyAdministrationViewModel @Inject constructor(
    private val vault: VaultRepository,
    private val session: VaultSessionGate,
) : ViewModel() {
    private val mutable = MutableStateFlow<FamilyAdminUiState>(FamilyAdminUiState.Locked)
    val state: StateFlow<FamilyAdminUiState> = mutable
    private var generation = 0L
    private var work: Job? = null
    private var inFlightIdentity: FamilyAdminChange.RegisterOwnerIdentity? = null

    private fun wipeIdentity() {
        inFlightIdentity?.wipeSecrets()
        inFlightIdentity = null
    }

    fun onIntent(intent: FamilyAdminIntent) {
        when (intent) {
            FamilyAdminIntent.Refresh -> refresh()
            is FamilyAdminIntent.Stage -> stage(intent.change)
            FamilyAdminIntent.CommitAfterCredential -> save()
            FamilyAdminIntent.Cancel -> clearSensitiveState()
        }
    }

    private fun refresh() {
        work?.cancel()
        wipeIdentity()
        val current = ++generation
        val revision = session.credentialEpoch()
        if (!session.isUnlocked()) { clearSensitiveState(); return }
        mutable.value = FamilyAdminUiState.Loading
        work = viewModelScope.launch {
            try {
                val data = vault.familyAdministration()
                if (current == generation) {
                    if (session.isCurrentSession(revision)) mutable.value =
                        FamilyAdminUiState.Ready(data)
                    else clearSensitiveState()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (current == generation) mutable.value =
                    FamilyAdminUiState.Error("Unable to read the family book. Reopen the vault.")
            }
        }
    }

    private fun stage(change: FamilyAdminChange) {
        if (mutable.value !is FamilyAdminUiState.Ready || !session.isUnlocked()) {
            (change as? FamilyAdminChange.RegisterOwnerIdentity)?.wipeSecrets()
            clearSensitiveState()
            return
        }
        wipeIdentity()
        inFlightIdentity = change as? FamilyAdminChange.RegisterOwnerIdentity
        // Domain and database validations are deliberately rerun INSIDE the
        // single encrypted transaction; the UI snapshot never grants authority.
        mutable.value = FamilyAdminUiState.AwaitingApproval(change, session.credentialEpoch())
    }

    private fun save() {
        val request = mutable.value as? FamilyAdminUiState.AwaitingApproval ?: return
        if (!session.canCommitFinancialChanges() ||
            session.credentialEpoch() <= request.epoch
        ) { clearSensitiveState(); return }
        work?.cancel()
        val current = ++generation
        val revision = session.credentialEpoch()
        mutable.value = FamilyAdminUiState.Saving
        work = viewModelScope.launch {
            try {
                vault.applyFamilyAdministration(request.change)
                if (current == generation) {
                    if (session.isCurrentSession(revision)) mutable.value = FamilyAdminUiState.Saved
                    else clearSensitiveState()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (current == generation) mutable.value = FamilyAdminUiState.Error(
                    "Family update was not confirmed. Reopen this screen to verify your records."
                )
            } finally {
                request.change.let {
                    (it as? FamilyAdminChange.RegisterOwnerIdentity)?.wipeSecrets()
                }
                if (inFlightIdentity === request.change) inFlightIdentity = null
            }
        }
    }

    fun clearSensitiveState() {
        ++generation
        work?.cancel()
        work = null
        wipeIdentity()
        mutable.value = FamilyAdminUiState.Locked
    }

    override fun onCleared() {
        clearSensitiveState()
        super.onCleared()
    }
}

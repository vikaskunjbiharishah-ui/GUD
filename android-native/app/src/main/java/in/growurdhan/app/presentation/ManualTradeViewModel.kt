package `in`.growurdhan.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.growurdhan.core.model.VaultBook
import `in`.growurdhan.domain.finance.ManualTradeDraft
import `in`.growurdhan.domain.finance.ManualTradePlan
import `in`.growurdhan.domain.finance.ManualTradePlanner
import `in`.growurdhan.domain.vault.VaultRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ManualTradeUiState {
    data object Locked : ManualTradeUiState
    data object Loading : ManualTradeUiState
    data class Ready(val book: VaultBook, val notice: String? = null) : ManualTradeUiState
    data class AwaitingCredential(
        val draft: ManualTradeDraft, val plan: ManualTradePlan,
        val stagedRevision: Long,
    ) : ManualTradeUiState
    data object Saving : ManualTradeUiState
    data object Saved : ManualTradeUiState
    data class Error(val message: String) : ManualTradeUiState
}

sealed interface ManualTradeIntent {
    data object Refresh : ManualTradeIntent
    data class Stage(val draft: ManualTradeDraft) : ManualTradeIntent
    /** Activity intercepts; obtaining device credential never happens in a ViewModel. */
    data object RequestApproval : ManualTradeIntent
    /** OS-confirmed ActivityResult callback only, not emitted from Compose UI. */
    data object SaveAfterCredential : ManualTradeIntent
    data object Cancel : ManualTradeIntent
}

/**
 * Only one explicit user trade can be pending. No SavedStateHandle,
 * external storage or network. Review is cleared on OS lock/background.
 */
@HiltViewModel
class ManualTradeViewModel @Inject constructor(
    private val vault: VaultRepository,
    private val session: VaultSessionGate,
) : ViewModel() {
    private val mutableState = MutableStateFlow<ManualTradeUiState>(ManualTradeUiState.Locked)
    val state: StateFlow<ManualTradeUiState> = mutableState
    private var job: Job? = null
    private var generation = 0L

    fun onIntent(intent: ManualTradeIntent) {
        when (intent) {
            ManualTradeIntent.Refresh -> refresh()
            is ManualTradeIntent.Stage -> stage(intent.draft)
            ManualTradeIntent.RequestApproval -> Unit // Activity intercepts
            ManualTradeIntent.SaveAfterCredential -> saveAfterCredential()
            ManualTradeIntent.Cancel -> clearSensitiveState()
        }
    }

    private fun refresh() {
        job?.cancel()
        val request = ++generation
        val revision = session.credentialEpoch()
        if (!session.isCurrentSession(revision)) {
            clearSensitiveState()
            return
        }
        mutableState.value = ManualTradeUiState.Loading
        job = viewModelScope.launch {
            try {
                val book = vault.readBook()
                if (request == generation) {
                    if (session.isCurrentSession(revision)) {
                        mutableState.value = ManualTradeUiState.Ready(book)
                    } else {
                        clearSensitiveState()
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (request == generation) {
                    mutableState.value = if (session.isCurrentSession(revision)) {
                        ManualTradeUiState.Error("Could not load enrolled holdings. Reopen the vault.")
                    } else ManualTradeUiState.Locked
                }
            }
        }
    }

    private fun stage(draft: ManualTradeDraft) {
        val ready = mutableState.value as? ManualTradeUiState.Ready ?: return
        val revision = session.credentialEpoch()
        if (!session.isCurrentSession(revision)) {
            clearSensitiveState()
            return
        }
        try {
            val plan = ManualTradePlanner.prepare(
                ready.book, draft, UUID.randomUUID().toString(),
            )
            mutableState.value = ManualTradeUiState.AwaitingCredential(draft, plan, revision)
        } catch (_: Exception) {
            mutableState.value = ready.copy(notice =
                "Trade cannot be staged. Check enrolled ownership, date, units, price " +
                    "and verified purchase lots before trying again.")
        }
    }

    private fun saveAfterCredential() {
        val pending = mutableState.value as? ManualTradeUiState.AwaitingCredential ?: return
        val revision = session.credentialEpoch()
        if (revision <= pending.stagedRevision ||
            !session.isCurrentApproval(revision)
        ) {
            clearSensitiveState()
            return
        }
        job?.cancel()
        val request = ++generation
        mutableState.value = ManualTradeUiState.Saving
        job = viewModelScope.launch {
            try {
                val latest = vault.readBook()
                check(session.isCurrentApproval(revision)) {
                    "Financial approval expired before book revalidation."
                }
                // Any changed owner/holding/lot history invalidates the staged
                // review, rather than silently accepting a different trade.
                val current = ManualTradePlanner.prepare(
                    latest, pending.draft, pending.plan.event.id,
                )
                check(current == pending.plan) {
                    "Book changed after review; start a new transaction."
                }
                vault.commitManualTrade(pending.draft, current)
                if (request == generation) {
                    if (session.isCurrentSession(revision)) {
                        mutableState.value = ManualTradeUiState.Saved
                    } else clearSensitiveState()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (request == generation) {
                    mutableState.value = if (session.isCurrentSession(revision)) {
                        ManualTradeUiState.Error(
                            "Transaction could not be confirmed. Reload the encrypted " +
                                "book and check recent entries before trying again.",
                        )
                    } else ManualTradeUiState.Locked
                }
            }
        }
    }

    fun clearSensitiveState() {
        ++generation
        job?.cancel()
        job = null
        mutableState.value = ManualTradeUiState.Locked
    }

    override fun onCleared() {
        clearSensitiveState()
        super.onCleared()
    }
}

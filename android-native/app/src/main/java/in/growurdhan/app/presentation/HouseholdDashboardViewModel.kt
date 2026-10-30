package `in`.growurdhan.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.growurdhan.domain.finance.FyTaxEstimate
import `in`.growurdhan.domain.finance.HouseholdDashboard
import `in`.growurdhan.domain.finance.HouseholdDashboardProjector
import `in`.growurdhan.domain.finance.LotEngine
import `in`.growurdhan.domain.finance.OwnerFyTax
import `in`.growurdhan.domain.finance.PortfolioEngine
import `in`.growurdhan.domain.finance.VaultFinanceReview
import `in`.growurdhan.domain.finance.toFinancialEvent
import `in`.growurdhan.domain.finance.toFinancialHolding
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.domain.vault.VaultRepository
import java.time.LocalDate
import org.json.JSONObject
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface HouseholdDashboardUiState {
    data object Locked : HouseholdDashboardUiState
    data object Loading : HouseholdDashboardUiState
    data class Ready(
        val dashboard: HouseholdDashboard,
        val interestReviews: List<VaultFinanceReview.InterestLine> = emptyList(),
        val emiReviews: List<VaultFinanceReview.EmiLine> = emptyList(),
    ) : HouseholdDashboardUiState
    data object Unavailable : HouseholdDashboardUiState
}

/**
 * Read-only presentation boundary. The repository performs an atomic encrypted
 * book read; no sensitive book is cached in SavedStateHandle or preferences.
 */
@HiltViewModel
class HouseholdDashboardViewModel @Inject constructor(
    private val repository: VaultRepository,
    private val session: VaultSessionGate,
) : ViewModel() {
    private val mutableState = MutableStateFlow<HouseholdDashboardUiState>(HouseholdDashboardUiState.Locked)
    val state: StateFlow<HouseholdDashboardUiState> = mutableState
    private var generation = 0L
    private var operation: Job? = null

    /** Revoke financial UI data synchronously, even while an I/O read is suspended. */
    fun lock() {
        generation++
        operation?.cancel()
        operation = null
        mutableState.value = HouseholdDashboardUiState.Locked
    }

    fun refresh() {
        if (!session.isUnlocked()) {
            lock()
            return
        }
        operation?.cancel()
        val request = ++generation
        val originalRevision = session.credentialEpoch()
        mutableState.value = HouseholdDashboardUiState.Loading
        operation = viewModelScope.launch {
            try {
                check(session.isCurrentSession(originalRevision))
                val book = repository.readBook()
                check(session.isCurrentSession(originalRevision))
                val holdingProfiles = repository.holdingProfiles()
                check(session.isCurrentSession(originalRevision))
                val asOf = LocalDate.now()
                val profilesById = holdingProfiles.associateBy { it.holdingId }
                val financialHoldings = book.holdings.map { holding ->
                    val profile = profilesById[holding.id]
                    holding.toFinancialHolding(
                        subCategory = profile?.subCategory.orEmpty(),
                        goldHoldMode = profile?.detailsJson?.let(::goldHoldModeOrNull),
                        acquiredOn = profile?.acquiredOn?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                    ).copy(openingCostPerUnit = profile?.verifiedCostPerUnit)
                }
                val financialEvents = book.events.map { it.toFinancialEvent() }
                val report = PortfolioEngine.derive(
                    holdings = financialHoldings,
                    events = financialEvents,
                    asOf = asOf,
                    knownMembers = book.members.map { it.id },
                    enforceKnownMembers = true,
                )
                if (request != generation) return@launch
                if (!session.isCurrentSession(originalRevision)) {
                    lock()
                    return@launch
                }
                val dashboard = HouseholdDashboardProjector.from(
                    report, book.members.associate { it.id to it.displayName },
                    profiles = holdingProfiles,
                ).copy(taxEstimates = taxEstimates(
                    book.members.map { it.id }, financialHoldings, financialEvents,
                    holdingProfiles, asOf,
                ))
                val interestReviews = VaultFinanceReview.interest(financialHoldings, holdingProfiles, asOf)
                val emiReviews = VaultFinanceReview.emi(financialHoldings, holdingProfiles, asOf)
                if (request != generation) return@launch
                if (session.isCurrentSession(originalRevision)) {
                    mutableState.value = HouseholdDashboardUiState.Ready(
                        dashboard, interestReviews, emiReviews,
                    )
                } else {
                    lock()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (request != generation) return@launch
                mutableState.value = if (session.isCurrentSession(originalRevision))
                    HouseholdDashboardUiState.Unavailable else HouseholdDashboardUiState.Locked
            }
        }
    }
}

private fun taxEstimates(
    members: List<`in`.growurdhan.core.model.OwnerId>,
    holdings: List<`in`.growurdhan.domain.finance.FinancialHolding>,
    events: List<`in`.growurdhan.domain.finance.FinancialEvent>,
    profiles: List<`in`.growurdhan.core.model.HoldingProfileSnapshot>,
    asOf: LocalDate,
): List<OwnerFyTax> {
    val year = LotEngine.fiscalYear(asOf)
    val fmv = profiles.mapNotNull { profile ->
        fmvOrNull(profile.richMetadataJson)?.let { profile.holdingId to it }
    }.toMap()
    return members.map { owner ->
        OwnerFyTax(owner, FyTaxEstimate.summarize(
            holdings = holdings.filter { it.ownerId == owner },
            events = events.filter { it.ownerId == owner },
            financialYear = year,
            asOf = asOf,
            fmvByHolding = fmv,
        ))
    }
}

private fun goldHoldModeOrNull(detailsJson: String): String? = try {
    JSONObject(detailsJson).optString("goldHoldMode").ifBlank { null }
} catch (_: Exception) {
    null
}

private fun fmvOrNull(richMetadataJson: String): MoneyScaled? = try {
    val json = JSONObject(richMetadataJson)
    if (!json.has("fmv_31jan2018_scaled")) null else MoneyScaled(json.getLong("fmv_31jan2018_scaled"))
} catch (_: Exception) {
    null
}

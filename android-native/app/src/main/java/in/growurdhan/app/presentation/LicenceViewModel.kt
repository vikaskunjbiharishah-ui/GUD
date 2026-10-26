package `in`.growurdhan.app.presentation

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.growurdhan.app.billing.AnnualPlay
import `in`.growurdhan.domain.commercial.LicenceGate
import `in`.growurdhan.domain.commercial.PurchaseVerificationEvidence
import `in`.growurdhan.domain.finance.Licence
import `in`.growurdhan.domain.finance.LicenceState
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface TrialMemory {
    fun read(): String?
    fun write(iso: String)
}

interface InstallFacts {
    val fromPlay: Boolean
    val debugTrialAllowed: Boolean
}

@Singleton
class PrefsTrialMemory @Inject constructor(
    @ApplicationContext context: Context,
) : TrialMemory {
    private val prefs = context.getSharedPreferences("gud.licence", Context.MODE_PRIVATE)

    override fun read(): String? = prefs.getString("trialStartedAt", null)

    override fun write(iso: String) {
        prefs.edit().putString("trialStartedAt", iso).apply()
    }
}

@Singleton
class PackageInstallFacts @Inject constructor(
    @ApplicationContext private val context: Context,
) : InstallFacts {
    override val fromPlay: Boolean get() = isPlayInstaller(context)
    override val debugTrialAllowed: Boolean
        get() = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    companion object {
        fun isPlayInstaller(context: Context): Boolean {
            val installer = if (Build.VERSION.SDK_INT >= 30) {
                val info = context.packageManager.getInstallSourceInfo(context.packageName)
                info.installingPackageName ?: info.initiatingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
            return installer == "com.android.vending" || installer == "com.google.android.feedback"
        }
    }
}

data class LicenceUiState(
    val licence: LicenceState = LicenceState(
        plan = "expired",
        subscribed = false,
        daysLeft = 0,
        sideloadBlocked = true,
    ),
    val checking: Boolean = true,
    val busy: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class LicenceViewModel @Inject constructor(
    private val clock: TrialMemory,
    private val install: InstallFacts,
    private val play: AnnualPlay,
) : ViewModel() {
    private val stateFlow = MutableStateFlow(LicenceUiState())
    val state: StateFlow<LicenceUiState> = stateFlow
    private var provisional: String? = null

    init {
        apply(emptyList(), commit = false)
        refresh()
    }

    fun refresh() {
        stateFlow.value = stateFlow.value.copy(error = null)
        play.load { evidence -> apply(evidence, commit = true) }
    }

    fun purchase() {
        val current = stateFlow.value.licence
        if (!Licence.canPurchaseAnnual(current)) {
            stateFlow.value = stateFlow.value.copy(
                error = if (current.subscribed) {
                    "The ₹99 yearly plan is already on this account."
                } else {
                    "This copy cannot buy the ₹99 yearly plan."
                },
            )
            return
        }
        stateFlow.value = stateFlow.value.copy(busy = true, error = null)
        play.purchase { error ->
            if (error != null) stateFlow.value = stateFlow.value.copy(busy = false, error = error)
            else refresh()
        }
    }

    private fun apply(evidence: List<PurchaseVerificationEvidence>, commit: Boolean) {
        val stored = clock.read()
        val resolution = LicenceGate.resolve(
            evidence,
            stored ?: provisional,
            install.fromPlay,
            install.debugTrialAllowed,
            Instant.now(),
        )
        val start = resolution.state.trialStartedAt
        if (stored == null && start != null && resolution.state.plan == "trial" && !resolution.state.subscribed) {
            if (commit) clock.write(start) else provisional = start
        }
        stateFlow.value = LicenceUiState(
            licence = resolution.state,
            checking = !commit,
            busy = false,
            error = stateFlow.value.error,
        )
    }
}

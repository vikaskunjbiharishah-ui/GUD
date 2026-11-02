package `in`.growurdhan.domain.preferences

import `in`.growurdhan.core.model.AppearanceSettings
import `in`.growurdhan.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Domain boundary: implementations must never write passwords or financial data here. */
interface AppearanceRepository {
    val settings: Flow<AppearanceSettings>
    suspend fun setTheme(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setSgbUseMarket(enabled: Boolean)
    suspend fun setDiagnostics(enabled: Boolean)
    val feedback: Flow<`in`.growurdhan.domain.finance.FeedbackState>
    suspend fun saveFeedback(state: `in`.growurdhan.domain.finance.FeedbackState)
}

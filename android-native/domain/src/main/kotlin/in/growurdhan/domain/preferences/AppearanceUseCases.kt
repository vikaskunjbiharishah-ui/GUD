package `in`.growurdhan.domain.preferences

import `in`.growurdhan.core.model.AppearanceSettings
import `in`.growurdhan.core.model.ThemeMode
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveAppearanceSettings @Inject constructor(
    private val repository: AppearanceRepository,
) {
    operator fun invoke(): Flow<AppearanceSettings> = repository.settings
    fun feedback(): Flow<`in`.growurdhan.domain.finance.FeedbackState> = repository.feedback
}

class UpdateAppearanceSettings @Inject constructor(
    private val repository: AppearanceRepository,
) {
    suspend fun theme(mode: ThemeMode) = repository.setTheme(mode)
    suspend fun dynamicColor(enabled: Boolean) = repository.setDynamicColor(enabled)
    suspend fun sgbUseMarket(enabled: Boolean) = repository.setSgbUseMarket(enabled)
    suspend fun diagnostics(enabled: Boolean) = repository.setDiagnostics(enabled)
    suspend fun feedback(state: `in`.growurdhan.domain.finance.FeedbackState) = repository.saveFeedback(state)
}

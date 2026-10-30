package `in`.growurdhan.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import `in`.growurdhan.core.model.AppearanceSettings
import `in`.growurdhan.core.model.ThemeMode
import `in`.growurdhan.domain.preferences.AppearanceRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.appearanceDataStore by preferencesDataStore(name = "appearance")

@Singleton
class DataStoreAppearanceRepository @Inject constructor(
    @ApplicationContext context: Context,
) : AppearanceRepository {
    private val store = context.appearanceDataStore

    override val settings: Flow<AppearanceSettings> = store.data
        .catch { failure ->
            if (failure is IOException) emit(emptyPreferences()) else throw failure
        }
        .map { values ->
            AppearanceSettings(
                theme = ThemeMode.fromStorage(values[THEME]),
                dynamicColor = values[DYNAMIC] ?: false,
                sgbUseMarket = values[SGB_MARKET] ?: false,
                diagnosticsEnabled = values[DIAGNOSTICS] ?: false,
            )
        }

    override suspend fun setTheme(mode: ThemeMode) {
        store.edit { it[THEME] = mode.storageKey }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        store.edit { it[DYNAMIC] = enabled }
    }

    override suspend fun setSgbUseMarket(enabled: Boolean) {
        store.edit { it[SGB_MARKET] = enabled }
    }

    override suspend fun setDiagnostics(enabled: Boolean) {
        store.edit { it[DIAGNOSTICS] = enabled }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val SGB_MARKET = booleanPreferencesKey("sgb_use_market")
        val DIAGNOSTICS = booleanPreferencesKey("diagnostics_enabled")
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AppearanceBindings {
    @Binds
    abstract fun bindAppearanceRepository(impl: DataStoreAppearanceRepository): AppearanceRepository
}

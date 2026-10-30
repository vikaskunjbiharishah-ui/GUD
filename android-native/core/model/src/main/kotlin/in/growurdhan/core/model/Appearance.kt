package `in`.growurdhan.core.model

/** Only non-sensitive, optional UI preferences belong in the native DataStore. */
enum class ThemeMode(val storageKey: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.storageKey == value } ?: SYSTEM
    }
}

data class AppearanceSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    /** False uses the RBI price for a certificate SGB. True uses the market close. */
    val sgbUseMarket: Boolean = false,
    /** Local diagnostics stay off until the user opts in. */
    val diagnosticsEnabled: Boolean = false,
)

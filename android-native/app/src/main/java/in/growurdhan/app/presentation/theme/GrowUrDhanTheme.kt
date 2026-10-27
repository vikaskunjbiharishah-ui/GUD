package `in`.growurdhan.app.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import `in`.growurdhan.core.model.AppearanceSettings
import `in`.growurdhan.core.model.ThemeMode

private val EnterpriseTypography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 27.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF3DDC97),
    onPrimary = Color(0xFF003820),
    primaryContainer = Color(0xFF0B6B45),
    onPrimaryContainer = Color(0xFFB8FFD8),
    secondary = Color(0xFFFFC56A),
    onSecondary = Color(0xFF3F2A00),
    secondaryContainer = Color(0xFF6B4A00),
    onSecondaryContainer = Color(0xFFFFE3B3),
    tertiary = Color(0xFFB4C5FF),
    onTertiary = Color(0xFF00297A),
    tertiaryContainer = Color(0xFF27408A),
    onTertiaryContainer = Color(0xFFDCE4FF),
    background = Color(0xFF0C1210),
    onBackground = Color(0xFFE7F2EB),
    surface = Color(0xFF121A17),
    onSurface = Color(0xFFE7F2EB),
    surfaceVariant = Color(0xFF2A3B33),
    onSurfaceVariant = Color(0xFFC5D6CC),
    surfaceContainerLowest = Color(0xFF070C0A),
    surfaceContainerLow = Color(0xFF141E1A),
    surfaceContainer = Color(0xFF18241F),
    surfaceContainerHigh = Color(0xFF22302A),
    surfaceContainerHighest = Color(0xFF2C3B34),
    outline = Color(0xFF8FA399),
    outlineVariant = Color(0xFF3D5147),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFE7F2EB),
    inverseOnSurface = Color(0xFF1A2420),
    inversePrimary = Color(0xFF006B3F),
)

private val Light = lightColorScheme(
    primary = Color(0xFF007A49),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8F5D4),
    onPrimaryContainer = Color(0xFF002113),
    secondary = Color(0xFFB86E00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDB0),
    onSecondaryContainer = Color(0xFF3A2400),
    tertiary = Color(0xFF2F4FA8),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCE4FF),
    onTertiaryContainer = Color(0xFF00164F),
    background = Color(0xFFF3F6F4),
    onBackground = Color(0xFF121C18),
    surface = Color(0xFFF7FAF8),
    onSurface = Color(0xFF121C18),
    surfaceVariant = Color(0xFFDCE8E1),
    onSurfaceVariant = Color(0xFF3C4F45),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFEEF4F0),
    surfaceContainer = Color(0xFFE6EFEA),
    surfaceContainerHigh = Color(0xFFDCE8E2),
    surfaceContainerHighest = Color(0xFFD3E0DA),
    outline = Color(0xFF6C8076),
    outlineVariant = Color(0xFFC3D4CB),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF27332E),
    inverseOnSurface = Color(0xFFEEF4F0),
    inversePrimary = Color(0xFF6EDEAA),
)

@Composable
fun GrowUrDhanTheme(
    preferences: AppearanceSettings,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (preferences.theme) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme = if (preferences.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) Dark else Light

    MaterialTheme(
        colorScheme = scheme,
        typography = EnterpriseTypography,
        content = content,
    )
}

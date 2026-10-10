package `in`.growurdhan.app.presentation.screens

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.growurdhan.app.presentation.AppIntent
import `in`.growurdhan.core.model.AppearanceSettings
import `in`.growurdhan.core.model.ThemeMode

@Composable
fun SettingsScreen(
    appearance: AppearanceSettings,
    onIntent: (AppIntent) -> Unit,
    onOpenVault: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onChangeAppPassword: () -> Unit = {},
    onOpenEmergencyExport: () -> Unit = {},
) {
    val haptics = LocalHapticFeedback.current
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Appearance",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Choose how GrowUrDhan looks. Changes are kept on this device.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                ThemeMode.entries.forEach { option ->
                    val selected = appearance.theme == option
                    val label = when (option) {
                        ThemeMode.SYSTEM -> "Follow device"
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DARK -> "Dark"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onIntent(AppIntent.SelectTheme(option))
                            }
                            .semantics { role = Role.RadioButton }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Security and privacy", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Your books stay on this device. Manage your encrypted vault " +
                        "and read the privacy details without connecting to a server.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onOpenVault,
                        modifier = Modifier.semantics {
                            contentDescription = "Vault settings"
                        },
                    ) { Text("Vault") }
                    OutlinedButton(
                        onClick = onOpenPrivacy,
                        modifier = Modifier.semantics {
                            contentDescription = "Privacy information"
                        },
                    ) { Text("Privacy") }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Commercial release readiness",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "This native build is still in development. Paid access and " +
                        "Gmail account connection are not enabled.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Before a Play release: old account/email recovery compatibility, " +
                        "legacy migration, live prices, verified NPS imports, Gmail OAuth, " +
                        "Play purchase verification, legal approval, device QA " +
                        "and independent security testing must be accepted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Native financial access requires both the local GrowUrDhan " +
                        "app password and Android device credential. The app password " +
                        "is an authorization gate, not the SQLCipher decryption key.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        OutlinedButton(
            onClick = onOpenEmergencyExport,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Export emergency summary (unencrypted CSV)") }

        OutlinedButton(
            onClick = onChangeAppPassword,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Change GrowUrDhan app password") }

        val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Dynamic color", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (dynamicAvailable) "Use your device wallpaper colors."
                        else "Available on Android 12 and newer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = appearance.dynamicColor && dynamicAvailable,
                    enabled = dynamicAvailable,
                    onCheckedChange = { enabled ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onIntent(AppIntent.SetDynamicColor(enabled))
                    },
                )
            }
        }
    }
}

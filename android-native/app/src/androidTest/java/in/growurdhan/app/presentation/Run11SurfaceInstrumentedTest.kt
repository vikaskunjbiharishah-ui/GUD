package `in`.growurdhan.app.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.growurdhan.app.presentation.theme.GrowUrDhanTheme
import `in`.growurdhan.core.model.AppearanceSettings
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import `in`.growurdhan.core.model.StoredStatementImport
import `in`.growurdhan.core.model.ThemeMode
import `in`.growurdhan.domain.finance.HouseholdDashboard
import `in`.growurdhan.domain.finance.VaultImportHistory
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** P17: Home's Records route opens the saved statement. The registrar is not guessed. */
@RunWith(AndroidJUnit4::class)
class Run11SurfaceInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun recordsRouteOpensTheSavedStatement() {
        val owner = OwnerId("self")
        val rows = listOf(StoredStatementImport(
            "snap-self", owner, "a".repeat(64), "2026-10-01", 1,
        ))
        val ready = HouseholdDashboardUiState.Ready(
            dashboard = HouseholdDashboard(
                asOf = LocalDate.parse("2026-11-03"),
                assets = MoneyScaled(0),
                liabilities = MoneyScaled(0),
                netWorth = MoneyScaled(0),
                memberCount = 1,
                holdingCount = 0,
                unknownCostHoldings = 0,
                integrityWarnings = emptyList(),
                byMember = emptyList(),
            ),
            importBatches = VaultImportHistory.batches(rows),
            importRecords = VaultImportHistory.records(rows),
        )
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings(ThemeMode.LIGHT, false)) {
                GrowUrDhanShell(
                    state = AppUiState(),
                    onIntent = {},
                    dashboardState = ready,
                )
            }
        }
        compose.onNodeWithContentDescription("Open records").performScrollTo().performClick()
        compose.onNodeWithText("Saved statement · 2026-10-01 · 1 line(s)").assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText(
            "No statement, tax note, or record is loaded for this view.",
        ).fetchSemanticsNodes().size)
    }
}

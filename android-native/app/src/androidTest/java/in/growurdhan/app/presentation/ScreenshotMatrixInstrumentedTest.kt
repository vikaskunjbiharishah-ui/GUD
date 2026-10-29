package `in`.growurdhan.app.presentation

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import `in`.growurdhan.app.presentation.screens.InsightsScreen
import `in`.growurdhan.app.presentation.screens.LicenceScreen
import `in`.growurdhan.app.presentation.screens.OnboardingScreen
import `in`.growurdhan.app.presentation.screens.TermsGateScreen
import `in`.growurdhan.app.presentation.screens.asBook
import `in`.growurdhan.app.presentation.theme.GrowUrDhanTheme
import `in`.growurdhan.core.model.AppearanceSettings
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.ThemeMode
import `in`.growurdhan.domain.finance.CategoryDashboard
import `in`.growurdhan.domain.finance.HouseholdDashboard
import `in`.growurdhan.domain.finance.Licence
import `in`.growurdhan.domain.finance.LicenceState
import `in`.growurdhan.domain.finance.RecordedHoldingDashboard
import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Q7 design gate: light/dark and 200% font on a 360dp-wide window when the
 * harness sets `wm size 1080x1920` and `wm density 480` before launch.
 * Q9: the licence screen offers one yearly price and no second product.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotMatrixInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun onboardingEnglishLightKeepsTheFirstSlide() {
        show(dark = false, fontScale = 1f) { OnboardingScreen(onFinished = {}) }
        compose.onNodeWithText("A family vault. Not another brokerage login.").assertIsDisplayed()
        assertTouch("Next")
        assertTouch("हिन्दी")
        save("onboarding-en-light-font1")
    }

    @Test fun onboardingHindiStaysReadableAtDoubleFont() {
        show(dark = false, fontScale = 2f) { OnboardingScreen(onFinished = {}) }
        compose.onNodeWithText("हिन्दी").performClick()
        compose.onNodeWithText("परिवार की तिजोरी। ब्रोकर का लॉगिन नहीं।").assertIsDisplayed()
        assertTouch("English")
        assertTouch("Next")
        save("onboarding-hi-light-font2")
    }

    @Test fun licenceScreenOffersOnlyTheYearlyPlan() {
        val trial = LicenceUiState(
            licence = LicenceState(
                plan = "trial",
                subscribed = false,
                daysLeft = 12,
                sideloadBlocked = false,
            ),
            checking = false,
        )
        show(dark = false, fontScale = 1f) {
            LicenceScreen(state = trial, onPurchase = {}, onRestore = {})
        }
        assertYearlyCopy()
        save("licence-trial-light-font1")
    }

    @Test fun licenceScreenStaysOnePlanAtDoubleFontInDark() {
        val trial = LicenceUiState(
            licence = LicenceState(
                plan = "trial",
                subscribed = false,
                daysLeft = 12,
                sideloadBlocked = false,
            ),
            checking = false,
        )
        show(dark = true, fontScale = 2f) {
            LicenceScreen(state = trial, onPurchase = {}, onRestore = {})
        }
        assertYearlyCopy()
        compose.onNodeWithText("Subscribe ₹99 / year").performScrollTo().assertIsDisplayed()
        assertTouch("Subscribe ₹99 / year")
        save("licence-trial-dark-font2")
    }

    @Test fun termsGateButtonsStayTappableInDarkLargeType() {
        show(dark = true, fontScale = 2f) {
            TermsGateScreen(onAccept = {}, onDecline = {})
        }
        compose.onNodeWithText("GrowUrDhan Terms and Conditions").assertIsDisplayed()
        compose.onNodeWithText("Don't accept").assertIsDisplayed()
        assertTouch("Don't accept")
        assertTouch("Accept")
        save("terms-dark-font2")
    }

    @Test fun insightsAmountsStayOnScreenAtDoubleFont() {
        val assets = MoneyScaled(300_000)
        show(dark = true, fontScale = 2f) {
            InsightsScreen(
                state = HouseholdDashboardUiState.Ready(sampleDashboard(assets)),
                onOpenVault = {},
                onRefresh = {},
            )
        }
        val amount = "Assets: ${assets.asBook()}"
        compose.onNodeWithText(amount).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("MUTUAL FUND · 66.7%").performScrollTo().assertIsDisplayed()
        assertTouch("MUTUAL FUND · 66.7%")
        val root = compose.onNodeWithTag("shot").fetchSemanticsNode().boundsInRoot
        val line = compose.onNodeWithText(amount).fetchSemanticsNode().boundsInRoot
        assertTrue("amount clipped ${line.right} > ${root.right}", line.right <= root.right + 1f)
        assertTrue(line.left >= root.left - 1f)
        save("insights-dark-font2")
    }

    private fun assertYearlyCopy() {
        assertEquals("gud_annual", Licence.SKU_ANNUAL)
        assertEquals(99, Licence.PRICE_ANNUAL)
        compose.onNodeWithText("One yearly plan").assertIsDisplayed()
        compose.onNodeWithText("Thirty days free, then ₹99 a year. That is the only plan.", substring = true)
            .assertIsDisplayed()
        compose.onNodeWithText("There is no lifetime licence and no second product.", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("gud_lifetime").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("gud_gmail").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("gud_support_annual").fetchSemanticsNodes().size)
    }

    private fun assertTouch(label: String) {
        val node = compose.onNodeWithText(label).fetchSemanticsNode()
        val minPx = with(compose.density) { 48.dp.toPx() }
        val bounds = node.boundsInRoot
        assertTrue(
            "$label touch ${bounds.width}x${bounds.height}px under 48dp ($minPx)",
            bounds.width + 0.5f >= minPx && bounds.height + 0.5f >= minPx,
        )
    }

    private fun show(dark: Boolean, fontScale: Float, content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent {
            val base = LocalDensity.current
            GrowUrDhanTheme(
                AppearanceSettings(if (dark) ThemeMode.DARK else ThemeMode.LIGHT, false),
            ) {
                CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                    Box(Modifier.fillMaxSize().testTag("shot")) { content() }
                }
            }
        }
    }

    private fun save(name: String) {
        val bitmap = compose.onNodeWithTag("shot").captureToImage().asAndroidBitmap()
        val dir = InstrumentationRegistry.getInstrumentation()
            .targetContext.getExternalFilesDir("screens")
            ?: error("no screens dir")
        dir.mkdirs()
        File(dir, "$name.png").outputStream().use {
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        File(dir, "$name.txt").writeText("${bitmap.width}x${bitmap.height}\n")
    }

    private fun sampleDashboard(assets: MoneyScaled) = HouseholdDashboard(
        asOf = LocalDate.parse("2026-10-10"),
        assets = assets,
        liabilities = MoneyScaled(100_000),
        netWorth = MoneyScaled(200_000),
        memberCount = 2,
        holdingCount = 3,
        unknownCostHoldings = 1,
        integrityWarnings = emptyList(),
        byMember = emptyList(),
        byCategory = listOf(
            CategoryDashboard("MUTUAL_FUND", MoneyScaled(200_000), 1),
            CategoryDashboard("GOLD", MoneyScaled(100_000), 1),
        ),
        recordedHoldings = listOf(
            RecordedHoldingDashboard(
                "Synthetic private fund", "MUTUAL_FUND", "Primary",
                100_000_000L, MoneyScaled(200_000), null, true, false,
            ),
            RecordedHoldingDashboard(
                "Synthetic gold holding", "GOLD", "Relative",
                100_000_000L, MoneyScaled(100_000), MoneyScaled(5_000), false, false,
            ),
            RecordedHoldingDashboard(
                "Synthetic housing debt", "LIABILITY", "Primary",
                100_000_000L, MoneyScaled(100_000), null, false, true,
            ),
        ),
    )
}

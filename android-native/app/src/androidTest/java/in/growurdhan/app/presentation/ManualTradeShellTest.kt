package `in`.growurdhan.app.presentation

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.growurdhan.app.presentation.screens.ManualTradeScreen
import `in`.growurdhan.core.model.*
import `in`.growurdhan.domain.finance.*
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManualTradeShellTest {
    @get:Rule val compose = createComposeRule()
    private val owner = OwnerId("test-member")
    private val holding = Holding(
        HoldingId("safe-id"), owner, null, "Synthetic index asset",
        "MUTUAL_FUND", QuantityScaled(0), MoneyScaled(100_000),
    )
    private val book = VaultBook(
        listOf(FamilyMember(owner, "Primary", "Self", true)),
        listOf(holding), emptyList(),
    )

    @Test fun publicComposeFormStagesOnlyAndCannotWriteWithoutActivityApproval() {
        var request: ManualTradeIntent? = null
        compose.setContent {
            ManualTradeScreen(
                ManualTradeUiState.Ready(book),
                onIntent = { request = it }, onReturn = {},
            )
        }
        compose.onNodeWithTag("ManualTradeUnits").performScrollTo().performTextInput("2")
        compose.onNodeWithTag("ManualTradePrice").performScrollTo().performTextInput("100")
        compose.onNodeWithText("Review recorded transaction")
            .performScrollTo().performClick()
        val staged = request as ManualTradeIntent.Stage
        assertEquals(holding.id, staged.draft.holdingId)
        assertEquals("2", staged.draft.quantity)
        assertEquals("100", staged.draft.unitPriceInr)
        compose.onNodeWithText("Confirm transaction with device lock").assertDoesNotExist()
    }

    @Test fun verifiedReviewDisplaysAmountsBeforeOsChallengeButton() {
        val plan = ManualTradePlanner.prepare(
            book, ManualTradeDraft(holding.id, ManualTradeKind.BUY,
                "2026-10-10", "1", "100", "2"),
            "synthetic-trade", LocalDate.parse("2026-10-10"),
        )
        var request: ManualTradeIntent? = null
        compose.setContent {
            ManualTradeScreen(
                ManualTradeUiState.AwaitingCredential(
                    ManualTradeDraft(holding.id, ManualTradeKind.BUY,
                        "2026-10-10", "1", "100", "2"),
                    plan, 1L,
                ),
                onIntent = { request = it }, onReturn = {},
            )
        }
        compose.onNodeWithText("Total paid: 102.0000 INR")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Confirm transaction with device lock")
            .performScrollTo().performClick()
        assertEquals(ManualTradeIntent.RequestApproval, request)
    }

    @Test fun lockedPageNeverRendersHoldingNameOrValue() {
        compose.setContent {
            ManualTradeScreen(ManualTradeUiState.Locked, {}, {})
        }
        compose.onNodeWithText("Synthetic index asset").assertDoesNotExist()
        compose.onNodeWithText("Unlock the vault before recording a financial transaction.")
            .assertIsDisplayed()
    }
}

package `in`.growurdhan.app.presentation

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.growurdhan.app.presentation.theme.GrowUrDhanTheme
import `in`.growurdhan.app.presentation.screens.HoldingProfileEditor
import `in`.growurdhan.core.model.*
import `in`.growurdhan.domain.market.CachedListedSecurity
import `in`.growurdhan.domain.market.OfficialMarketSource
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustodyHoldingShellTest {
    @get:Rule val compose = createComposeRule()
    private val owner = OwnerId("self")
    private val member = FamilyMember(owner, "Synthetic Primary", "Self", true)
    private val account = CustodyAccount("a1", owner, "CAMS", "Synthetic registrar")
    private fun book(accounts: List<CustodyAccount> = emptyList()): VaultBook =
        VaultBook(listOf(member), emptyList(), emptyList(), accounts)

    private val opened = VaultUiState.Ready(VaultHealth("ok", 0, 1, 0, 0))

    @Test fun vaultOpensSecondaryRegistrarEnrollmentNotAnotherBottomTab() {
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings()) {
                GrowUrDhanShell(
                    state = AppUiState(),
                    onIntent = {},
                    vaultState = opened,
                    custodyState = CustodyHoldingUiState.Ready(book()),
                )
            }
        }
        compose.onNodeWithText("Open vault").performScrollTo().performClick()
        compose.onNodeWithText("Manage accounts & holdings").performScrollTo().performClick()
        compose.onNodeWithText("Accounts & holdings").assertIsDisplayed()
        // The guidance includes a follow-up explanation in the same Text node.
        // Match the stable instruction without requiring an exact full-string match.
        compose.onNodeWithText(
            "Save a CAMS or KFINTECH registrar account first.", substring = true,
        ).performScrollTo().assertIsDisplayed()
    }

    @Test fun accountNeedsNicknameAndOnlyStagesUserRequestedWrite() {
        var action: CustodyHoldingIntent? = null
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings()) {
                GrowUrDhanShell(
                    state = AppUiState(), onIntent = {}, vaultState = opened,
                    custodyState = CustodyHoldingUiState.Ready(book()),
                    onCustodyIntent = { action = it },
                )
            }
        }
        compose.onNodeWithText("Open vault").performScrollTo().performClick()
        compose.onNodeWithText("Manage accounts & holdings").performScrollTo().performClick()
        compose.onNodeWithText("Save registrar account securely")
            .performScrollTo().assertIsNotEnabled()
        compose.onNodeWithContentDescription("Registrar account nickname input")
            .performScrollTo().performTextInput("Synthetic nickname")
        compose.onNodeWithText("Save registrar account securely")
            .performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(
                CustodyHoldingIntent.StageAccount(owner, "CAMS", "Synthetic nickname"), action,
            )
        }
    }

    @Test fun enrolledFundFormStagesExactIsinAndFolioWithoutLedgerMutation() {
        var action: CustodyHoldingIntent? = null
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings()) {
                GrowUrDhanShell(
                    state = AppUiState(), onIntent = {}, vaultState = opened,
                    custodyState = CustodyHoldingUiState.Ready(book(listOf(account))),
                    onCustodyIntent = { action = it },
                )
            }
        }
        compose.onNodeWithText("Open vault").performScrollTo().performClick()
        compose.onNodeWithText("Manage accounts & holdings").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Mutual fund scheme name input")
            .performScrollTo().performTextInput("Synthetic Index")
        compose.onNodeWithContentDescription("CAS ISIN input")
            .performScrollTo().performTextInput("INF000000001")
        compose.onNodeWithContentDescription("CAS folio input")
            .performScrollTo().performTextInput("12345")
        compose.onNodeWithText("Enrol mutual fund securely").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(
                CustodyHoldingIntent.StageHolding(
                    owner, account.id, "Synthetic Index", "INF000000001", "12345",
                ), action,
            )
        }
    }

    @Test fun statementWithoutEnrolledIdentityOffersRealSetupRoute() {
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings()) {
                GrowUrDhanShell(
                    state = AppUiState(), onIntent = {},
                    reviewState = StatementReviewUiState.MissingCasMapping,
                    custodyState = CustodyHoldingUiState.Ready(book()),
                )
            }
        }
        compose.onNodeWithContentDescription("Statements").performClick()
        compose.onNodeWithText("Enrol your first CAS holding").assertIsDisplayed()
        compose.onNodeWithText("Add account and holding").performClick()
        compose.onNodeWithText("Accounts & holdings").assertIsDisplayed()
    }

    @Test fun fullyDetailedCreateStagesOneDraftWithoutWritingAnyLedgerRow() {
        var action: CustodyHoldingIntent? = null
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings()) {
                CustodyHoldingScreen(
                    state = CustodyHoldingUiState.Ready(book()),
                    onIntent = { action = it },
                    onBackToVault = {},
                    onContinueToStatements = {},
                )
            }
        }
        compose.onNodeWithContentDescription("Manual asset nickname input")
            .performScrollTo().performTextInput("Synthetic Holdings")
        compose.onNodeWithTag("AdvancedHoldingCreateToggle")
            .performScrollTo().performClick()
        compose.onNodeWithText("Holding details").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Review and register complete holding")
            .performScrollTo().performClick()
        compose.runOnIdle {
            val draft = action as? CustodyHoldingIntent.StageAsset
            assertEquals("Synthetic Holdings", draft?.name)
            assertEquals("EQUITY", draft?.category)
            assertEquals("Large cap", draft?.subCategory)
            assertEquals(draft?.ownerId, draft?.initialProfile?.ownerId)
        }
    }

    @Test fun holdingGoalPickerStagesOwnerScopedAssociationOnly() {
        var action: CustodyHoldingIntent? = null
        val held = Holding(HoldingId("holding-for-goal"), owner, null, "Synthetic equity",
            "EQUITY", QuantityScaled(0), MoneyScaled(0))
        val goal = FamilyGoalProfile(
            "g1", owner, "Education", null, null, null, emptyList(), MoneyScaled(0),
        )
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings()) {
                CustodyHoldingScreen(
                    state = CustodyHoldingUiState.Ready(
                        book().copy(holdings = listOf(held)),
                        profiles = listOf(HoldingProfileSnapshot(
                            held.id, owner, held.name, "EQUITY", subCategory = "Large cap",
                        )),
                        goals = listOf(goal),
                    ),
                    onIntent = { action = it },
                    onBackToVault = {},
                    onContinueToStatements = {},
                )
            }
        }
        compose.onNodeWithText("Details / edit").performScrollTo().performClick()
        compose.onNodeWithTag("HoldingGoal_g1").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(CustodyHoldingIntent.StageGoalTag(
                owner, held.id, goal.id, true,
            ), action)
        }
    }

    @Test fun newlySavedRegistrationOffersExplicitOpeningBalanceRoute() {
        var opened = false
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings()) {
                CustodyHoldingScreen(
                    state = CustodyHoldingUiState.Saved(
                        "Synthetic holding registered.", HoldingId("created-stub")),
                    onIntent = {}, onBackToVault = {},
                    onContinueToStatements = {},
                    onRecordOpeningBalance = { opened = true },
                )
            }
        }
        compose.onNodeWithText("Record opening balance or debt")
            .performScrollTo().performClick()
        compose.runOnIdle { org.junit.Assert.assertTrue(opened) }
    }

    @Test fun savedHoldingRouteCarriesExactNewHoldingIdWithoutWritingLedger() {
        val created = HoldingId("synthetic-new-id")
        var routed: HoldingId? = null
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings()) {
                CustodyHoldingScreen(
                    state = CustodyHoldingUiState.Saved(
                        "Synthetic newly enrolled asset", createdHoldingId = created),
                    onIntent = {}, onBackToVault = {},
                    onContinueToStatements = {},
                    onRecordOpeningBalance = { routed = it },
                )
            }
        }
        compose.onNodeWithText("Record opening balance or debt")
            .performScrollTo().performClick()
        compose.runOnIdle { assertEquals(created, routed) }
    }

    @Test fun verifiedDatedOfflineSecurityChoiceStagesIdentityAndProvenance() {
        val priceDay = java.time.LocalDate.now().minusDays(1)
        val cached = CachedListedSecurity(
            "INE000000001", "Synthetic Market Share", "EQUITY",
            OfficialMarketSource.NSE, priceDay, MoneyScaled(1_230_000),
            symbol = "SYNTH",
        )
        var action: CustodyHoldingIntent? = null
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings()) {
                CustodyHoldingScreen(
                    state = CustodyHoldingUiState.Ready(book()),
                    onIntent = { action = it }, onBackToVault = {},
                    onContinueToStatements = {},
                    verifiedOfficialCatalog = listOf(cached),
                )
            }
        }
        compose.onNodeWithContentDescription("Manual asset nickname input")
            .performScrollTo().performTextInput("Temporary nickname")
        compose.onNodeWithTag("AdvancedHoldingCreateToggle")
            .performScrollTo().performClick()
        compose.onNodeWithTag("OfficialSecuritySearch")
            .performScrollTo().performTextInput("Synthetic Market")
        compose.onNodeWithText("Synthetic Market Share", substring = true)
            .performScrollTo().performClick()
        compose.onNodeWithText("Review and register complete holding")
            .performScrollTo().performClick()
        compose.runOnIdle {
            val staged = action as CustodyHoldingIntent.StageAsset
            assertEquals(cached.name, staged.name)
            assertEquals(cached.isin, staged.initialProfile?.isin)
            assertEquals(priceDay.toString(), staged.initialProfile?.priceAsOf)
            assertEquals("NSE_OFFICIAL_CLOSE", staged.initialProfile?.priceSource)
        }
    }

    @Test fun staleOfflineSecurityChoiceCopiesIdentityButNotPrice() {
        val cached = CachedListedSecurity(
            "INE000000001", "Synthetic Stale Share", "EQUITY",
            OfficialMarketSource.BSE, java.time.LocalDate.now().minusDays(60),
            MoneyScaled(1_230_000),
        )
        var action: CustodyHoldingIntent? = null
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings()) {
                CustodyHoldingScreen(
                    state = CustodyHoldingUiState.Ready(book()),
                    onIntent = { action = it }, onBackToVault = {},
                    onContinueToStatements = {},
                    verifiedOfficialCatalog = listOf(cached),
                )
            }
        }
        compose.onNodeWithContentDescription("Manual asset nickname input")
            .performScrollTo().performTextInput("Temporary nickname")
        compose.onNodeWithTag("AdvancedHoldingCreateToggle")
            .performScrollTo().performClick()
        compose.onNodeWithTag("OfficialSecuritySearch")
            .performScrollTo().performTextInput("Synthetic Stale")
        compose.onNodeWithText("stale, price not used", substring = true)
            .performScrollTo().performClick()
        compose.onNodeWithText("Review and register complete holding")
            .performScrollTo().performClick()
        compose.runOnIdle {
            val staged = action as CustodyHoldingIntent.StageAsset
            assertEquals(cached.isin, staged.initialProfile?.isin)
            assertEquals(null, staged.initialProfile?.customPrice)
            assertEquals(null, staged.initialProfile?.priceSource)
        }
    }

    @Test fun savedAccountNeverRoutesIntoUnrelatedHoldingOpening() {
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings()) {
                CustodyHoldingScreen(
                    state = CustodyHoldingUiState.Saved("Account saved, no asset yet."),
                    onIntent = {}, onBackToVault = {},
                    onContinueToStatements = {},
                    onRecordOpeningBalance = {
                        throw AssertionError("No holding exists to record a balance.")
                    },
                )
            }
        }
        compose.onNodeWithText("Record opening balance or debt")
            .assertDoesNotExist()
    }

    @Test fun aifCommitmentRupeesAreSavedAsExactScaledIntegerString() {
        var staged: HoldingProfileEdit? = null
        compose.setContent {
            GrowUrDhanTheme(AppearanceSettings()) {
                HoldingProfileEditor(
                    profile = HoldingProfileSnapshot(
                        HoldingId("draft-aif"), owner, "Synthetic AIF",
                        "SPECIALISED", "AIF",
                    ),
                    creating = true,
                    onStage = { staged = it },
                )
            }
        }
        compose.onNodeWithTag("HoldingDetails_aifCategory")
            .performScrollTo().performTextInput("II")
        compose.onNodeWithTag("HoldingDetails_commitmentMinor")
            .performScrollTo().performTextInput("12500.1234")
        compose.onNodeWithText("Review and register complete holding")
            .performScrollTo().performClick()
        compose.runOnIdle {
            val json = org.json.JSONObject(staged!!.detailsJson)
            assertEquals("II", json.getString("aifCategory"))
            assertEquals("125001234", json.getString("commitmentMinor"))
        }
    }
}

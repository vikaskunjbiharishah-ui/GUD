package `in`.growurdhan.app.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.FilterChip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import `in`.growurdhan.core.model.OwnerId
import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.app.market.OfficialAmfiCatalogGateway
import `in`.growurdhan.domain.market.CachedListedSecurity
import `in`.growurdhan.app.presentation.screens.BackupFolderScreen
import `in`.growurdhan.app.presentation.screens.PortableRecoveryIntent
import `in`.growurdhan.app.presentation.screens.PortableRecoveryScreen
import `in`.growurdhan.app.presentation.screens.CustodyHoldingScreen
import `in`.growurdhan.app.presentation.screens.FamilyMemberScreen
import `in`.growurdhan.app.presentation.screens.FamilyAdministrationScreen
import `in`.growurdhan.app.presentation.screens.HomeScreen
import `in`.growurdhan.app.presentation.screens.PortfolioScreen
import `in`.growurdhan.app.presentation.screens.ManualTradeScreen
import `in`.growurdhan.app.presentation.screens.InsightsScreen
import `in`.growurdhan.app.presentation.screens.TaxOwnerScopeScreen
import `in`.growurdhan.app.presentation.screens.StatementReviewScreen
import `in`.growurdhan.app.presentation.screens.PrivacyScreen
import `in`.growurdhan.app.gmail.GmailUiState
import `in`.growurdhan.app.presentation.screens.LicenceScreen
import `in`.growurdhan.app.presentation.screens.SettingsScreen
import `in`.growurdhan.app.presentation.screens.VaultScreen
import `in`.growurdhan.app.presentation.screens.LegacyBackupScreen
import `in`.growurdhan.app.presentation.screens.LegacyBackupUiState
import `in`.growurdhan.app.presentation.screens.LegacyBackupIntent
import `in`.growurdhan.app.presentation.screens.ChangeLocalPasswordScreen
import `in`.growurdhan.app.presentation.screens.EmergencyExportScreen
import `in`.growurdhan.app.presentation.screens.EmergencyExportUiState

private enum class MainDestination(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    HOME("home", "Home", Icons.Outlined.Home),
    PORTFOLIO("portfolio", "Portfolio", Icons.Outlined.AccountBalanceWallet),
    REVIEW("review", "Statements", Icons.Outlined.Description),
    INSIGHTS("insights", "Insights", Icons.Outlined.BarChart),
    SETTINGS("settings", "Settings", Icons.Outlined.Settings),
}

/** Secondary routes remain reachable from Home and Settings without crowding navigation. */
private const val VAULT_ROUTE = "vault"
private const val PRIVACY_ROUTE = "privacy"
private const val FAMILY_ROUTE = "family"
private const val FAMILY_ADMIN_ROUTE = "family-admin"
private const val ACCOUNTS_ROUTE = "accounts"
private const val TAX_SCOPE_ROUTE = "tax-owner-scope"
private const val DOCUMENTS_ROUTE = "documents"
/** Secondary secure registrar/holding setup route, not an extra bottom tab. */
private const val CUSTODY_ROUTE = "custody"
private const val RECOVERY_ROUTE = "recovery"
private const val MANUAL_TRADE_ROUTE = "manual-trade"
private const val LEGACY_PREVIEW_ROUTE = "legacy-preview"
private const val PASSWORD_CHANGE_ROUTE = "password-change"
private const val EMERGENCY_EXPORT_ROUTE = "emergency-export"
private const val BACKUP_FOLDER_ROUTE = "backup-folder"
private const val LICENCE_ROUTE = "licence"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrowUrDhanShell(
    state: AppUiState,
    onIntent: (AppIntent) -> Unit,
    vaultState: VaultUiState = VaultUiState.Loading,
    dashboardState: HouseholdDashboardUiState = HouseholdDashboardUiState.Locked,
    onDashboardRefresh: () -> Unit = {},
    onVaultIntent: (VaultIntent) -> Unit = {},
    reviewState: StatementReviewUiState = StatementReviewUiState.Empty,
    onReviewIntent: (StatementReviewIntent) -> Unit = {},
    familyState: FamilyMemberUiState = FamilyMemberUiState.Locked,
    onFamilyIntent: (FamilyMemberIntent) -> Unit = {},
    familyAdminState: FamilyAdminUiState = FamilyAdminUiState.Locked,
    onFamilyAdminIntent: (FamilyAdminIntent) -> Unit = {},
    custodyState: CustodyHoldingUiState = CustodyHoldingUiState.Locked,
    onCustodyIntent: (CustodyHoldingIntent) -> Unit = {},
    recoveryState: PortableRecoveryUiState = PortableRecoveryUiState.Empty,
    onRecoveryIntent: (PortableRecoveryIntent) -> Unit = {},
    manualTradeState: ManualTradeUiState = ManualTradeUiState.Locked,
    onManualTradeIntent: (ManualTradeIntent) -> Unit = {},
    legacyBackupState: LegacyBackupUiState = LegacyBackupUiState.Empty,
    onLegacyBackupIntent: (LegacyBackupIntent) -> Unit = {},
    onChangeLocalPassword: (String, String) -> Unit = { _, _ -> },
    dossierStatus: EmergencyExportUiState = EmergencyExportUiState.READY,
    onChooseDossierDestination: () -> Unit = {},
    onCancelDossier: () -> Unit = {},
    backupFolderState: BackupFolderUiState = BackupFolderUiState(),
    onPickBackupFolder: () -> Unit = {},
    onBackupFolderEnabled: (Boolean) -> Unit = {},
    onBackupFolderNow: (CharArray) -> Unit = {},
    onBackupFolderRefresh: () -> Unit = {},
    licenceState: LicenceUiState = LicenceUiState(),
    onLicencePurchase: () -> Unit = {},
    onLicenceRefresh: () -> Unit = {},
    gmail: GmailUiState = GmailUiState(),
    onGmailConnect: () -> Unit = {},
    onGmailDisconnect: (String) -> Unit = {},
) {
    val controller = rememberNavController()
    val currentBackStackEntry by controller.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: MainDestination.HOME.route
    val snackbars = remember { SnackbarHostState() }
    val appContext = LocalContext.current.applicationContext
    val catalogGateway = remember(appContext) { OfficialAmfiCatalogGateway(appContext) }
    val catalogScope = rememberCoroutineScope()
    var officialCatalog by remember { mutableStateOf<List<CachedListedSecurity>>(emptyList()) }
    var catalogLoading by remember { mutableStateOf(false) }
    var catalogMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(currentRoute) {
        if (currentRoute == CUSTODY_ROUTE) {
            officialCatalog = withContext(Dispatchers.IO) { catalogGateway.cached() }
        }
    }
    // One presentation selection shared across Home, Holdings, Accounts, Goals
    // and CAS documents. Never persisted in preferences or SavedStateHandle.
    var selectedOwner by remember { mutableStateOf<OwnerId?>(null) }
    // Ephemeral, owner-approved creation handoff. Never saved across lock/restart.
    var pendingOpeningHoldingId by remember { mutableStateOf<HoldingId?>(null) }
    val visibleMembers = (dashboardState as? HouseholdDashboardUiState.Ready)
        ?.dashboard?.byMember.orEmpty()
    LaunchedEffect(dashboardState) {
        if (dashboardState !is HouseholdDashboardUiState.Ready) selectedOwner = null
    }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage
        if (message != null) {
            snackbars.showSnackbar(message)
            onIntent(AppIntent.DismissError)
        }
    }

    val navigate: (String) -> Unit = { route ->
        controller.navigate(route) {
            popUpTo(MainDestination.HOME.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val extended = maxWidth >= 600.dp
        Row(modifier = Modifier.fillMaxSize()) {
            if (extended) {
                NavigationRail {
                    MainDestination.entries.forEach { destination ->
                        NavigationRailItem(
                            selected = currentRoute == destination.route,
                            onClick = { navigate(destination.route) },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.title) },
                            modifier = Modifier.semantics {
                                contentDescription = destination.title
                            },
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    Column {
                        CenterAlignedTopAppBar(
                            title = { Text("GrowUrDhan") },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                        if (visibleMembers.size > 1) {
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                FilterChip(selectedOwner == null,
                                    onClick = { selectedOwner = null },
                                    label = { Text("All family") })
                                visibleMembers.forEach { member ->
                                    FilterChip(selectedOwner == member.ownerId,
                                        onClick = { selectedOwner = member.ownerId },
                                        label = { Text(member.displayName ?: "Member") })
                                }
                            }
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbars) },
                bottomBar = {
                    if (!extended) {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                            MainDestination.entries.forEach { destination ->
                                NavigationBarItem(
                                    selected = currentRoute == destination.route,
                                    onClick = { navigate(destination.route) },
                                    icon = { Icon(destination.icon, contentDescription = null) },
                                    label = { Text(destination.title) },
                                    modifier = Modifier.semantics {
                                        contentDescription = destination.title
                                    },
                                )
                            }
                        }
                    }
                },
            ) { insets ->
                NavHost(
                    navController = controller,
                    startDestination = MainDestination.HOME.route,
                    modifier = Modifier.fillMaxSize().padding(insets),
                ) {
                    composable(MainDestination.HOME.route) {
                        HomeScreen(
                            dashboardState = dashboardState,
                            onRefreshDashboard = onDashboardRefresh,
                            onOpenSettings = { navigate(MainDestination.SETTINGS.route) },
                            onOpenPrivacy = { navigate(PRIVACY_ROUTE) },
                            onOpenVault = { navigate(VAULT_ROUTE) },
                            onOpenStatements = { navigate(MainDestination.REVIEW.route) },
                            onOpenPortfolio = { navigate(MainDestination.PORTFOLIO.route) },
                            onOpenInsights = { navigate(MainDestination.INSIGHTS.route) },
                            onManageFamily = { navigate(FAMILY_ADMIN_ROUTE) },
                            onOpenAccounts = { navigate(ACCOUNTS_ROUTE) },
                            onOpenTaxScope = { navigate(TAX_SCOPE_ROUTE) },
                            onOpenDocuments = { navigate(DOCUMENTS_ROUTE) },
                            selectedFamilyOwner = selectedOwner,
                            onSelectFamilyOwner = { selectedOwner = it },
                        )
                    }
                    composable(MainDestination.PORTFOLIO.route) {
                        PortfolioScreen(
                            state = dashboardState,
                            onOpenVault = { navigate(VAULT_ROUTE) },
                            onRefresh = onDashboardRefresh,
                            onManageHoldings = { navigate(CUSTODY_ROUTE) },
                            onRecordTrade = {
                                pendingOpeningHoldingId = null
                                navigate(MANUAL_TRADE_ROUTE)
                            },
                            selectedFamilyOwner = selectedOwner,
                            onSelectFamilyOwner = { selectedOwner = it },
                        )
                    }
                    composable(TAX_SCOPE_ROUTE) {
                        TaxOwnerScopeScreen(
                            state = dashboardState,
                            ownerId = selectedOwner,
                            onBack = { navigate(MainDestination.HOME.route) },
                        )
                    }
                    composable(DOCUMENTS_ROUTE) {
                        // Same consent-gated CAS importer, same owner lens as
                        // the Statements destination. No separate file copies.
                        StatementReviewScreen(
                            state = reviewState,
                            onIntent = onReviewIntent,
                            onAddFamilyMember = { navigate(FAMILY_ROUTE) },
                            onSetUpHoldings = { navigate(CUSTODY_ROUTE) },
                            selectedFamilyOwner = selectedOwner,
                            onSelectFamilyOwner = { selectedOwner = it },
                        )
                    }
                    composable(MainDestination.INSIGHTS.route) {
                        InsightsScreen(
                            state = dashboardState,
                            onOpenVault = { navigate(VAULT_ROUTE) },
                            onRefresh = onDashboardRefresh,
                            selectedOwnerId = selectedOwner,
                        )
                    }
                    composable(VAULT_ROUTE) {
                        VaultScreen(
                            state = vaultState,
                            onIntent = onVaultIntent,
                            onAddFamilyMember = { navigate(FAMILY_ROUTE) },
                            onManageHoldings = { navigate(CUSTODY_ROUTE) },
                            onBackupRecovery = { navigate(RECOVERY_ROUTE) },
                            onInspectLegacyBackup = { navigate(LEGACY_PREVIEW_ROUTE) },
                        )
                    }
                    composable(MainDestination.REVIEW.route) {
                        StatementReviewScreen(
                            state = reviewState,
                            onIntent = onReviewIntent,
                            onAddFamilyMember = { navigate(FAMILY_ROUTE) },
                            onSetUpHoldings = { navigate(CUSTODY_ROUTE) },
                            selectedFamilyOwner = selectedOwner,
                            onSelectFamilyOwner = { selectedOwner = it },
                        )
                    }
                    composable(FAMILY_ROUTE) {
                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            onFamilyIntent(FamilyMemberIntent.Refresh)
                        }
                        FamilyMemberScreen(
                            state = familyState,
                            onIntent = onFamilyIntent,
                            onReturnToVault = {
                                onFamilyIntent(FamilyMemberIntent.Cancel)
                                navigate(VAULT_ROUTE)
                            },
                            onContinueToStatements = {
                                onFamilyIntent(FamilyMemberIntent.Cancel)
                                onReviewIntent(StatementReviewIntent.LoadOwners)
                                navigate(MainDestination.REVIEW.route)
                            },
                            onSetUpHoldings = {
                                onFamilyIntent(FamilyMemberIntent.Cancel)
                                navigate(CUSTODY_ROUTE)
                            },
                            onManageFamily = {
                                onFamilyIntent(FamilyMemberIntent.Cancel)
                                navigate(FAMILY_ADMIN_ROUTE)
                            },
                        )
                    }
                    composable(FAMILY_ADMIN_ROUTE) {
                        LaunchedEffect(Unit) { onFamilyAdminIntent(FamilyAdminIntent.Refresh) }
                        FamilyAdministrationScreen(
                            state = familyAdminState,
                            onIntent = onFamilyAdminIntent,
                            onAddMember = {
                                onFamilyAdminIntent(FamilyAdminIntent.Cancel)
                                navigate(FAMILY_ROUTE)
                            },
                            onBack = {
                                onFamilyAdminIntent(FamilyAdminIntent.Cancel)
                                navigate(VAULT_ROUTE)
                            },
                            selectedFamilyOwner = selectedOwner,
                            onSelectFamilyOwner = { selectedOwner = it },
                        )
                    }
                    composable(ACCOUNTS_ROUTE) {
                        LaunchedEffect(Unit) { onFamilyAdminIntent(FamilyAdminIntent.Refresh) }
                        FamilyAdministrationScreen(
                            state = familyAdminState,
                            onIntent = onFamilyAdminIntent,
                            onAddMember = {
                                onFamilyAdminIntent(FamilyAdminIntent.Cancel)
                                navigate(FAMILY_ROUTE)
                            },
                            onBack = {
                                onFamilyAdminIntent(FamilyAdminIntent.Cancel)
                                navigate(MainDestination.HOME.route)
                            },
                            selectedFamilyOwner = selectedOwner,
                            onSelectFamilyOwner = { selectedOwner = it },
                            initialSection = "Accounts",
                        )
                    }
                    composable(CUSTODY_ROUTE) {
                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            onCustodyIntent(CustodyHoldingIntent.Refresh)
                        }
                        CustodyHoldingScreen(
                            state = custodyState,
                            verifiedOfficialCatalog = officialCatalog,
                            officialCatalogLoading = catalogLoading,
                            officialCatalogMessage = catalogMessage,
                            onRefreshOfficialCatalog = {
                                if (!catalogLoading) {
                                    catalogLoading = true
                                    catalogMessage = null
                                    catalogScope.launch {
                                        try {
                                            val rows = withContext(Dispatchers.IO) {
                                                catalogGateway.refresh()
                                            }
                                            officialCatalog = rows
                                            catalogMessage = "AMFI public NAV cache updated: " +
                                                rows.size + " dated fund identities."
                                        } catch (_: Exception) {
                                            // Fail closed and preserve the previously validated cache.
                                            catalogMessage = "AMFI report unavailable or format changed. " +
                                                "Previously validated offline data remains unchanged."
                                        } finally {
                                            catalogLoading = false
                                        }
                                    }
                                }
                            },
                            onIntent = onCustodyIntent,
                            onBackToVault = {
                                onCustodyIntent(CustodyHoldingIntent.Cancel)
                                navigate(VAULT_ROUTE)
                            },
                            onContinueToStatements = {
                                onCustodyIntent(CustodyHoldingIntent.Cancel)
                                onReviewIntent(StatementReviewIntent.LoadOwners)
                                navigate(MainDestination.REVIEW.route)
                            },
                            onRecordOpeningBalance = { newlyRegistered ->
                                pendingOpeningHoldingId = newlyRegistered
                                onCustodyIntent(CustodyHoldingIntent.Cancel)
                                onManualTradeIntent(ManualTradeIntent.Refresh)
                                navigate(MANUAL_TRADE_ROUTE)
                            },
                            selectedFamilyOwner = selectedOwner,
                            onSelectFamilyOwner = { selectedOwner = it },
                        )
                    }
                    composable(MANUAL_TRADE_ROUTE) {
                        LaunchedEffect(Unit) {
                            onManualTradeIntent(ManualTradeIntent.Refresh)
                        }
                        ManualTradeScreen(
                            state = manualTradeState,
                            onIntent = onManualTradeIntent,
                            preselectedHoldingId = pendingOpeningHoldingId,
                            selectedOwnerId = selectedOwner,
                            onReturn = {
                                pendingOpeningHoldingId = null
                                onManualTradeIntent(ManualTradeIntent.Cancel)
                                onDashboardRefresh()
                                navigate(MainDestination.PORTFOLIO.route)
                            },
                        )
                    }
                    composable(EMERGENCY_EXPORT_ROUTE) {
                        EmergencyExportScreen(
                            status = dossierStatus,
                            onSelectDestination = onChooseDossierDestination,
                            onBack = {
                                onCancelDossier()
                                navigate(MainDestination.SETTINGS.route)
                            },
                        )
                    }
                    composable(PASSWORD_CHANGE_ROUTE) {
                        ChangeLocalPasswordScreen(
                            onChange = onChangeLocalPassword,
                            onBack = { navigate(MainDestination.SETTINGS.route) },
                        )
                    }
                    composable(LEGACY_PREVIEW_ROUTE) {
                        LegacyBackupScreen(
                            state = legacyBackupState,
                            onIntent = onLegacyBackupIntent,
                            onBack = { navigate(VAULT_ROUTE) },
                        )
                    }
                    composable(RECOVERY_ROUTE) {
                        PortableRecoveryScreen(
                            state = recoveryState,
                            onIntent = onRecoveryIntent,
                            onReturnToVault = {
                                onRecoveryIntent(PortableRecoveryIntent.Cancel)
                                navigate(VAULT_ROUTE)
                            },
                        )
                    }
                    composable(BACKUP_FOLDER_ROUTE) {
                        LaunchedEffect(Unit) { onBackupFolderRefresh() }
                        BackupFolderScreen(
                            state = backupFolderState,
                            onPickFolder = onPickBackupFolder,
                            onEnabled = onBackupFolderEnabled,
                            onBackupNow = onBackupFolderNow,
                            onBack = { navigate(MainDestination.SETTINGS.route) },
                        )
                    }
                    composable(LICENCE_ROUTE) {
                        LicenceScreen(
                            state = licenceState,
                            onPurchase = onLicencePurchase,
                            onRestore = onLicenceRefresh,
                            onBack = { navigate(MainDestination.SETTINGS.route) },
                        )
                    }
                    composable(PRIVACY_ROUTE) {
                        PrivacyScreen()
                    }
                    composable(MainDestination.SETTINGS.route) {
                        SettingsScreen(
                            appearance = state.appearance,
                            onIntent = onIntent,
                            onOpenVault = { navigate(VAULT_ROUTE) },
                            onOpenPrivacy = { navigate(PRIVACY_ROUTE) },
                            onChangeAppPassword = { navigate(PASSWORD_CHANGE_ROUTE) },
                            onOpenEmergencyExport = { navigate(EMERGENCY_EXPORT_ROUTE) },
                            onOpenBackupFolder = { navigate(BACKUP_FOLDER_ROUTE) },
                            licence = licenceState.licence,
                            onOpenLicence = { navigate(LICENCE_ROUTE) },
                            gmail = gmail,
                            onGmailConnect = onGmailConnect,
                            onGmailDisconnect = onGmailDisconnect,
                        )
                    }
                }
            }
        }
    }
}

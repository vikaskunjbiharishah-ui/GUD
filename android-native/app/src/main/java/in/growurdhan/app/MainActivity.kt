package `in`.growurdhan.app

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import `in`.growurdhan.app.cas.NativeCasPdfReader
import `in`.growurdhan.core.model.OwnerId
import `in`.growurdhan.core.model.FamilyAdminChange
import `in`.growurdhan.domain.finance.toFinancialHolding
import `in`.growurdhan.domain.finance.toFinancialEvent
import `in`.growurdhan.domain.finance.PortfolioEngine
import `in`.growurdhan.domain.finance.HouseholdDashboardProjector
import `in`.growurdhan.domain.finance.EmergencyDossierCsv
import java.time.LocalDate
import `in`.growurdhan.domain.vault.LegacyRestoreIdentity
import `in`.growurdhan.domain.vault.VaultRepository
import `in`.growurdhan.app.presentation.FamilyAdminIntent
import `in`.growurdhan.app.presentation.FamilyAdminUiState
import `in`.growurdhan.app.presentation.FamilyAdministrationViewModel
import `in`.growurdhan.app.presentation.FamilyMemberIntent
import `in`.growurdhan.app.presentation.FamilyMemberUiState
import `in`.growurdhan.app.presentation.FamilyMemberViewModel
import `in`.growurdhan.app.presentation.PortableRecoveryMode
import `in`.growurdhan.app.presentation.PortableRecoveryUiState
import `in`.growurdhan.app.presentation.PortableRecoveryViewModel
import `in`.growurdhan.app.presentation.screens.PortableRecoveryIntent
import `in`.growurdhan.app.presentation.CustodyHoldingIntent
import `in`.growurdhan.app.presentation.CustodyHoldingUiState
import `in`.growurdhan.app.presentation.CustodyHoldingViewModel
import `in`.growurdhan.app.presentation.AppViewModel
import `in`.growurdhan.app.billing.PlayBillingGateway
import `in`.growurdhan.app.presentation.BackupFolderViewModel
import `in`.growurdhan.app.gmail.GmailSignInHost
import `in`.growurdhan.app.presentation.LicenceViewModel
import `in`.growurdhan.app.presentation.screens.LicenceScreen
import `in`.growurdhan.domain.finance.Licence
import `in`.growurdhan.app.presentation.GrowUrDhanShell
import `in`.growurdhan.app.presentation.StatementReviewIntent
import `in`.growurdhan.app.presentation.StatementReviewUiState
import `in`.growurdhan.app.presentation.StatementReviewViewModel
import `in`.growurdhan.app.presentation.VaultIntent
import `in`.growurdhan.app.presentation.VaultUiState
import `in`.growurdhan.app.presentation.VaultSessionGate
import `in`.growurdhan.app.presentation.PdfImportGuard
import `in`.growurdhan.app.presentation.VaultViewModel
import `in`.growurdhan.app.presentation.HouseholdDashboardViewModel
import `in`.growurdhan.app.presentation.HouseholdDashboardUiState
import `in`.growurdhan.app.presentation.ManualTradeViewModel
import `in`.growurdhan.app.presentation.ManualTradeIntent
import `in`.growurdhan.app.presentation.ManualTradeUiState
import `in`.growurdhan.app.presentation.LocalAppPasswordStore
import `in`.growurdhan.app.presentation.LocalLoginStatus
import `in`.growurdhan.app.presentation.screens.LocalLoginScreen
import `in`.growurdhan.app.presentation.screens.RecoveryEnrollmentScreen
import `in`.growurdhan.app.presentation.screens.OneTimeRecoveryCodeScreen
import `in`.growurdhan.app.presentation.screens.ForgottenPasswordRecoveryScreen
import `in`.growurdhan.app.presentation.screens.ChangeLocalPasswordScreen
import `in`.growurdhan.app.presentation.screens.LegacyBackupIntent
import `in`.growurdhan.app.presentation.screens.LegacyBackupUiState
import `in`.growurdhan.app.presentation.screens.EmergencyExportUiState
import `in`.growurdhan.data.vault.LegacyLockedBackupInspector
import `in`.growurdhan.data.vault.LegacyVaultMigration
import java.security.MessageDigest
import java.io.ByteArrayOutputStream
import `in`.growurdhan.domain.vault.LocalPasswordRules
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import `in`.growurdhan.app.presentation.theme.GrowUrDhanTheme
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Native access requires a local GrowUrDhan app-password verifier AND
 * Android device credential confirmation. The app password does not decrypt
 * SQLCipher or the Keystore database key. No vault is created automatically.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var sessionGate: VaultSessionGate
    @Inject lateinit var casReader: NativeCasPdfReader
    @Inject lateinit var repository: VaultRepository
    @Inject lateinit var appPassword: LocalAppPasswordStore
    @Inject lateinit var playBilling: PlayBillingGateway
    private var localLoginError by mutableStateOf<String?>(null)
    private var showForgotRecovery by mutableStateOf(false)
    private var displayedRecoveryCode by mutableStateOf<String?>(null)
    private var pendingRecoveryCodeInput: CharArray? = null
    private var pendingRecoveredPassword: CharArray? = null
    private var pendingEnrollmentPassword: CharArray? = null
    private var pendingEnrollmentEmail: String? = null
    private var pendingPreviousPassword: CharArray? = null
    private var pendingReplacementPassword: CharArray? = null
    private var dossierPickerOpen = false
    private var dossierUri: Uri? = null
    private var dossierGeneration = 0L
    private var dossierUiStatus by mutableStateOf(EmergencyExportUiState.READY)
    private var legacyPickerOpen = false
    private var legacyFileUri: Uri? = null
    private var legacyEmail: String? = null
    private var legacySecret: CharArray? = null
    private var legacyRestoreIdentity: LegacyRestoreIdentity? = null
    private var legacyGeneration = 0L
    private var legacyExpectedSha: String? = null
    private var legacyUiState by mutableStateOf<LegacyBackupUiState>(LegacyBackupUiState.Empty)
    private var loginOperationGeneration = 0L
    private val viewModel: AppViewModel by viewModels()
    private val vaultViewModel: VaultViewModel by viewModels()
    private val dashboardViewModel: HouseholdDashboardViewModel by viewModels()
    private val reviewViewModel: StatementReviewViewModel by viewModels()
    private val familyViewModel: FamilyMemberViewModel by viewModels()
    private val familyAdminViewModel: FamilyAdministrationViewModel by viewModels()
    private val custodyViewModel: CustodyHoldingViewModel by viewModels()
    private val recoveryViewModel: PortableRecoveryViewModel by viewModels()
    private val manualTradeViewModel: ManualTradeViewModel by viewModels()
    private val backupFolderViewModel: BackupFolderViewModel by viewModels()
    private val licenceViewModel: LicenceViewModel by viewModels()
    private lateinit var gmailSignIn: GmailSignInHost
    private enum class CredentialAction {
        OPEN_VAULT, APPROVE_REVIEW, IMPORT_PDF, ADD_FAMILY_MEMBER, REGISTER_CUSTODY,
        PORTABLE_BACKUP, APPROVE_MANUAL_TRADE, ENROLL_APP_PASSWORD, INSPECT_LEGACY,
        CHANGE_APP_PASSWORD, EXPORT_DOSSIER, RESTORE_LEGACY, MANAGE_FAMILY,
        ISSUE_RECOVERY_CODE, RECOVER_APP_PASSWORD, RESTORE_LEGACY_COMMIT, BACKUP_FOLDER,
    }
    private var pendingCredential: CredentialAction? = null
    private var pdfPickerOpen: Boolean = false
    private var backupPickerOpen: Boolean = false
    private var folderPickerOpen: Boolean = false
    private var backupMode: PortableRecoveryMode? = null
    private var backupUri: Uri? = null
    private var backupPassword: CharArray? = null
    private var pickedPdfOwner: OwnerId? = null
    private var pickedPdfPassword: CharArray? = null
    private var pickedPdfUri: Uri? = null
    // A document parse belongs to one import action AND one OS device challenge.
    private val pdfImportGuard by lazy { PdfImportGuard(sessionGate) }
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutCheck = object : Runnable {
        override fun run() {
            if (pendingCredential == null && vaultViewModel.state.value is VaultUiState.Ready &&
                !sessionGate.isUnlocked()
            ) {
                lockSensitiveUi()
            }
            timeoutHandler.postDelayed(this, 5_000L)
        }
    }

    private val credentialResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val action = pendingCredential
        pendingCredential = null
        if (result.resultCode == Activity.RESULT_OK && action != null &&
            (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure
        ) {
            if (action == CredentialAction.ENROLL_APP_PASSWORD) {
                val pending = pendingEnrollmentPassword
                val pendingEmail = pendingEnrollmentEmail
                pendingEnrollmentPassword = null
                pendingEnrollmentEmail = null
                if (pending == null) {
                    lockSensitiveUi()
                } else {
                    val generation = ++loginOperationGeneration
                    lifecycleScope.launch {
                        try {
                            val completed = withContext(Dispatchers.Default) {
                                appPassword.setup(pending, pendingEmail)
                            }
                            if (generation != loginOperationGeneration) {
                                appPassword.lock()
                            } else {
                                if (completed) {
                                    displayedRecoveryCode = withContext(Dispatchers.Default) {
                                        appPassword.issueRecoveryCodeAfterDeviceApproval()
                                    }
                                }
                                localLoginError = if (completed) null else
                                    "Local password storage is unavailable; records have been preserved."
                            }
                        } catch (_: Exception) {
                            appPassword.lock()
                            localLoginError =
                                "App password could not be created. No financial records were changed."
                        } finally {
                            pending.fill('\u0000')
                        }
                    }
                }
            } else if (action == CredentialAction.RECOVER_APP_PASSWORD) {
                val recovery = pendingRecoveryCodeInput
                val next = pendingRecoveredPassword
                pendingRecoveryCodeInput = null
                pendingRecoveredPassword = null
                if (recovery == null || next == null ||
                    appPassword.status.value != LocalLoginStatus.LOCKED
                ) {
                    recovery?.fill('\u0000')
                    next?.fill('\u0000')
                    lockSensitiveUi()
                } else {
                    val generation = ++loginOperationGeneration
                    lifecycleScope.launch {
                        try {
                            val ok = withContext(Dispatchers.Default) {
                                appPassword.recoverAfterDeviceApproval(recovery, next)
                            }
                            if (generation != loginOperationGeneration) {
                                appPassword.lock()
                            } else {
                                showForgotRecovery = false
                                localLoginError = if (ok)
                                    "Password recovered. Sign in using the new password."
                                else "Recovery could not be verified. Check the code and device lock."
                            }
                        } catch (_: Exception) {
                            localLoginError = "Recovery evidence unavailable. No records were changed."
                        } finally {
                            recovery.fill('\u0000')
                            next.fill('\u0000')
                        }
                    }
                }
            } else if (!appPassword.hasVerifiedPassword()) {
                lockSensitiveUi()
            } else {
            try {
                sessionGate.grantDeviceCredential()
            } catch (_: IllegalStateException) {
                lockSensitiveUi()
                return@registerForActivityResult
            }
            when (action) {
                CredentialAction.ENROLL_APP_PASSWORD -> Unit // handled above
                CredentialAction.RECOVER_APP_PASSWORD -> Unit // handled above
                CredentialAction.ISSUE_RECOVERY_CODE -> {
                    displayedRecoveryCode = appPassword.issueRecoveryCodeAfterDeviceApproval()
                }
                CredentialAction.INSPECT_LEGACY -> inspectLegacyAfterCredential()
                CredentialAction.RESTORE_LEGACY -> prepareLegacyDryRunAfterCredential()
                CredentialAction.RESTORE_LEGACY_COMMIT -> restoreLegacyAfterCredential()
                CredentialAction.CHANGE_APP_PASSWORD -> finishPasswordRotation()
                CredentialAction.EXPORT_DOSSIER -> exportDossierAfterCredential()
                CredentialAction.OPEN_VAULT -> vaultViewModel.onIntent(VaultIntent.OpenOrCreate)
                CredentialAction.APPROVE_REVIEW ->
                    reviewViewModel.onIntent(StatementReviewIntent.CommitAfterCredential)
                CredentialAction.IMPORT_PDF -> parseSelectedPdfAfterCredential()
                CredentialAction.ADD_FAMILY_MEMBER ->
                    familyViewModel.onIntent(FamilyMemberIntent.SaveAfterCredential)
                CredentialAction.MANAGE_FAMILY ->
                    familyAdminViewModel.onIntent(FamilyAdminIntent.CommitAfterCredential)
                CredentialAction.REGISTER_CUSTODY ->
                    custodyViewModel.onIntent(CustodyHoldingIntent.SaveAfterCredential)
                CredentialAction.PORTABLE_BACKUP -> recoverAfterCredential()
                CredentialAction.APPROVE_MANUAL_TRADE ->
                    manualTradeViewModel.onIntent(ManualTradeIntent.SaveAfterCredential)
                CredentialAction.BACKUP_FOLDER -> backupFolderViewModel.writeAfterCredential()
            }
            }
        } else {
            // Device credential cancelled, removed, or otherwise not verified.
            lockSensitiveUi()
        }
    }

    // SAF document chooser grants read access only to the selected local/cloud URI.
    // No blanket READ_EXTERNAL_STORAGE or INTERNET permission is requested.
    private val pdfPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        pdfPickerOpen = false
        if (uri == null || pickedPdfOwner == null) {
            lockSensitiveUi()
        } else {
            pickedPdfUri = uri
            // OS picker/background revokes prior session; require device credential
            // again before even reading the PDF or decrypted financial book.
            requestCredential(CredentialAction.IMPORT_PDF)
        }
    }

    // An external document picker revokes the active vault session. Only the
    // selected URI and operation kind survive; the backup password is entered
    // *after* returning to the app and before a new system credential check.
    private val backupExportPicker = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        completeBackupDocumentSelection(uri, PortableRecoveryMode.EXPORT)
    }

    private val backupRestorePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        completeBackupDocumentSelection(uri, PortableRecoveryMode.RESTORE)
    }

    private val dossierDestinationPicker = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        dossierPickerOpen = false
        dossierUri = uri
        if (uri == null) {
            dossierUiStatus = EmergencyExportUiState.READY
        } else {
            requestCredential(CredentialAction.EXPORT_DOSSIER)
        }
    }

    private val legacyBackupPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        legacyPickerOpen = false
        if (uri == null) {
            clearLegacySensitive()
        } else {
            legacyFileUri = uri
            legacyUiState = LegacyBackupUiState.EnterCredentials
        }
    }

    /** A folder the user picks (Drive, OneDrive, or Downloads). No password is stored. */
    private val backupFolderPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        folderPickerOpen = false
        if (uri == null) return@registerForActivityResult
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (_: SecurityException) {
            return@registerForActivityResult
        }
        backupFolderViewModel.rememberFolder(uri, folderLabel(uri))
    }

    private fun clearLegacySensitive() {
        ++legacyGeneration
        legacyEmail = null
        legacySecret?.fill('\u0000')
        legacySecret = null
        legacyRestoreIdentity?.wipeSecrets()
        legacyRestoreIdentity = null
        legacyFileUri = null
        legacyExpectedSha = null
        legacyPickerOpen = false
        legacyUiState = LegacyBackupUiState.Empty
    }

    private fun inspectLegacyAfterCredential() {
        val uri = legacyFileUri
        val email = legacyEmail
        val password = legacySecret
        legacySecret = null
        legacyEmail = null
        if (uri == null || email == null || password == null ||
            !sessionGate.canCommitFinancialChanges()
        ) {
            password?.fill('\u0000')
            clearLegacySensitive()
            return
        }
        val request = ++legacyGeneration
        val revision = sessionGate.credentialEpoch()
        legacyUiState = LegacyBackupUiState.Inspecting
        lifecycleScope.launch {
            var sourceBytes: ByteArray? = null
            try {
                val bytes = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { input ->
                        val destination = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            check(destination.size() + count <=
                                LegacyLockedBackupInspector.MAX_INPUT_BYTES) {
                                "Legacy archive is oversized."
                            }
                            destination.write(buffer, 0, count)
                        }
                        destination.toByteArray()
                    } ?: throw IllegalArgumentException("Selected archive is unavailable.")
                }
                sourceBytes = bytes
                check(sessionGate.isCurrentSession(revision))
                val summary = withContext(Dispatchers.Default) {
                    LegacyLockedBackupInspector.inspect(bytes, email, password)
                }
                if (legacyGeneration == request && sessionGate.isCurrentSession(revision)) {
                    legacyUiState = LegacyBackupUiState.Preview(summary)
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (legacyGeneration == request && sessionGate.isCurrentSession(revision)) {
                    legacyUiState = LegacyBackupUiState.Error(
                        "Archive could not be authenticated or inspected. " +
                            "No financial data was imported or changed.",
                    )
                }
            } finally {
                sourceBytes?.fill(0)
                password.fill('\u0000')
            }
        }
    }

    /**
     * Strict original archive import: the SAF picker never grants broader access,
     * the local application password is required separately, the OS challenge
     * must have succeeded after preparing this import, and SQLCipher enforces
     * empty-vault atomic commit. Do not use this to reset a lost native password.
     */
    private fun prepareLegacyDryRunAfterCredential() {
        val uri = legacyFileUri
        val identity = legacyRestoreIdentity
        if (uri == null || identity == null || !sessionGate.canCommitFinancialChanges()) {
            identity?.wipeSecrets()
            clearLegacySensitive()
            return
        }
        val generation = ++legacyGeneration
        val revision = sessionGate.credentialEpoch()
        legacyUiState = LegacyBackupUiState.Inspecting
        lifecycleScope.launch {
            var archive: ByteArray? = null
            try {
                vaultViewModel.awaitPendingCleanup()
                check(legacyGeneration == generation && sessionGate.isCurrentApproval(revision))
                val bytes = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { input ->
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val size = input.read(buffer)
                            if (size < 0) break
                            check(output.size() + size <=
                                LegacyLockedBackupInspector.MAX_INPUT_BYTES) {
                                "Original archive exceeds the supported safe size."
                            }
                            output.write(buffer, 0, size)
                        }
                        buffer.fill(0)
                        output.toByteArray()
                    } ?: error("Original archive is unavailable.")
                }
                archive = bytes
                check(legacyGeneration == generation && sessionGate.isCurrentApproval(revision))
                val plan = withContext(Dispatchers.Default) {
                    LegacyVaultMigration.dryRunAuthenticated(bytes,identity)
                }
                check(legacyGeneration == generation && sessionGate.isCurrentApproval(revision))
                legacyExpectedSha = plan.sourceSha256
                if (legacyGeneration == generation && sessionGate.isCurrentSession(revision)) {
                    legacyUiState = LegacyBackupUiState.DryRunReady(plan)
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (legacyGeneration == generation && sessionGate.isCurrentSession(revision)) {
                    legacyUiState = LegacyBackupUiState.Error(
                        "Original identity or dry-run mapping could not be verified. " +
                            "Existing financial records were not merged or overwritten. " +
                            "If a commit may have occurred, reopen your vault to confirm it.",
                    )
                }
            } finally {
                archive?.fill(0)
                if (legacyExpectedSha == null) {
                    identity.wipeSecrets()
                    legacyRestoreIdentity = null
                }
            }
        }
    }

    private fun restoreLegacyAfterCredential() {
        val uri = legacyFileUri
        val identity = legacyRestoreIdentity
        legacyRestoreIdentity = null
        if (uri == null || identity == null || !sessionGate.canCommitFinancialChanges()) {
            identity?.wipeSecrets()
            clearLegacySensitive()
            return
        }
        val fingerprint = legacyExpectedSha ?: run {
            identity.wipeSecrets()
            clearLegacySensitive()
            return
        }
        legacyExpectedSha = null
        val generation = ++legacyGeneration
        val revision = sessionGate.credentialEpoch()
        legacyUiState = LegacyBackupUiState.Restoring
        lifecycleScope.launch {
            var archive: ByteArray? = null
            try {
                vaultViewModel.awaitPendingCleanup()
                check(legacyGeneration == generation && sessionGate.isCurrentApproval(revision))
                val bytes = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { input ->
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val size = input.read(buffer)
                            if (size < 0) break
                            check(output.size() + size <=
                                LegacyLockedBackupInspector.MAX_INPUT_BYTES) {
                                "Original archive exceeds the supported safe size."
                            }
                            output.write(buffer, 0, size)
                        }
                        buffer.fill(0)
                        output.toByteArray()
                    } ?: error("Original archive is unavailable.")
                }
                archive = bytes
                check(legacyGeneration == generation && sessionGate.isCurrentApproval(revision))
                val actualSha = MessageDigest.getInstance("SHA-256").digest(bytes)
                    .joinToString("") { "%02x".format(it.toInt() and 0xff) }
                require(MessageDigest.isEqual(actualSha.toByteArray(),fingerprint.toByteArray())) {
                    "Selected archive changed after the approved dry run."
                }
                repository.openOrCreate()
                repository.restoreOriginalLegacyBackup(identity, bytes)
                if (!appPassword.hasAccountEmailBinding()) {
                    // Backward compatible prior native logins: never persist the
                    // restored original email in plaintext preferences.
                    appPassword.bindAccountEmail(identity.email)
                }
                if (legacyGeneration == generation && sessionGate.isCurrentSession(revision)) {
                    legacyUiState = LegacyBackupUiState.Recovered
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (legacyGeneration == generation && sessionGate.isCurrentSession(revision)) {
                    legacyUiState = LegacyBackupUiState.Error(
                        "Original account verification or encrypted restore did not complete. " +
                            "Existing financial records were not merged or overwritten. " +
                            "If a commit may have occurred, reopen your vault to confirm it.",
                    )
                }
            } finally {
                archive?.fill(0)
                identity.wipeSecrets()
            }
        }
    }

    private fun completeBackupDocumentSelection(uri: Uri?, mode: PortableRecoveryMode) {
        backupPickerOpen = false
        if (uri == null || backupMode != mode) {
            lockSensitiveUi()
            return
        }
        backupUri = uri
        recoveryViewModel.offerPickedDocument(mode)
    }

    private fun recoverAfterCredential() {
        val selectedMode = backupMode
        val selectedUri = backupUri
        val selectedPassword = backupPassword
        backupMode = null
        backupUri = null
        backupPassword = null
        if (selectedMode == null || selectedUri == null || selectedPassword == null ||
            !sessionGate.canCommitFinancialChanges()
        ) {
            selectedPassword?.fill('\u0000')
            lockSensitiveUi()
            return
        }
        recoveryViewModel.executeAfterCredential(
            selectedMode, selectedUri, selectedPassword, vaultViewModel::awaitPendingCleanup,
        )
    }

    private fun parseSelectedPdfAfterCredential() {
        val uri = pickedPdfUri
        val owner = pickedPdfOwner
        val password = pickedPdfPassword
        pickedPdfUri = null
        pickedPdfOwner = null
        pickedPdfPassword = null
        if (uri == null || owner == null || !sessionGate.isUnlocked()) {
            password?.fill('\u0000')
            lockSensitiveUi()
            return
        }
        val importRequest = pdfImportGuard.begin()
        // One authenticated, repository-level open. Do not also dispatch
        // OpenOrCreate to the ViewModel: two independent opens can race Close.
        lifecycleScope.launch {
            try {
                vaultViewModel.awaitPendingCleanup()
                check(pdfImportGuard.isCurrent(importRequest)) {
                    "Vault session expired while waiting for picker cleanup."
                }
                repository.openOrCreate()
                check(pdfImportGuard.isCurrent(importRequest)) {
                    "Vault session expired during PDF import."
                }
                vaultViewModel.onIntent(VaultIntent.Refresh)
                val book = repository.readBook()
                check(pdfImportGuard.isCurrent(importRequest)) {
                    "Vault session expired before PDF validation."
                }
                check(book.members.any { it.id == owner }) {
                    "Statement owner does not exist in encrypted book."
                }
                val verified = casReader.parseAndValidate(
                    uri, owner, book.holdings.map { it.toFinancialHolding() },
                    book.casIdentities,
                    password?.concatToString() ?: "",
                )
                check(pdfImportGuard.isCurrent(importRequest)) {
                    "Vault session expired during PDF parsing."
                }
                reviewViewModel.prepareFromValidatedParser(
                    "cas_${verified.sourceSha256}",
                    owner,
                    verified.sourceSha256,
                    verified.lines,
                )
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Never reveal source content, Python errors, PAN or passwords.
                // A stale/background import must not resurrect the review screen.
                if (pdfImportGuard.isCurrent(importRequest)) {
                    reviewViewModel.reportParserFailure()
                }
            } finally {
                password?.fill('\u0000')
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gmailSignIn = GmailSignInHost(this, lifecycleScope)
        playBilling.attach(this)
        // Never allow financial rows or receipt details in OS screenshots/recents.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val loginStatus by appPassword.status.collectAsStateWithLifecycle()
            val vaultState by vaultViewModel.state.collectAsStateWithLifecycle()
            val dashboardState by dashboardViewModel.state.collectAsStateWithLifecycle()
            androidx.compose.runtime.LaunchedEffect(vaultState) {
                if (vaultState is VaultUiState.Ready) dashboardViewModel.refresh()
                else dashboardViewModel.lock()
            }
            val reviewState by reviewViewModel.state.collectAsStateWithLifecycle()
            val familyState by familyViewModel.state.collectAsStateWithLifecycle()
            val familyAdminState by familyAdminViewModel.state.collectAsStateWithLifecycle()
            val custodyState by custodyViewModel.state.collectAsStateWithLifecycle()
            val recoveryState by recoveryViewModel.state.collectAsStateWithLifecycle()
            val manualTradeState by manualTradeViewModel.state.collectAsStateWithLifecycle()
            val backupFolderState by backupFolderViewModel.state.collectAsStateWithLifecycle()
            val licenceState by licenceViewModel.state.collectAsStateWithLifecycle()
            val gmailState by gmailSignIn.state.collectAsStateWithLifecycle()
            androidx.compose.runtime.LaunchedEffect(reviewState) {
                if (reviewState is StatementReviewUiState.Completed) {
                    dashboardViewModel.refresh()
                    vaultViewModel.onIntent(VaultIntent.Refresh)
                }
            }
            androidx.compose.runtime.LaunchedEffect(familyState) {
                if (familyState is FamilyMemberUiState.Saved) {
                    // A successful encrypted write updates the vault's member count.
                    vaultViewModel.onIntent(VaultIntent.Refresh)
                    dashboardViewModel.refresh()
                }
            }
            androidx.compose.runtime.LaunchedEffect(familyAdminState) {
                if (familyAdminState is FamilyAdminUiState.Saved) {
                    vaultViewModel.onIntent(VaultIntent.Refresh)
                    dashboardViewModel.refresh()
                    // Account enrollment stores the email in SQLCipher. Keep only
                    // a one-way hardware MAC in local login preferences.
                    if (appPassword.hasVerifiedPassword() && sessionGate.isUnlocked()) {
                        try {
                            val owner = withContext(Dispatchers.IO) {
                                repository.familyAdministration().ownerIdentity
                            }
                            if (owner != null) appPassword.bindAccountEmail(owner.email)
                        } catch (_: Exception) {
                            // The encrypted identity remains intact; no fallback
                            // verifier is generated or original key replaced.
                        }
                    }
                }
            }
            androidx.compose.runtime.LaunchedEffect(custodyState) {
                if (custodyState is CustodyHoldingUiState.Saved) {
                    vaultViewModel.onIntent(VaultIntent.Refresh)
                    dashboardViewModel.refresh()
                }
            }
            androidx.compose.runtime.LaunchedEffect(recoveryState) {
                if (recoveryState is PortableRecoveryUiState.Completed) {
                    vaultViewModel.onIntent(VaultIntent.Refresh)
                    dashboardViewModel.refresh()
                }
            }
            androidx.compose.runtime.LaunchedEffect(manualTradeState) {
                if (manualTradeState is ManualTradeUiState.Saved) {
                    vaultViewModel.onIntent(VaultIntent.Refresh)
                    dashboardViewModel.refresh()
                }
            }
            GrowUrDhanTheme(state.appearance) {
                if (loginStatus != LocalLoginStatus.VERIFIED) {
                    if (showForgotRecovery && loginStatus == LocalLoginStatus.LOCKED) {
                        ForgottenPasswordRecoveryScreen(
                            message = localLoginError,
                            onReset = ::stageForgottenRecovery,
                            onBack = {
                                showForgotRecovery = false
                                localLoginError = null
                            },
                        )
                    } else {
                        LocalLoginScreen(
                            state = loginStatus,
                            message = localLoginError,
                            accountEmailRequired = appPassword.hasAccountEmailBinding(),
                            onSubmitWithEmail = ::handleLocalLogin,
                            onSubmit = { password, repeated -> handleLocalLogin("", password, repeated) },
                            hasRecoveryCode = appPassword.hasRecoveryCode(),
                            onForgotPassword = {
                                showForgotRecovery = true
                                localLoginError = null
                            },
                        )
                    }
                } else if (displayedRecoveryCode != null) {
                    OneTimeRecoveryCodeScreen(
                        code = displayedRecoveryCode ?: "",
                        onAcknowledged = { displayedRecoveryCode = null },
                    )
                } else if (!appPassword.hasRecoveryCode()) {
                    RecoveryEnrollmentScreen(
                        onEnable = { requestCredential(CredentialAction.ISSUE_RECOVERY_CODE) },
                    )
                } else if (appPassword.passwordNeedsRotation()) {
                    // Match the legacy 180-day mandatory password rotation policy.
                    // Rotation still requires the PREVIOUS password + a NEW OS challenge.
                    ChangeLocalPasswordScreen(
                        onChange = ::handleChangeLocalPassword,
                        onBack = { lockSensitiveUi() },
                    )
                } else if (licenceState.checking && !Licence.appUnlocked(licenceState.licence)) {
                    androidx.compose.material3.Text("Checking Google Play…")
                } else if (!Licence.appUnlocked(licenceState.licence)) {
                    LicenceScreen(
                        state = licenceState,
                        onPurchase = licenceViewModel::purchase,
                        onRestore = licenceViewModel::refresh,
                    )
                } else GrowUrDhanShell(
                    state = state,
                    onIntent = viewModel::onIntent,
                    vaultState = vaultState,
                    // A late collector emission must never display holdings outside a live vault session.
                    dashboardState = if (vaultState is VaultUiState.Ready && sessionGate.isUnlocked()) {
                        dashboardState
                    } else {
                        HouseholdDashboardUiState.Locked
                    },
                    onDashboardRefresh = dashboardViewModel::refresh,
                    onVaultIntent = ::handleVaultIntent,
                    reviewState = reviewState,
                    onReviewIntent = ::handleReviewIntent,
                    familyState = familyState,
                    onFamilyIntent = ::handleFamilyIntent,
                    familyAdminState = familyAdminState,
                    onFamilyAdminIntent = ::handleFamilyAdminIntent,
                    custodyState = custodyState,
                    onCustodyIntent = ::handleCustodyIntent,
                    recoveryState = recoveryState,
                    onRecoveryIntent = ::handleRecoveryIntent,
                    manualTradeState = manualTradeState,
                    onManualTradeIntent = ::handleManualTradeIntent,
                    legacyBackupState = legacyUiState,
                    onLegacyBackupIntent = ::handleLegacyBackupIntent,
                    onChangeLocalPassword = ::handleChangeLocalPassword,
                    dossierStatus = dossierUiStatus,
                    onChooseDossierDestination = ::chooseDossierDestination,
                    onCancelDossier = ::cancelDossier,
                    backupFolderState = backupFolderState,
                    onPickBackupFolder = ::chooseBackupFolder,
                    onBackupFolderEnabled = backupFolderViewModel::setEnabled,
                    onBackupFolderNow = ::startFolderBackup,
                    onBackupFolderRefresh = backupFolderViewModel::refresh,
                    licenceState = licenceState,
                    onLicencePurchase = licenceViewModel::purchase,
                    onLicenceRefresh = licenceViewModel::refresh,
                    gmail = gmailState,
                    onGmailConnect = gmailSignIn::connect,
                    onGmailDisconnect = gmailSignIn::disconnect,
                )
            }
        }
    }

    /** The first verifier setup is separately approved by Android's secure lock. */
    private fun handleLocalLogin(email: String, password: String, repeated: String?) {
        val secret = password.toCharArray()
        try {
            when (appPassword.status.value) {
                LocalLoginStatus.NEEDS_SETUP -> {
                    val issues = LocalPasswordRules.issues(secret)
                    if (issues.isNotEmpty() || password != repeated) {
                        localLoginError = issues.firstOrNull()
                            ?: "The two app passwords do not match."
                        return
                    }
                    val manager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                    if (!manager.isDeviceSecure) {
                        localLoginError = "Configure your Android screen lock first."
                        return
                    }
                    pendingEnrollmentPassword?.fill('\u0000')
                    pendingEnrollmentPassword = secret.copyOf()
                    pendingEnrollmentEmail = email.trim().lowercase()
                    localLoginError = null
                    requestCredential(CredentialAction.ENROLL_APP_PASSWORD)
                }
                LocalLoginStatus.LOCKED -> {
                    val generation = ++loginOperationGeneration
                    localLoginError = null
                    val attempt = secret.copyOf()
                    lifecycleScope.launch {
                        try {
                            val accepted = withContext(Dispatchers.Default) {
                                appPassword.verify(attempt) && appPassword.verifyAccountEmail(email)
                            }
                            if (generation != loginOperationGeneration) {
                                appPassword.lock()
                            } else {
                                localLoginError = if (accepted) null else
                                    "Account email or password could not be verified. Repeated password failures cause a cooldown."
                            }
                        } catch (_: Exception) {
                            appPassword.lock()
                            localLoginError = "Local login is unavailable. Records were preserved."
                        } finally {
                            attempt.fill('\u0000')
                        }
                    }
                }
                LocalLoginStatus.VERIFIED, LocalLoginStatus.STORAGE_UNAVAILABLE -> Unit
            }
        } finally {
            secret.fill('\u0000')
        }
    }

    /** Never executes the reset without a NEW OS credential result. */
    private fun stageForgottenRecovery(code: String, next: String) {
        if (appPassword.status.value != LocalLoginStatus.LOCKED ||
            !appPassword.hasRecoveryCode() ||
            LocalPasswordRules.issues(next.toCharArray()).isNotEmpty()
        ) {
            localLoginError = "A saved recovery code and a valid new password are required."
            return
        }
        pendingRecoveryCodeInput?.fill('\u0000')
        pendingRecoveredPassword?.fill('\u0000')
        pendingRecoveryCodeInput = code.toCharArray()
        pendingRecoveredPassword = next.toCharArray()
        requestCredential(CredentialAction.RECOVER_APP_PASSWORD)
    }

    private fun handleVaultIntent(intent: VaultIntent) {
        if (intent == VaultIntent.OpenOrCreate) {
            requestCredential(CredentialAction.OPEN_VAULT)
        } else {
            if (intent == VaultIntent.Close || intent == VaultIntent.CredentialUnavailable) {
                pdfImportGuard.invalidate()
            }
            if (intent == VaultIntent.Close) {
                ++loginOperationGeneration
                appPassword.lock()
                sessionGate.lock()
                dashboardViewModel.lock()
                reviewViewModel.clearSensitiveState()
                manualTradeViewModel.clearSensitiveState()
                recoveryViewModel.clearSensitiveState()
            }
            vaultViewModel.onIntent(intent)
        }
    }

    private fun handleReviewIntent(intent: StatementReviewIntent) {
        when (intent) {
            StatementReviewIntent.LoadOwners -> reviewViewModel.onIntent(intent)
            is StatementReviewIntent.PickPdf -> {
                if (!sessionGate.isUnlocked() || vaultViewModel.state.value !is VaultUiState.Ready) {
                    lockSensitiveUi()
                } else {
                    pdfImportGuard.invalidate()
                    pickedPdfPassword?.fill('\u0000')
                    pickedPdfOwner = intent.ownerId
                    pickedPdfPassword = intent.pdfPassword.toCharArray()
                    pdfPickerOpen = true
                    try {
                        pdfPicker.launch(arrayOf("application/pdf"))
                    } catch (_: Exception) {
                        lockSensitiveUi()
                    }
                }
            }
            StatementReviewIntent.RequestApproval -> {
                if (reviewViewModel.state.value !is StatementReviewUiState.Reviewing) return
                if (!sessionGate.isUnlocked()) {
                    lockSensitiveUi()
                } else {
                    requestCredential(CredentialAction.APPROVE_REVIEW)
                }
            }
            StatementReviewIntent.Cancel -> {
                pdfImportGuard.invalidate()
                pickedPdfPassword?.fill('\u0000')
                pickedPdfPassword = null
                pickedPdfUri = null
                pickedPdfOwner = null
                reviewViewModel.clearSensitiveState()
            }
            StatementReviewIntent.CommitAfterCredential -> Unit // never accept from UI
        }
    }

    private fun handleFamilyIntent(intent: FamilyMemberIntent) {
        when (intent) {
            is FamilyMemberIntent.Stage -> {
                if (!sessionGate.isUnlocked() || vaultViewModel.state.value !is VaultUiState.Ready) {
                    lockSensitiveUi()
                    return
                }
                familyViewModel.onIntent(intent)
                // Only a validated form staged by the ViewModel may request
                // the write-capable device credential challenge.
                if (familyViewModel.state.value is FamilyMemberUiState.AwaitingCredential) {
                    requestCredential(CredentialAction.ADD_FAMILY_MEMBER)
                }
            }
            FamilyMemberIntent.SaveAfterCredential -> Unit // OS callback only, never from UI
            else -> familyViewModel.onIntent(intent)
        }
    }

    private fun handleFamilyAdminIntent(intent: FamilyAdminIntent) {
        when (intent) {
            is FamilyAdminIntent.Stage -> {
                val identity = intent.change as? FamilyAdminChange.RegisterOwnerIdentity
                if (identity != null && appPassword.hasAccountEmailBinding() &&
                    !appPassword.verifyAccountEmail(identity.email)
                ) {
                    identity.wipeSecrets()
                    lockSensitiveUi()
                    return
                }
                if (!sessionGate.isUnlocked() ||
                    vaultViewModel.state.value !is VaultUiState.Ready
                ) {
                    lockSensitiveUi()
                    return
                }
                familyAdminViewModel.onIntent(intent)
                if (familyAdminViewModel.state.value is FamilyAdminUiState.AwaitingApproval) {
                    requestCredential(CredentialAction.MANAGE_FAMILY)
                }
            }
            FamilyAdminIntent.CommitAfterCredential -> Unit // ActivityResult only
            else -> familyAdminViewModel.onIntent(intent)
        }
    }

    private fun handleCustodyIntent(intent: CustodyHoldingIntent) {
        when (intent) {
            is CustodyHoldingIntent.StageAccount, is CustodyHoldingIntent.StageHolding,
            is CustodyHoldingIntent.StageAsset -> {
                if (!sessionGate.isUnlocked() || vaultViewModel.state.value !is VaultUiState.Ready) {
                    lockSensitiveUi()
                    return
                }
                custodyViewModel.onIntent(intent)
                if (custodyViewModel.state.value is CustodyHoldingUiState.AwaitingCredential) {
                    requestCredential(CredentialAction.REGISTER_CUSTODY)
                }
            }
            CustodyHoldingIntent.SaveAfterCredential -> Unit // OS callback only
            else -> custodyViewModel.onIntent(intent)
        }
    }

    private fun handleManualTradeIntent(intent: ManualTradeIntent) {
        when (intent) {
            ManualTradeIntent.RequestApproval -> {
                if (manualTradeViewModel.state.value !is ManualTradeUiState.AwaitingCredential ||
                    !sessionGate.isUnlocked() ||
                    vaultViewModel.state.value !is VaultUiState.Ready
                ) {
                    lockSensitiveUi()
                    return
                }
                requestCredential(CredentialAction.APPROVE_MANUAL_TRADE)
            }
            ManualTradeIntent.SaveAfterCredential -> Unit // OS callback only
            else -> manualTradeViewModel.onIntent(intent)
        }
    }

    /** The currently verified app password alone cannot authorize changing it. */
    private fun handleChangeLocalPassword(current: String, replacement: String) {
        if (!appPassword.hasVerifiedPassword() ||
            LocalPasswordRules.issues(replacement.toCharArray()).isNotEmpty() ||
            current == replacement || current.isEmpty()
        ) {
            localLoginError = "Enter valid, different current and new app passwords."
            return
        }
        pendingPreviousPassword?.fill('\u0000')
        pendingReplacementPassword?.fill('\u0000')
        pendingPreviousPassword = current.toCharArray()
        pendingReplacementPassword = replacement.toCharArray()
        requestCredential(CredentialAction.CHANGE_APP_PASSWORD)
    }

    private fun finishPasswordRotation() {
        val previous = pendingPreviousPassword
        val replacement = pendingReplacementPassword
        pendingPreviousPassword = null
        pendingReplacementPassword = null
        if (previous == null || replacement == null ||
            !sessionGate.canCommitFinancialChanges()
        ) {
            previous?.fill('\u0000')
            replacement?.fill('\u0000')
            lockSensitiveUi()
            return
        }
        val generation = ++loginOperationGeneration
        val revision = sessionGate.credentialEpoch()
        lifecycleScope.launch {
            try {
                val changed = withContext(Dispatchers.Default) {
                    appPassword.rotatePassword(previous, replacement)
                }
                if (generation == loginOperationGeneration &&
                    sessionGate.credentialEpoch() == revision
                ) {
                    lockSensitiveUi() // Forces login using the NEW app password.
                    localLoginError = if (changed) null else
                        "App password could not be changed. Verify the previous password."
                } else appPassword.lock()
            } catch (_: Exception) {
                lockSensitiveUi()
                localLoginError = "Password change was not confirmed; unlock and verify current status."
            } finally {
                previous.fill('\u0000')
                replacement.fill('\u0000')
            }
        }
    }

    private fun chooseDossierDestination() {
        if (!appPassword.hasVerifiedPassword() ||
            !sessionGate.isUnlocked() ||
            vaultViewModel.state.value !is VaultUiState.Ready
        ) {
            dossierUiStatus = EmergencyExportUiState.FAILED
            return
        }
        dossierUiStatus = EmergencyExportUiState.READY
        dossierPickerOpen = true
        try {
            dossierDestinationPicker.launch("GrowUrDhan-Family-Summary.csv")
        } catch (_: Exception) {
            dossierPickerOpen = false
            dossierUiStatus = EmergencyExportUiState.FAILED
        }
    }

    private fun cancelDossier() {
        ++dossierGeneration
        dossierPickerOpen = false
        dossierUri = null
        dossierUiStatus = EmergencyExportUiState.READY
    }

    private fun exportDossierAfterCredential() {
        val uri = dossierUri
        dossierUri = null
        if (uri == null || !sessionGate.canCommitFinancialChanges()) {
            dossierUiStatus = EmergencyExportUiState.FAILED
            return
        }
        val generation = ++dossierGeneration
        val revision = sessionGate.credentialEpoch()
        dossierUiStatus = EmergencyExportUiState.PREPARING
        lifecycleScope.launch {
            var bytes: ByteArray? = null
            try {
                vaultViewModel.awaitPendingCleanup()
                check(sessionGate.isCurrentSession(revision))
                repository.openOrCreate()
                check(sessionGate.isCurrentSession(revision))
                val book = repository.readBook()
                check(sessionGate.isCurrentSession(revision))
                val report = withContext(Dispatchers.Default) {
                    val portfolio = PortfolioEngine.derive(
                        book.holdings.map { it.toFinancialHolding() },
                        book.events.map { it.toFinancialEvent() },
                        LocalDate.now(), book.members.map { it.id },
                    )
                    val snapshot = HouseholdDashboardProjector.from(
                        portfolio, book.members.associate { it.id to it.displayName },
                    )
                    EmergencyDossierCsv.export(snapshot)
                }
                bytes = report
                check(sessionGate.isCurrentApproval(revision))
                check(generation == dossierGeneration)
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri, "wt")?.use { output ->
                        output.write(report)
                        output.flush()
                    } ?: throw IllegalStateException("Selected destination is unavailable.")
                }
                if (generation == dossierGeneration && sessionGate.isCurrentSession(revision)) {
                    dossierUiStatus = EmergencyExportUiState.SAVED
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation == dossierGeneration) {
                    dossierUiStatus = EmergencyExportUiState.FAILED
                }
            } finally {
                bytes?.fill(0)
            }
        }
    }

    private fun handleLegacyBackupIntent(intent: LegacyBackupIntent) {
        when (intent) {
            LegacyBackupIntent.ConfirmDryRun -> {
                if (legacyUiState is LegacyBackupUiState.DryRunReady &&
                    legacyExpectedSha != null && legacyRestoreIdentity != null &&
                    sessionGate.isUnlocked()
                ) {
                    requestCredential(CredentialAction.RESTORE_LEGACY_COMMIT)
                } else clearLegacySensitive()
            }
            LegacyBackupIntent.PrepareRestore -> {
                if (legacyFileUri != null && legacyUiState is LegacyBackupUiState.Preview &&
                    sessionGate.isUnlocked()
                ) {
                    legacyUiState = LegacyBackupUiState.EnterRecovery
                }
            }
            is LegacyBackupIntent.Restore -> {
                if (legacyFileUri == null || legacyUiState != LegacyBackupUiState.EnterRecovery ||
                    !sessionGate.isUnlocked()
                ) {
                    clearLegacySensitive()
                    return
                }
                if (appPassword.hasAccountEmailBinding() &&
                    !appPassword.verifyAccountEmail(intent.email)
                ) {
                    localLoginError = "The original account email must match this enrolled owner."
                    lockSensitiveUi()
                    return
                }
                legacyExpectedSha = null
                legacyRestoreIdentity?.wipeSecrets()
                legacyRestoreIdentity = LegacyRestoreIdentity(
                    intent.email, intent.password.toCharArray(), intent.pan,
                    intent.dateOfBirth, intent.firstQuestionId,
                    intent.firstAnswer.toCharArray(), intent.secondQuestionId,
                    intent.secondAnswer.toCharArray(),
                )
                requestCredential(CredentialAction.RESTORE_LEGACY)
            }
            LegacyBackupIntent.SelectFile -> {
                clearLegacySensitive()
                if (!appPassword.hasVerifiedPassword()) return
                legacyPickerOpen = true
                try {
                    legacyBackupPicker.launch(arrayOf("application/octet-stream", "*/*"))
                } catch (_: Exception) {
                    clearLegacySensitive()
                }
            }
            is LegacyBackupIntent.Authenticate -> {
                if (legacyFileUri == null ||
                    legacyUiState != LegacyBackupUiState.EnterCredentials ||
                    intent.email.length !in 5..254 ||
                    intent.password.length !in 1..1024
                ) {
                    legacyUiState = LegacyBackupUiState.Error(
                        "Enter the legacy backup account email and password.",
                    )
                    return
                }
                legacyEmail = intent.email
                legacySecret?.fill('\u0000')
                legacySecret = intent.password.toCharArray()
                requestCredential(CredentialAction.INSPECT_LEGACY)
            }
            LegacyBackupIntent.Cancel -> clearLegacySensitive()
        }
    }

    private fun handleRecoveryIntent(intent: PortableRecoveryIntent) {
        when (intent) {
            PortableRecoveryIntent.SelectExportDestination -> {
                if (!sessionGate.isUnlocked() ||
                    vaultViewModel.state.value !is VaultUiState.Ready
                ) {
                    lockSensitiveUi()
                    return
                }
                backupMode = PortableRecoveryMode.EXPORT
                backupPickerOpen = true
                try {
                    backupExportPicker.launch("GrowUrDhan-Backup.gud")
                } catch (_: Exception) {
                    lockSensitiveUi()
                }
            }
            PortableRecoveryIntent.SelectRestoreSource -> {
                backupMode = PortableRecoveryMode.RESTORE
                backupPickerOpen = true
                try {
                    backupRestorePicker.launch(arrayOf("application/octet-stream", "*/*"))
                } catch (_: Exception) {
                    lockSensitiveUi()
                }
            }
            is PortableRecoveryIntent.ConfirmPassphrase -> {
                val mode = backupMode
                if (mode == null || backupUri == null ||
                    (recoveryViewModel.state.value as? PortableRecoveryUiState.EnterPassphrase)
                        ?.mode != mode ||
                    intent.password.length !in 12..1024 ||
                    (mode == PortableRecoveryMode.EXPORT && intent.password != intent.repeated)
                ) {
                    recoveryViewModel.failInput()
                    return
                }
                backupPassword?.fill('\u0000')
                backupPassword = intent.password.toCharArray()
                requestCredential(CredentialAction.PORTABLE_BACKUP)
            }
            PortableRecoveryIntent.Cancel -> {
                backupPassword?.fill('\u0000')
                backupPassword = null
                backupMode = null
                backupUri = null
                recoveryViewModel.clearSensitiveState()
            }
        }
    }

    private fun chooseBackupFolder() {
        if (!appPassword.hasVerifiedPassword()) {
            lockSensitiveUi()
            return
        }
        folderPickerOpen = true
        try {
            backupFolderPicker.launch(null)
        } catch (_: Exception) {
            folderPickerOpen = false
        }
    }

    private fun startFolderBackup(password: CharArray) {
        if (backupFolderViewModel.stage(password) == null) return
        requestCredential(CredentialAction.BACKUP_FOLDER)
        if (pendingCredential != CredentialAction.BACKUP_FOLDER) {
            backupFolderViewModel.clearSensitiveState()
        }
    }

    private fun folderLabel(uri: Uri): String {
        val decoded = Uri.decode(uri.lastPathSegment ?: return "Chosen folder")
        val name = decoded.substringAfterLast(':').substringAfterLast('/').trim()
        return name.ifBlank { "Chosen folder" }
    }

    private fun requestCredential(action: CredentialAction) {
        if (pendingCredential != null) return
        if (action != CredentialAction.ENROLL_APP_PASSWORD &&
            action != CredentialAction.RECOVER_APP_PASSWORD &&
            !appPassword.hasVerifiedPassword()
        ) {
            lockSensitiveUi()
            return
        }
        val manager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!manager.isDeviceSecure) {
            // Never retain a selected PDF URI or password when credentials
            // are missing/removed while returning from the document picker.
            lockSensitiveUi()
            if (action == CredentialAction.OPEN_VAULT) {
                vaultViewModel.onIntent(VaultIntent.CredentialUnavailable)
            }
            return
        }
        val title = when (action) {
            CredentialAction.OPEN_VAULT -> "Unlock GrowUrDhan"
            CredentialAction.IMPORT_PDF -> "Verify identity before reading CAS PDF"
            CredentialAction.APPROVE_REVIEW -> "Approve financial statement"
            CredentialAction.ADD_FAMILY_MEMBER -> "Add family member"
            CredentialAction.MANAGE_FAMILY -> "Approve family, account or goal change"
            CredentialAction.REGISTER_CUSTODY -> "Register financial account or holding"
            CredentialAction.PORTABLE_BACKUP -> "Protect portable backup and recovery"
            CredentialAction.APPROVE_MANUAL_TRADE -> "Approve recorded buy or sell"
            CredentialAction.ENROLL_APP_PASSWORD -> "Protect GrowUrDhan login"
            CredentialAction.RECOVER_APP_PASSWORD -> "Recover GrowUrDhan password"
            CredentialAction.ISSUE_RECOVERY_CODE -> "Generate secure recovery code"
            CredentialAction.INSPECT_LEGACY -> "Inspect legacy encrypted backup"
            CredentialAction.RESTORE_LEGACY -> "Inspect authenticated original migration plan"
            CredentialAction.RESTORE_LEGACY_COMMIT -> "Commit verified migration to empty vault"
            CredentialAction.CHANGE_APP_PASSWORD -> "Change GrowUrDhan app password"
            CredentialAction.EXPORT_DOSSIER -> "Export household emergency summary"
            CredentialAction.BACKUP_FOLDER -> "Write locked backup folder"
        }
        val explanation = when (action) {
            CredentialAction.OPEN_VAULT ->
                "Confirm your device screen lock before opening the encrypted vault."
            CredentialAction.IMPORT_PDF ->
                "Confirm your device screen lock before accessing the selected PDF."
            CredentialAction.APPROVE_REVIEW ->
                "Confirm your device screen lock before committing these financial changes."
            CredentialAction.ADD_FAMILY_MEMBER ->
                "Confirm your device screen lock before saving a family member."
            CredentialAction.MANAGE_FAMILY ->
                "Review your private family, account or goal change and confirm this device lock."
            CredentialAction.REGISTER_CUSTODY ->
                "Confirm your screen lock before enrolling this account or CAS holding."
            CredentialAction.PORTABLE_BACKUP ->
                "Confirm your device screen lock before exporting or restoring encrypted records."
            CredentialAction.APPROVE_MANUAL_TRADE ->
                "Confirm the reviewed trade before adding an immutable ledger event."
            CredentialAction.ENROLL_APP_PASSWORD ->
                "Confirm your Android screen lock before registering this device's local app password."
            CredentialAction.RECOVER_APP_PASSWORD ->
                "Confirm your Android device identity before using the one-time recovery code."
            CredentialAction.ISSUE_RECOVERY_CODE ->
                "Confirm your Android device identity to issue a new offline recovery code."
            CredentialAction.INSPECT_LEGACY ->
                "Confirm device identity before reading the selected old GrowUrDhan backup."
            CredentialAction.RESTORE_LEGACY ->
                "Confirm identity before a read-only conversion report; no book data is written."
            CredentialAction.RESTORE_LEGACY_COMMIT ->
                "Confirm a second fresh device challenge before the exact-archive empty-vault commit."
            CredentialAction.CHANGE_APP_PASSWORD ->
                "Confirm your device lock to change your local GrowUrDhan password."
            CredentialAction.EXPORT_DOSSIER ->
                "Confirm before writing a deliberately unencrypted family CSV file."
            CredentialAction.BACKUP_FOLDER ->
                "Confirm your device screen lock before writing a locked .kosha file. The password is not stored."
        }
        val challenge = manager.createConfirmDeviceCredentialIntent(title, explanation)
        if (challenge == null) {
            lockSensitiveUi()
            return
        }
        pendingCredential = action
        try {
            credentialResult.launch(challenge)
        } catch (_: Exception) {
            pendingCredential = null
            lockSensitiveUi()
        }
    }

    private fun lockSensitiveUi() {
        ++loginOperationGeneration
        pendingEnrollmentPassword?.fill('\u0000')
        pendingEnrollmentPassword = null
        pendingEnrollmentEmail = null
        pendingRecoveryCodeInput?.fill('\u0000')
        pendingRecoveredPassword?.fill('\u0000')
        pendingRecoveryCodeInput = null
        pendingRecoveredPassword = null
        showForgotRecovery = false
        displayedRecoveryCode = null
        pendingPreviousPassword?.fill('\u0000')
        pendingReplacementPassword?.fill('\u0000')
        pendingPreviousPassword = null
        pendingReplacementPassword = null
        appPassword.lock()
        cancelDossier()
        clearLegacySensitive()
        pdfImportGuard.invalidate()
        dashboardViewModel.lock()
        sessionGate.lock()
        reviewViewModel.clearSensitiveState()
        familyViewModel.clearSensitiveState()
        familyAdminViewModel.clearSensitiveState()
        custodyViewModel.clearSensitiveState()
        manualTradeViewModel.clearSensitiveState()
        recoveryViewModel.clearSensitiveState()
        backupFolderViewModel.clearSensitiveState()
        backupPassword?.fill('\u0000')
        backupPassword = null
        backupMode = null
        backupUri = null
        backupPickerOpen = false
        folderPickerOpen = false
        pickedPdfOwner = null
        pickedPdfPassword?.fill('\u0000')
        pickedPdfPassword = null
        pickedPdfUri = null
        pdfPickerOpen = false
        vaultViewModel.onIntent(VaultIntent.Close)
    }

    override fun onStart() {
        super.onStart()
        licenceViewModel.refresh()
        timeoutHandler.removeCallbacks(timeoutCheck)
        timeoutHandler.postDelayed(timeoutCheck, 5_000L)
    }

    override fun onStop() {
        pdfImportGuard.invalidate()
        timeoutHandler.removeCallbacks(timeoutCheck)
        super.onStop()
        // Android's own credential confirmation activity may temporarily stop us.
        // A cancelled challenge locks; success must come from RESULT_OK only.
        dashboardViewModel.lock()
        when {
            dossierPickerOpen -> {
                sessionGate.lock()
                reviewViewModel.clearSensitiveState()
                familyViewModel.clearSensitiveState()
        familyAdminViewModel.clearSensitiveState()
                custodyViewModel.clearSensitiveState()
                manualTradeViewModel.clearSensitiveState()
                vaultViewModel.onIntent(VaultIntent.Close)
            }
            legacyPickerOpen -> {
                sessionGate.lock()
                reviewViewModel.clearSensitiveState()
                familyViewModel.clearSensitiveState()
        familyAdminViewModel.clearSensitiveState()
                custodyViewModel.clearSensitiveState()
                manualTradeViewModel.clearSensitiveState()
                vaultViewModel.onIntent(VaultIntent.Close)
            }
            backupPickerOpen -> {
                // SAF destination/source selection is not a valid financial
                // session. Password is not collected before the picker.
                sessionGate.lock()
                reviewViewModel.clearSensitiveState()
                familyViewModel.clearSensitiveState()
        familyAdminViewModel.clearSensitiveState()
                custodyViewModel.clearSensitiveState()
                manualTradeViewModel.clearSensitiveState()
                vaultViewModel.onIntent(VaultIntent.Close)
            }
            folderPickerOpen -> {
                // Choosing Drive or OneDrive is not a financial session.
                // Do not log the user out, and do not keep a backup password.
                backupFolderViewModel.clearSensitiveState()
                sessionGate.lock()
                reviewViewModel.clearSensitiveState()
                familyViewModel.clearSensitiveState()
        familyAdminViewModel.clearSensitiveState()
                custodyViewModel.clearSensitiveState()
                manualTradeViewModel.clearSensitiveState()
                vaultViewModel.onIntent(VaultIntent.Close)
            }
            pdfPickerOpen -> {
                // The external document chooser isn't an authenticated session.
                // Close SQLCipher and clear financial receipt state while away.
                sessionGate.lock()
                reviewViewModel.clearSensitiveState()
                familyViewModel.clearSensitiveState()
        familyAdminViewModel.clearSensitiveState()
                custodyViewModel.clearSensitiveState()
                manualTradeViewModel.clearSensitiveState()
                vaultViewModel.onIntent(VaultIntent.Close)
            }
            pendingCredential == null -> lockSensitiveUi()
            else -> {
                // The credential activity revokes the old session, and only its
                // RESULT_OK callback may start a new one.
                sessionGate.lock()
            }
        }
    }
    override fun onDestroy() {
        ++loginOperationGeneration
        pendingRecoveryCodeInput?.fill('\u0000')
        pendingRecoveredPassword?.fill('\u0000')
        pendingRecoveryCodeInput = null
        pendingRecoveredPassword = null
        displayedRecoveryCode = null
        showForgotRecovery = false
        pendingEnrollmentPassword?.fill('\u0000')
        pendingEnrollmentPassword = null
        pendingPreviousPassword?.fill('\u0000')
        pendingReplacementPassword?.fill('\u0000')
        pendingPreviousPassword = null
        pendingReplacementPassword = null
        cancelDossier()
        clearLegacySensitive()
        appPassword.lock()
        // A credential Activity can destroy/recreate its caller. No backup or
        // document password survives that process/activity boundary.
        backupPassword?.fill('\u0000')
        pickedPdfPassword?.fill('\u0000')
        backupPassword = null
        pickedPdfPassword = null
        backupFolderViewModel.clearSensitiveState()
        playBilling.shutdown()
        super.onDestroy()
    }
}

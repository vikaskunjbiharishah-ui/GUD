package `in`.growurdhan.app.presentation

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.growurdhan.domain.finance.BackupSchedule
import `in`.growurdhan.domain.vault.VaultRepository
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class BackupFolderUiState(
    val folderLabel: String? = null,
    val enabled: Boolean = false,
    val lastRunAt: String? = null,
    val lastError: String? = null,
    val lastKept: Int = 0,
    val busy: Boolean = false,
    val message: String? = null,
)

/** Names and times of locked copies already in the chosen folder. No file bytes. */
internal interface KoshaTree {
    val label: String
    fun files(): List<Pair<String, Long>>
    fun write(name: String, bytes: ByteArray)
    fun delete(name: String)
}

internal interface FolderMemory {
    fun read(): StoredFolder
    fun write(next: StoredFolder)
}

internal data class StoredFolder(
    val uri: String?,
    val label: String?,
    val enabled: Boolean,
    val lastRunAt: String?,
    val lastError: String?,
    val lastKept: Int,
)

/**
 * A folder of locked .kosha files. The backup password is held only until the
 * device credential returns, then wiped. It is never written into preferences.
 */
@HiltViewModel
class BackupFolderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vault: VaultRepository,
    private val session: VaultSessionGate,
) : ViewModel() {
    private val stateFlow = MutableStateFlow(BackupFolderUiState())
    val state: StateFlow<BackupFolderUiState> = stateFlow
    private var memoryOverride: FolderMemory? = null
    internal var openTree: (String) -> KoshaTree = { SafKoshaTree(context, Uri.parse(it)) }
    private var staged: CharArray? = null
    private var stagedEpoch = 0L

    fun refresh() {
        val stored = memory().read()
        stateFlow.value = BackupFolderUiState(
            folderLabel = stored.label,
            enabled = stored.enabled,
            lastRunAt = stored.lastRunAt,
            lastError = stored.lastError,
            lastKept = stored.lastKept,
        )
    }

    fun rememberFolder(uri: Uri, label: String) {
        val stored = memory().read()
        memory().write(stored.copy(uri = uri.toString(), label = label, lastError = null))
        refresh()
    }

    fun setEnabled(enabled: Boolean) {
        val stored = memory().read()
        if (enabled && stored.uri.isNullOrBlank()) {
            stateFlow.value = stateFlow.value.copy(
                message = "Choose a Google Drive or OneDrive folder first.",
            )
            return
        }
        memory().write(stored.copy(enabled = enabled))
        refresh()
    }

    /** Returns the credential epoch that must be replaced by a newer device approval. */
    fun stage(password: CharArray): Long? {
        val stored = runCatching { memory().read() }.getOrNull()
        val block = BackupSchedule.refusal(session.isUnlocked(), !stored?.uri.isNullOrBlank())
        if (block != null || password.size !in 12..1024) {
            password.fill('\u0000')
            stateFlow.value = stateFlow.value.copy(
                message = block ?: "Use a backup password of at least 12 characters.",
            )
            return null
        }
        staged?.fill('\u0000')
        staged = password
        stagedEpoch = session.credentialEpoch()
        stateFlow.value = stateFlow.value.copy(message = null)
        return stagedEpoch
    }

    fun writeAfterCredential() {
        val password = staged
        staged = null
        val epoch = stagedEpoch
        if (password == null || !session.canCommitFinancialChanges() || session.credentialEpoch() <= epoch) {
            password?.fill('\u0000')
            stateFlow.value = stateFlow.value.copy(
                message = "Unlock the vault to run scheduled backup. The password is not stored.",
            )
            return
        }
        val approved = session.credentialEpoch()
        stateFlow.value = stateFlow.value.copy(busy = true, message = null)
        viewModelScope.launch {
            try {
                val stored = memory().read()
                val uri = stored.uri ?: error("No folder")
                val tree = openTree(uri)
                vault.openOrCreate()
                val encrypted = vault.exportEncryptedBackup(password)
                try {
                    check(session.credentialEpoch() == approved && session.canCommitFinancialChanges()) {
                        "Backup approval expired before writing the folder."
                    }
                    val now = Instant.now()
                    val plan = BackupSchedule.planFolder(true, true, tree.files(), now, manual = true)
                        ?: error("No folder")
                    tree.write(plan.filename, encrypted)
                    plan.deleteNames.forEach { name -> runCatching { tree.delete(name) } }
                    val kept = tree.files().size
                    memory().write(stored.copy(lastError = null, lastKept = kept, lastRunAt = now.toString()))
                } finally {
                    encrypted.fill(0)
                }
                refresh()
                stateFlow.value = stateFlow.value.copy(message = "Locked backup saved. Only the last five copies are kept.")
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                val stored = runCatching { memory().read() }.getOrNull()
                if (stored != null) {
                    memory().write(stored.copy(lastError = "The locked backup could not be written."))
                }
                refresh()
                stateFlow.value = stateFlow.value.copy(
                    message = "The locked backup could not be written. The password was not saved.",
                )
            } finally {
                password.fill('\u0000')
                stateFlow.value = stateFlow.value.copy(busy = false)
            }
        }
    }

    fun clearSensitiveState() {
        staged?.fill('\u0000')
        staged = null
    }

    internal fun useMemoryForTest(memory: FolderMemory) {
        memoryOverride = memory
    }

    private fun memory(): FolderMemory = memoryOverride ?: PrefsFolderMemory(context).also { memoryOverride = it }
}

internal class PrefsFolderMemory(private val context: Context) : FolderMemory {
    private val prefs get() = context.getSharedPreferences("gud.backup.folder", Context.MODE_PRIVATE)
    override fun read() = StoredFolder(
        prefs.getString("uri", null),
        prefs.getString("label", null),
        prefs.getBoolean("enabled", false),
        prefs.getString("lastRun", null),
        prefs.getString("lastError", null),
        prefs.getInt("lastKept", 0),
    )
    override fun write(next: StoredFolder) {
        prefs.edit()
            .putString("uri", next.uri)
            .putString("label", next.label)
            .putBoolean("enabled", next.enabled)
            .putString("lastRun", next.lastRunAt)
            .putString("lastError", next.lastError)
            .putInt("lastKept", next.lastKept)
            .apply()
    }
}

internal class SafKoshaTree(private val context: Context, private val tree: Uri) : KoshaTree {
    override val label: String get() = tree.lastPathSegment ?: "Chosen folder"

    override fun files(): List<Pair<String, Long>> {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            tree, DocumentsContract.getTreeDocumentId(tree),
        )
        val found = mutableListOf<Pair<String, Long>>()
        context.contentResolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            ),
            null, null, null,
        )?.use { cursor ->
            val name = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val time = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            while (cursor.moveToNext()) {
                val shown = cursor.getString(name) ?: continue
                if (shown.endsWith(".kosha")) found += shown to cursor.getLong(time)
            }
        }
        return found
    }

    override fun write(name: String, bytes: ByteArray) {
        val parent = DocumentsContract.buildDocumentUriUsingTree(
            tree, DocumentsContract.getTreeDocumentId(tree),
        )
        val created = DocumentsContract.createDocument(
            context.contentResolver, parent, "application/octet-stream", name,
        ) ?: error("Selected folder cannot be written.")
        context.contentResolver.openOutputStream(created)?.use { it.write(bytes) }
            ?: error("Selected folder cannot be written.")
    }

    override fun delete(name: String) {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            tree, DocumentsContract.getTreeDocumentId(tree),
        )
        context.contentResolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            ),
            null, null, null,
        )?.use { cursor ->
            val nameColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val idColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            while (cursor.moveToNext()) {
                if (cursor.getString(nameColumn) != name) continue
                val document = DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(idColumn))
                DocumentsContract.deleteDocument(context.contentResolver, document)
            }
        }
    }
}

package `in`.growurdhan.domain.finance

import java.time.Instant

/**
 * Scheduled backup rules from src/lib/backupSchedule.ts.
 * Keep the last five locked files. The password is not stored.
 * Picking a Drive or OneDrive folder stays on the device.
 */
object BackupSchedule {
    const val KEEP_LAST = 5
    val defaults = ScheduleSettings(enabled = false, onOpen = true, onCasImport = true)

    /** The files beyond the newest [keep], which are the ones to delete. */
    fun extras(times: List<Pair<String, Long>>, keep: Int = KEEP_LAST): List<String> {
        if (keep < 0) return times.map { it.first }
        return times.sortedByDescending { it.second }.drop(keep).map { it.first }
    }

    fun shouldRun(settings: ScheduleSettings, reason: String, unlocked: Boolean, hasFolder: Boolean): String? {
        if (!settings.enabled) return null
        if (reason == "open" && !settings.onOpen) return null
        if (reason == "cas" && !settings.onCasImport) return null
        if (!unlocked) return "Unlock the vault to run scheduled backup. The password is not stored."
        if (!hasFolder) return "Choose a Google Drive or OneDrive folder first."
        return "run"
    }

    /**
     * Decide a folder backup without touching a disk or a password.
     * A manual backup still refuses while the vault is locked or no folder is chosen.
     * The new file is counted before the oldest copies are chosen for deletion.
     */
    fun planFolder(
        unlocked: Boolean,
        hasFolder: Boolean,
        existing: List<Pair<String, Long>>,
        now: Instant,
        manual: Boolean = false,
        settings: ScheduleSettings = defaults,
    ): FolderWrite? {
        if (!manual) {
            if (shouldRun(settings, "open", unlocked, hasFolder) != "run") return null
        } else if (!unlocked || !hasFolder) {
            return null
        }
        val filename = BackupFile.suggestedScheduledName(now)
        val withNew = existing + (filename to now.toEpochMilli())
        return FolderWrite(filename, extras(withNew))
    }

    fun refusal(unlocked: Boolean, hasFolder: Boolean): String? = when {
        !unlocked -> "Unlock the vault to run scheduled backup. The password is not stored."
        !hasFolder -> "Choose a Google Drive or OneDrive folder first."
        else -> null
    }
}

data class ScheduleSettings(
    val enabled: Boolean,
    val onOpen: Boolean,
    val onCasImport: Boolean,
    val folderLabel: String? = null,
    val lastRunAt: String? = null,
    val lastError: String? = null,
    val lastKept: Int = 0,
)

/** A locked file to write, and the older copies that no longer fit in the last five. */
data class FolderWrite(val filename: String, val deleteNames: List<String>)

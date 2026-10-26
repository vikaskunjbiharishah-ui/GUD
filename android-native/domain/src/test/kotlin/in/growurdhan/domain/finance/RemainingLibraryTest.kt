package `in`.growurdhan.domain.finance

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Goldens from kosha.test.ts sections Z, AA, and the device envelope. */
class RemainingLibraryTest {
    @Test fun termsBackupScheduleAndReviewPrompt() {
        assertEquals("2026-10-26", Terms.VERSION)
        assertEquals("GUD-TOS-2026-10-26-v5", Terms.TEXT_ID)
        assertEquals(21, Terms.sections.size)
        val text = Terms.plainText()
        assertFalse(text.contains("Vikas Shah", ignoreCase = true))
        assertFalse(text.contains("Proprietor"))
        assertTrue(text.contains("licence to use the App is granted and distributed solely by GrowUrDhan"))
        assertTrue(text.contains("₹99"))
        assertFalse(text.contains("₹249") || text.contains("₹499") || text.contains("₹250") || text.contains("₹199"))
        assertTrue(text.contains("one paid product: an annual subscription at ₹99 per year"))
        assertFalse(text.contains("perpetual licence"))
        assertTrue(text.contains("Investment Adviser"))
        assertTrue(text.contains("SEBI"))
        assertTrue(text.contains("1,000"))
        assertTrue(text.contains("Information Technology Act"))
        assertTrue(text.contains("memory only"))
        assertTrue(text.contains("Google Drive") && text.contains("OneDrive"))
        assertFalse(Terms.hasAccepted(null, null))
        val accepted = Terms.accept(Instant.parse("2026-10-06T00:00:00Z"))
        assertEquals(Terms.VERSION, accepted.version)
        assertTrue(Terms.hasAccepted(accepted.version, accepted.textId))

        assertEquals(5, BackupSchedule.KEEP_LAST)
        assertFalse(BackupSchedule.defaults.enabled)
        assertTrue(BackupSchedule.defaults.onOpen && BackupSchedule.defaults.onCasImport)
        val extra = BackupSchedule.extras(listOf("a" to 1L, "b" to 5L, "c" to 3L, "d" to 4L, "e" to 2L, "f" to 6L))
        assertEquals(listOf("a"), extra)
        assertEquals(
            "Unlock the vault to run scheduled backup. The password is not stored.",
            BackupSchedule.shouldRun(BackupSchedule.defaults.copy(enabled = true), "open", unlocked = false, hasFolder = true),
        )
        assertNull(BackupSchedule.shouldRun(BackupSchedule.defaults, "open", unlocked = true, hasFolder = true))

        val whenSaved = Instant.parse("2026-09-20T12:34:56.000Z")
        val kept = listOf("a" to 1L, "b" to 2L, "c" to 3L, "d" to 4L, "e" to 5L)
        val planned = BackupSchedule.planFolder(true, true, kept, whenSaved, manual = true)
        assertEquals("growurdhan_20260920_123456.kosha", planned?.filename)
        assertEquals(listOf("a"), planned?.deleteNames)
        assertEquals(null, BackupSchedule.planFolder(false, true, kept, whenSaved, manual = true))
        assertEquals(
            "Unlock the vault to run scheduled backup. The password is not stored.",
            BackupSchedule.refusal(false, true),
        )

        assertEquals(7, FeedbackPrompt.FIRST_DELAY_DAYS)
        assertEquals(2, FeedbackPrompt.SNOOZE_DAYS)
        assertEquals("in.growurdhan.app", FeedbackPrompt.PLAY_PACKAGE_ID)
        val now = Instant.parse("2026-09-21T12:00:00.000Z")
        val fresh = FeedbackPrompt.seedFirstSeen(FeedbackState(), "2026-09-21T12:00:00.000Z", now)
        assertEquals("2026-09-21T12:00:00.000Z", fresh.firstSeenAt)
        assertFalse(FeedbackPrompt.shouldShow(fresh, now))
        assertFalse(FeedbackPrompt.shouldShow(fresh, Instant.parse("2026-09-27T11:59:00.000Z")))
        assertTrue(FeedbackPrompt.shouldShow(fresh, Instant.parse("2026-09-28T12:00:00.000Z")))
        val snoozed = FeedbackPrompt.snooze(fresh, Instant.parse("2026-09-28T12:00:00.000Z"))
        assertFalse(FeedbackPrompt.shouldShow(snoozed, Instant.parse("2026-09-28T12:00:00.000Z")))
        assertFalse(FeedbackPrompt.shouldShow(snoozed, Instant.parse("2026-09-29T12:00:00.000Z")))
        assertTrue(FeedbackPrompt.shouldShow(snoozed, Instant.parse("2026-09-30T12:00:01.000Z")))
        val rated = FeedbackPrompt.complete(fresh, Instant.parse("2026-09-28T12:00:00.000Z"))
        assertFalse(FeedbackPrompt.shouldShow(rated, Instant.parse("2026-12-01T00:00:00.000Z")))
        var nag = FeedbackPrompt.seedFirstSeen(FeedbackState(), "2026-01-01T00:00:00.000Z", now)
        repeat(FeedbackPrompt.MAX_PROMPTS) { nag = FeedbackPrompt.snooze(nag, now) }
        assertFalse(FeedbackPrompt.shouldShow(nag, Instant.parse("2026-12-01T00:00:00.000Z")))
        FeedbackPrompt.resetSession()
        val due = FeedbackPrompt.seedFirstSeen(FeedbackState(), "2026-01-01T00:00:00.000Z", now)
        assertTrue(FeedbackPrompt.claim(due, now))
        assertFalse(FeedbackPrompt.claim(due, now))
        FeedbackPrompt.resetSession()
    }

    @Test fun deviceEnvelopeAndUnlockMemory() {
        val salt = "0123456789abcdef".toByteArray()
        val packed = DeviceEnvelope.seal("vault-bytes".toByteArray(), salt)
        assertEquals(DeviceEnvelope.MAGIC, "GUDENC01")
        assertTrue(DeviceEnvelope.hasMagic(packed))
        assertEquals("vault-bytes", DeviceEnvelope.open(packed, salt).toString(Charsets.UTF_8))
        assertFalse(packed.toString(Charsets.UTF_8).contains("vault-bytes"))

        SessionGate.clear()
        assertNull(SessionGate.peek())
        SessionGate.remember(" Vikas@Example.com ", "Abcd1234!")
        assertEquals(SessionSecret("vikas@example.com", "Abcd1234!"), SessionGate.peek())
        SessionGate.clear()
        assertNull(SessionGate.peek())
        assertFalse(SessionGate.pickerOpen())
        SessionGate.beginPicker()
        assertTrue(SessionGate.pickerOpen())
        SessionGate.endPicker()
        assertFalse(SessionGate.pickerOpen())
        assertEquals(30_000, SessionGate.LOCK_AFTER_MS)
    }
}

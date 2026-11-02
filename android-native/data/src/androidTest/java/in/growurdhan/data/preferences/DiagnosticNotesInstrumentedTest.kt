package `in`.growurdhan.data.preferences

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import `in`.growurdhan.domain.finance.DiagnosticsLog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/** Local notes are DataStore, not a Compose remember. The process was not killed. */
@RunWith(AndroidJUnit4::class)
class DiagnosticNotesInstrumentedTest {
    @Test fun diagnosticNotesSurviveANewRepository(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val first = DataStoreAppearanceRepository(context)
        val noted = DiagnosticsLog.record(true, emptyList(), "Member ABCDE1234F checked")
        first.saveDiagnosticNotes(noted)
        val loaded = DataStoreAppearanceRepository(context).diagnosticNotes.first()
        assertEquals(listOf("Member [redacted] checked"), loaded)
        assertFalse(loaded.any { it.contains("ABCDE1234F") })
        first.saveDiagnosticNotes(DiagnosticsLog.clear())
        assertEquals(emptyList<String>(), DataStoreAppearanceRepository(context).diagnosticNotes.first())
    }
}

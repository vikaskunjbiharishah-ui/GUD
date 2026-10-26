package `in`.growurdhan.domain.finance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Goldens from the statement-mail section of src/lib/kosha.test.ts. */
class StatementMailTest {
    private fun file(name: String) = NamedFile(name)

    @Test fun onlyTheNamedPersonsFileIsDownloaded() {
        val cdsl = StatementMail.classify("AUG2026_AA07970379_TXN.pdf")
        requireNotNull(cdsl)
        assertEquals("AA07970379", cdsl.casId)
        assertEquals("TXN", cdsl.variant)
        assertEquals("HLD", StatementMail.classify("MAR2022_AA07970379_HLD.pdf")?.variant)
        assertEquals("1120330238", StatementMail.classify("1120330238_Aug2026.pdf")?.pran)
        assertEquals("2026-08", StatementMail.monthKey("AUG2026_AA07970379_TXN.pdf"))
        assertEquals("CDSL", StatementMail.issuerFromSender("eCAS <eCAS@cdslstatement.com>"))
        assertEquals("NPS", StatementMail.issuerFromSender("nps-statements@mailer.proteantech.in"))
        assertEquals("CAMS", StatementMail.issuerFromSender("donotreply@camsonline.com"))

        val members = listOf(
            MailMember("a", gmailSync = true, casId = "AA07970379", pran = "1120330238"),
            MailMember("b", gmailSync = false, casId = "ZZ00000000"),
        )
        assertEquals(MailDecision.Download("a"), StatementMail.decide("AUG2026_AA07970379_TXN.pdf", "eCAS@cdslstatement.com", members))
        val other = StatementMail.decide("AUG2026_BB11111111_TXN.pdf", "eCAS@cdslstatement.com", members)
        assertEquals(MailDecision.Skip(true, "CAS ID does not match a member"), other)
        assertEquals(MailDecision.Download("a"), StatementMail.decide("1120330238_Aug2026.pdf", "nps-statements@mailer.proteantech.in", members))
        assertEquals(MailDecision.Skip(false, "no member has Gmail sync on"), StatementMail.decide("AUG2026_AA07970379_TXN.pdf", "eCAS@cdslstatement.com", listOf(members[1])))
        assertEquals(
            MailDecision.Skip(false, "CDSL file name has no CAS ID"),
            StatementMail.decide("statement.pdf", "eCAS@cdslstatement.com", members),
        )
        assertEquals(
            MailDecision.Skip(true, "not a holdings statement"),
            StatementMail.decide("capitalgain.pdf", "donotreply@camsonline.com", members),
        )
        assertEquals(MailDecision.Download(null), StatementMail.decide("statement.pdf", "donotreply@camsonline.com", members))

        val bothOn = listOf(
            MailMember("primary", gmailSync = true, pran = "110123981321", panForStatements = "ABCDE1234F"),
            MailMember("child", gmailSync = true, pran = "120000000001"),
        )
        assertEquals(
            MailDecision.Download(null),
            StatementMail.decide("1113981321_Aug2026.pdf", "nps-statements@mailer.proteantech.in", bothOn),
        )
        assertEquals("primary", StatementMail.assignOwner(bothOn, ParsedStatement("NPS", investorPran = "110123981321"), "1113981321_Aug2026.pdf"))
        assertEquals("child", StatementMail.assignOwner(bothOn, ParsedStatement("NPS", investorPran = "120000000001"), "9999999999_Aug2026.pdf"))
        assertNull(StatementMail.assignOwner(
            listOf(MailMember("only", gmailSync = true, pran = "110123981321", panForStatements = "ABCDE1234F")),
            ParsedStatement("NPS", investorPran = "120000000001"),
            "9999999999_Aug2026.pdf",
        ))
        val childFiles = StatementMail.filesForMember(
            listOf(file("1113981321_Aug2026.pdf")),
            MailMember("child", gmailSync = true, pran = "120000000001"),
        )
        assertEquals(0, childFiles.keep.size)
        assertEquals(0, childFiles.older.size)

        val picked = StatementMail.newest(listOf(
            file("JUL2021_AA07970379_TXN.pdf"),
            file("AUG2026_AA07970379_HLD.pdf"),
            file("AUG2026_AA07970379_TXN.pdf"),
            file("1120330238_Jul2021.pdf"),
            file("1120330238_Aug2026.pdf"),
        ))
        assertEquals(2, picked.keep.size)
        assertTrue(picked.keep.any { it.filename == "AUG2026_AA07970379_TXN.pdf" })
        assertTrue(picked.keep.any { it.filename == "1120330238_Aug2026.pdf" })
        assertEquals(3, picked.older.size)
        assertFalse(picked.keep.any { it.filename.endsWith("_HLD.pdf") })

        val history = StatementMail.filesForMember(listOf(
            file("JUL2021_AA07970379_TXN.pdf"),
            file("AUG2026_AA07970379_TXN.pdf"),
            file("1113981321_Jan2024.pdf"),
            file("1113981321_Aug2026.pdf"),
        ), MailMember("a", casId = "AA07970379", pran = "1113981321", casIdSource = "profile", pranSource = "profile"))
        assertEquals(
            "1113981321_Aug2026.pdf,AUG2026_AA07970379_TXN.pdf",
            history.keep.map { it.filename }.sorted().joinToString(","),
        )
        val after = StatementMail.filesForMember(listOf(
            file("1113981321_Jan2024.pdf"),
            file("1113981321_Aug2026.pdf"),
        ), MailMember("a", pran = "1113981321", pranSource = "import", pranSyncAfter = "2024-01-31"))
        assertEquals("1113981321_Aug2026.pdf", after.keep.joinToString(",") { it.filename })

        val afterFloor = StatementMail.filesForMember(listOf(
            file("1113981321_Mar2026.pdf"),
            file("1113981321_Aug2026.pdf"),
        ), MailMember("a", pran = "1113981321", pranSource = "import", pranSyncAfter = "2026-03-31"))
        assertEquals(listOf("1113981321_Aug2026.pdf"), afterFloor.keep.map { it.filename })
        assertEquals(listOf("1113981321_Mar2026.pdf"), afterFloor.older.map { it.filename })
        assertTrue(StatementMail.pranMatches("110123981321", "0123981321"))
        assertTrue(StatementMail.hasStatementId(members[0]))
    }
}

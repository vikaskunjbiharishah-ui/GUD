package `in`.growurdhan.domain.finance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Goldens from src/lib/kosha.test.ts sections N and N2, plus the statement date. */
class CasStatementsTest {
    @Test fun npsPasswordAndPerMemberDatesMatchTauri() {
        assertEquals("vika2203", CasStatements.npsPassword("Vikas Kunjbihari Shah", "1986-03-22"))
        assertEquals("kiah0702", CasStatements.npsPassword("Kiah Vikas Shah", "2018-02-07"))
        assertEquals("om0501", CasStatements.npsPassword("Om Shah", "2020-01-05"))
        assertEquals("amit1508", CasStatements.npsPassword("Amit", "15/08/1990"))
        assertNull(CasStatements.npsPassword("Vikas", null))
        assertNull(CasStatements.npsPassword("123 Shah", "1986-03-22"))

        assertEquals(
            listOf("ABCPS1234K", "vika2203"),
            CasStatements.passwordsToTry(typed = "ABCPS1234K", pan = "ABCPS1234K", name = "Vikas Shah", dateOfBirth = "1986-03-22"),
        )
        assertEquals(
            listOf("kiah0702"),
            CasStatements.passwordsToTry(name = "Kiah Vikas Shah", dateOfBirth = "2018-02-07"),
        )
        assertEquals(
            listOf("secret", "vika2203", "ABCPS1234K"),
            CasStatements.passwordsToTry(typed = " secret ", pan = "ABCPS1234K", name = "Vikas Shah", dateOfBirth = "1986-03-22"),
        )

        val raw = """
            {"asOf":"2026-02-28","byOwner":{
              "fam_1":{"CDSL":{"fileType":"CDSL","filename":"a.pdf","pdfGrandTotal":1,"parsedGrandTotal":1,"complete":true,"periodTo":"2026-01-31"}},
              "fam_2":{"CDSL":{"fileType":"CDSL","filename":"b.pdf","pdfGrandTotal":1,"parsedGrandTotal":1,"complete":true,"periodTo":"2026-02-28"}},
              "fam_3":{"CAMS":{"fileType":"CAMS","filename":"c.pdf","pdfGrandTotal":1,"parsedGrandTotal":1,"complete":true,"periodTo":"2026-01-31"}}
            }}
        """.trim()
        val state = CasStatements.parseLatest(raw)
        assertEquals("2026-02-28", state.asOf)
        assertEquals("2026-01-31", CasStatements.latestForOwner(state, "fam_1"))
        assertEquals("2026-02-28", CasStatements.latestForOwner(state, "fam_2"))
        assertNull(CasStatements.latestForOwner(state, "fam_x"))
        val listed = CasStatements.asOfByMember(state, listOf(
            StatementMember("fam_1", "Vikas"),
            StatementMember("fam_2", "Nancy"),
            StatementMember("fam_3", "HUF"),
            StatementMember("fam_x", "Nobody"),
        ))
        assertEquals("Vikas:2026-01-31|Nancy:2026-02-28|HUF:2026-01-31", listed.joinToString("|") { "${it.name}:${it.asOf}" })
        assertEquals(CasLatest(null, emptyMap()).asOf, CasStatements.parseLatest("{").asOf)
        assertEquals(emptyMap<String, Map<String, CasFileStamp>>(), CasStatements.parseLatest("{\"asOf\":\"2026-01-01\"}").byOwner)

        val newer = CasStatements.upsert(
            state, "fam_1",
            CasFileStamp("CAMS", "cams.pdf", "2026-03-31"),
            "2026-04-01T00:00:00Z",
        )
        assertEquals("2026-03-31", newer.asOf)
        assertEquals("2026-03-31", CasStatements.latestForOwner(newer, "fam_1"))
        assertEquals("2026-04-01T00:00:00Z", newer.byOwner.getValue("fam_1").getValue("CAMS").importedAt)

        assertEquals("31 March 2026", CasStatements.formatDate("2026-03-31"))
        assertEquals("31 Mar 2026", CasStatements.formatDateShort("2026-03-31"))
        assertEquals("", CasStatements.formatDate(null))
        assertEquals("not-a-date", CasStatements.formatDate("not-a-date"))
        assertEquals("₹ 12,34,567.50", CasStatements.formatInr(1234567.5))
        assertEquals("-₹ 18,500.00", CasStatements.formatInr(-18500.0))
        assertEquals("₹ 0.00", CasStatements.formatInr(Double.NaN))
    }
}

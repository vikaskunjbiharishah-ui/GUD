package `in`.growurdhan.domain.market

import java.time.LocalDate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OfficialAmfiNavParserTest {
    private val day = LocalDate.parse("2026-10-16")
    private val heading =
        "Scheme Code;ISIN Div Payout/ ISIN Growth;ISIN Div Reinvestment;" +
        "Scheme Name;Net Asset Value;Date\n"

    @Test fun acceptsOnlyValidDatedAmfiIsinAndNeverInventsNav() {
        val file = heading + """
Some Mutual Fund
Open Ended Schemes
123456;INF000000001;INF000000002;Synthetic Direct Growth;128.5471;15-Oct-2026
123457;-;-;Synthetic no ISIN;12.0000;15-Oct-2026
123458;INF000000003;-;Synthetic outdated;21.0000;05-May-2022
123459;INF000000004;-;Synthetic future;99.0000;17-Oct-2026
123460;INF000000005;-;Synthetic text price;N.A.;15-Oct-2026
""".trimIndent()
        val rows = OfficialAmfiNavParser.parse(file, day)
        assertEquals(2, rows.size)
        assertEquals(setOf("INF000000001", "INF000000002"), rows.map { it.isin }.toSet())
        assertEquals(1_285_471L, rows.first().closePrice.raw)
        assertEquals("123456", rows.first().amfiCode)
        assertEquals(OfficialMarketSource.AMFI, rows.first().source)
    }

    @Test fun refusesHtmlMissingHeaderAndFutureOnlyReports() {
        assertThrows(IllegalArgumentException::class.java) {
            OfficialAmfiNavParser.parse("<html>login</html>", day)
        }
        assertThrows(IllegalArgumentException::class.java) {
            OfficialAmfiNavParser.parse("untrusted;csv;row", day)
        }
        assertThrows(IllegalArgumentException::class.java) {
            OfficialAmfiNavParser.parse(
                heading + "123456;INF000000001;-;Future fund;14.0500;18-Oct-2026",
                day)
        }
    }

    @Test fun staleNavIsRetainedWithDateAndNeverTreatedAsCurrent() {
        val rows = OfficialAmfiNavParser.parse(
            heading + "123456;INF000000001;-;Stale scheme;14.0500;06-Oct-2026",
            day)
        assertEquals(QuoteCondition.STALE, rows.single().condition(day))
    }
}

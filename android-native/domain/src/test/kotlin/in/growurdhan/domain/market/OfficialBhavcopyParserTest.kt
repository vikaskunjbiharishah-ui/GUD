package `in`.growurdhan.domain.market

import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OfficialBhavcopyParserTest {
    private val today = LocalDate.parse("2026-09-10")
    private val header = "TradDt,BizDt,Sgmt,Src,FinInstrmTp,FinInstrmId,ISIN,TckrSymb,SctySrs,FinInstrmNm,ClsPric"

    @Test fun nseCloseKeepsTheFileDateAndSkipsOtherSeries() {
        val csv = """
            $header
            2026-09-09,2026-09-09,CM,NSE,STK,1,INE000000001,SYN,EQ,Synthetic Share,123.4500
            2026-09-09,2026-09-09,CM,NSE,STK,2,INE000000002,SKIP,ZZ,Skipped Series,10
            2026-09-09,2026-09-09,CM,NSE,SOV,3,INE000000003,GOLD,GB,Synthetic Gold Bond,5000.0000
            2026-09-11,2026-09-11,CM,NSE,STK,4,INE000000004,FUTURE,EQ,Future Close,10
            2026-09-09,2026-09-09,CM,NSE,STK,5,INE000000005,ZERO,EQ,Zero Close,0
        """.trimIndent()
        val parsed = OfficialBhavcopyParser.parse(csv, OfficialMarketSource.NSE, today)
        assertTrue(parsed.understood)
        assertEquals(listOf("INE000000001", "INE000000003"), parsed.rows.map { it.isin })
        val share = parsed.rows[0]
        assertEquals(OfficialMarketSource.NSE, share.source)
        assertEquals(LocalDate.parse("2026-09-09"), share.closeDate)
        assertEquals(1_234_500L, share.closePrice.raw)
        assertEquals("SYN", share.symbol)
        assertEquals("EQUITY", share.category)
        assertEquals("GOLD_SGB", parsed.rows[1].category)
        assertEquals(50_000_000L, parsed.rows[1].closePrice.raw)
    }

    @Test fun bseKeepsOnlyStockRowsAndDoesNotInventAPrice() {
        val csv = """
            $header
            09-Sep-2026,09-Sep-2026,CM,BSE,STK,9,INE000000009,BSESYN,A,Synthetic BSE Share,"1,234.50"
            09-Sep-2026,09-Sep-2026,CM,BSE,IDX,8,INE000000008,INDEX,A,Not a stock,10
        """.trimIndent()
        val parsed = OfficialBhavcopyParser.parse(csv, OfficialMarketSource.BSE, today)
        assertEquals(1, parsed.rows.size)
        assertEquals(OfficialMarketSource.BSE, parsed.rows.single().source)
        assertEquals(LocalDate.parse("2026-09-09"), parsed.rows.single().closeDate)
        assertEquals(12_345_000L, parsed.rows.single().closePrice.raw)
        assertThrows(IllegalArgumentException::class.java) {
            OfficialBhavcopyParser.parse("<html>no</html>", OfficialMarketSource.NSE, today)
        }
    }
}

package `in`.growurdhan.app.market

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import `in`.growurdhan.domain.market.OfficialMarketSource
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfficialExchangeCloseGatewayInstrumentedTest {
    private val today = LocalDate.parse("2026-09-10")
    private val csv = """
        TradDt,BizDt,Sgmt,Src,FinInstrmTp,FinInstrmId,ISIN,TckrSymb,SctySrs,FinInstrmNm,ClsPric
        2026-09-09,2026-09-09,CM,NSE,STK,1,INE000000001,SYN,EQ,Synthetic Share,2208.0000
    """.trimIndent()

    @Test fun refreshUsesTheOfficialUrlCachesTheCloseAndThenReadsOffline() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cache = java.io.File(context.noBackupFilesDir, "exchange-close-NSE.csv")
        cache.delete()
        val requested = mutableListOf<String>()
        val gateway = OfficialExchangeCloseGateway(context) { url, referer ->
            requested += url
            assertEquals("https://www.nseindia.com/all-reports", referer)
            assertTrue(url.startsWith("https://nsearchives.nseindia.com/content/cm/BhavCopy_NSE_CM_0_0_0_"))
            assertTrue(url.endsWith("_F_0000.csv.zip"))
            assertTrue('?' !in url)
            if (url.contains("20260910")) return@OfficialExchangeCloseGateway null
            if (url.contains("20260909")) zip(csv) else null
        }
        val fresh = gateway.refresh(OfficialMarketSource.NSE, today)
        assertEquals("INE000000001", fresh.single().isin)
        assertEquals(LocalDate.parse("2026-09-09"), fresh.single().closeDate)
        assertEquals(22_080_000L, fresh.single().closePrice.raw)
        assertEquals(OfficialMarketSource.NSE, fresh.single().source)
        var calls = 0
        val offline = OfficialExchangeCloseGateway(context) { _, _ ->
            calls += 1
            throw AssertionError("cached read must not use the network")
        }.cached(OfficialMarketSource.NSE, today)
        assertEquals(0, calls)
        assertEquals(fresh.single().closePrice.raw, offline.single().closePrice.raw)
        val poisoned = OfficialExchangeCloseGateway(context) { _, _ ->
            "<html>not a bhavcopy</html>".toByteArray()
        }
        try {
            poisoned.refresh(OfficialMarketSource.NSE, today)
            throw AssertionError("HTML must not replace the cache")
        } catch (_: IllegalStateException) {
            // expected after the six-day walk finds no official file
        }
        assertEquals("INE000000001", gateway.cached(OfficialMarketSource.NSE, today).single().isin)
        cache.delete()
    }

    @Test fun bseCsvIsCachedWithItsOwnAsOfDate() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cache = java.io.File(context.noBackupFilesDir, "exchange-close-BSE.csv")
        cache.delete()
        val bse = csv.replace("NSE", "BSE").replace(",EQ,", ",A,")
            .replace("SctySrs", "SctySrs")
        val gateway = OfficialExchangeCloseGateway(context) { url, referer ->
            assertEquals("https://www.bseindia.com/", referer)
            assertTrue(url.contains("BhavCopy_BSE_CM_0_0_0_20260910_F_0000.CSV"))
            bse.toByteArray()
        }
        val rows = gateway.refresh(OfficialMarketSource.BSE, today)
        assertEquals(OfficialMarketSource.BSE, rows.single().source)
        assertEquals(LocalDate.parse("2026-09-09"), rows.single().closeDate)
        cache.delete()
    }

    private fun zip(text: String): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("BhavCopy.csv"))
            zip.write(text.toByteArray())
            zip.closeEntry()
        }
        return out.toByteArray()
    }
}

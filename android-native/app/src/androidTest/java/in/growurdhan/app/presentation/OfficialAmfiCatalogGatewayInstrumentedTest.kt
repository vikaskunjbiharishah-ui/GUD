package `in`.growurdhan.app.presentation

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import `in`.growurdhan.app.market.OfficialAmfiCatalogGateway
import `in`.growurdhan.domain.market.OfficialMarketSource
import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfficialAmfiCatalogGatewayInstrumentedTest {
    @Test fun validatedPublicCacheLoadsOfflineWithoutAnyNetworkRequest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cache = File(context.noBackupFilesDir, "amfi-public-nav-cache.txt")
        val previous = if (cache.exists()) cache.readBytes() else null
        try {
            cache.writeText(
                "Scheme Code;ISIN Div Payout/ ISIN Growth;ISIN Div Reinvestment;" +
                    "Scheme Name;Net Asset Value;Date\n" +
                    "123456;INF000000001;-;Synthetic Fund Growth;10.1234;15-Oct-2026\n",
            )
            val result = OfficialAmfiCatalogGateway(context).cached(
                LocalDate.parse("2026-10-16"))
            assertEquals(1, result.size)
            assertEquals("INF000000001", result.single().isin)
            assertEquals(OfficialMarketSource.AMFI, result.single().source)
            assertEquals(101_234L, result.single().closePrice.raw)
            // A malformed public file may never be promoted to an official row.
            cache.writeText("<html>Not a market report</html>")
            assertTrue(OfficialAmfiCatalogGateway(context).cached(
                LocalDate.parse("2026-10-16")).isEmpty())
        } finally {
            if (previous != null) cache.writeBytes(previous) else cache.delete()
        }
    }
}

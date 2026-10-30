package `in`.growurdhan.domain.market

import `in`.growurdhan.core.model.MoneyScaled
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ManualPriceTest {
    private val today = LocalDate.parse("2026-10-30")
    private fun rupees(value: Long) = MoneyScaled(value * 10_000L)

    @Test fun aMissingCloseStaysMissing() {
        val priced = ManualPrice.resolve(null, null, null, null, today)
        assertNull(priced.amount)
        assertEquals(PriceProvenance.MISSING, priced.provenance)
        assertEquals(ManualPrice.MISSING_LABEL, priced.label)
        val zero = ManualPrice.resolve(MoneyScaled(0), today, MoneyScaled(0), today, today)
        assertNull(zero.amount)
        assertEquals(PriceProvenance.MISSING, zero.provenance)
    }

    @Test fun aManualPriceKeepsItsDateAndTurnsStale() {
        val fresh = ManualPrice.resolve(
            null, null, rupees(125), LocalDate.parse("2026-10-28"), today,
        )
        assertEquals(rupees(125).raw, fresh.amount!!.raw)
        assertEquals(PriceProvenance.MANUAL, fresh.provenance)
        assertEquals(false, fresh.stale)
        assertTrue(fresh.label.contains("Manual price you typed."))
        val stale = ManualPrice.resolve(
            rupees(90), LocalDate.parse("2026-10-01"), rupees(125), LocalDate.parse("2026-10-01"), today,
        )
        assertEquals(PriceProvenance.MANUAL, stale.provenance)
        assertTrue(stale.stale)
        assertTrue(stale.label.contains("Stale as of 2026-10-01."))
        assertEquals(rupees(125).raw, stale.amount!!.raw)
    }
}

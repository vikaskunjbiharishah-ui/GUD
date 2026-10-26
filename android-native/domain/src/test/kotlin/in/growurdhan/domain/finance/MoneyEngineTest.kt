package `in`.growurdhan.domain.finance

import java.math.BigInteger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * Goldens executed from src/lib/money.ts. kosha.test.ts does not cover this engine.
 */
class MoneyEngineTest {
    @Test fun scalesMatchTauri() {
        assertEquals(10_000L, MoneyEngine.MONEY_SCALE)
        assertEquals(100_000_000L, MoneyEngine.QTY_SCALE)
    }

    @Test fun fromRupeesMatchesJavascriptRounding() {
        assertEquals(bi("0"), MoneyEngine.fromRupees(0))
        assertEquals(bi("10000"), MoneyEngine.fromRupees(1))
        assertEquals(bi("100"), MoneyEngine.fromRupees(0.01))
        assertEquals(bi("1"), MoneyEngine.fromRupees(0.0001))
        assertEquals(bi("1"), MoneyEngine.fromRupees(0.00005))
        assertEquals(bi("15000"), MoneyEngine.fromRupees(1.5))
        assertEquals(bi("17205000"), MoneyEngine.fromRupees(1720.5))
        assertEquals(bi("2900"), MoneyEngine.fromRupees(0.29))
        assertEquals(bi("-193000"), MoneyEngine.fromRupees(-19.3))
        assertEquals(bi("38987500"), MoneyEngine.fromRupees(3898.75))
        assertEquals(bi("123456789"), MoneyEngine.fromRupees(12345.6789))
        assertEquals(bi("10050"), MoneyEngine.fromRupees(1.005))
        assertEquals(bi("0"), MoneyEngine.fromRupees(Double.NaN))
        assertEquals(bi("14800000"), MoneyEngine.fromRupees("1,480"))
        assertEquals(bi("1234565000"), MoneyEngine.fromRupees("1,23,456.50"))
        assertEquals(bi("0"), MoneyEngine.fromRupees(""))
        assertEquals(bi("0"), MoneyEngine.fromRupees("  "))
        assertEquals(bi("0"), MoneyEngine.fromRupees("abc"))
        assertEquals(bi("0"), MoneyEngine.fromRupees("₹100"))
        assertEquals(bi("123450"), MoneyEngine.fromRupees("12.345"))
        assertEquals(bi("1000000"), MoneyEngine.fromRupees("1e2"))
        assertEquals(bi("17205000"), MoneyEngine.fromRupees("  1720.50 "))
    }

    @Test fun toRupeesMatchesJavascript() {
        assertEquals(0.0, MoneyEngine.toRupees(0))
        assertEquals(1.0, MoneyEngine.toRupees(10_000))
        assertEquals(0.0001, MoneyEngine.toRupees(1))
        assertEquals(0.015, MoneyEngine.toRupees(150))
        assertEquals(12_345_678.0, MoneyEngine.toRupees(bi("123456780000")))
        assertEquals(-19.3, MoneyEngine.toRupees(-193_000))
        assertEquals(3898.75, MoneyEngine.toRupees(38_987_500))
        assertEquals(2.5, MoneyEngine.toRupees(25_000.0))
        assertEquals(1234.5678, MoneyEngine.toRupees("12345678"))
        assertEquals(12.3456, MoneyEngine.toRupees("1,23,456"))
        assertEquals(0.0, MoneyEngine.toRupees(""))
        assertEquals(0.0, MoneyEngine.toRupees("abc"))
        assertEquals(0.00125, MoneyEngine.toRupees("12.5"))
        assertEquals(1.0, MoneyEngine.toRupees("  10000 "))
    }

    @Test fun quantitiesMatchJavascriptIncludingStringFallback() {
        assertEquals(bi("0"), MoneyEngine.fromQty(0))
        assertEquals(bi("100000000"), MoneyEngine.fromQty(1))
        assertEquals(bi("45000000000"), MoneyEngine.fromQty(450))
        assertEquals(bi("1425084200000"), MoneyEngine.fromQty(14250.842))
        assertEquals(bi("864012000000"), MoneyEngine.fromQty(8640.12))
        assertEquals(bi("1"), MoneyEngine.fromQty(1e-8))
        assertEquals(bi("0"), MoneyEngine.fromQty(4e-9))
        assertEquals(bi("0"), MoneyEngine.fromQty(Double.NaN))
        assertEquals(bi("-100000000"), MoneyEngine.fromQty(-1))
        assertEquals(bi("1425084200000"), MoneyEngine.fromQty("14,250.842"))
        assertEquals(bi("0"), MoneyEngine.fromQty("nope"))
        assertEquals(bi("50000000"), MoneyEngine.fromQty("0.5"))
        assertEquals(1.0, MoneyEngine.toQty(100_000_000))
        assertEquals(450.0, MoneyEngine.toQty(bi("45000000000")))
        assertEquals(14250.842, MoneyEngine.toQty(bi("1425084200000")), 0.0)
        assertEquals(2.0, MoneyEngine.toQty(200_000_000.0))
        assertEquals(0.1, MoneyEngine.toQty("10000000"), 0.0)
        assertEquals(10_000_000.0, MoneyEngine.toQty("10000000.0"), 0.0)
        assertEquals(0.10000001, MoneyEngine.toQty("10000001.0"), 0.0)
        assertEquals(0.200000005, MoneyEngine.toQty("20000000.5"), 0.0)
        assertEquals(100.5, MoneyEngine.toQty("100.5"), 0.0)
        assertEquals(14250.842, MoneyEngine.toQty("14,250.842"), 0.0)
        assertEquals(0.0, MoneyEngine.toQty(""), 0.0)
        assertEquals(0.0, MoneyEngine.toQty("abc"), 0.0)
    }

    @Test fun indianGroupingAndRupeeDisplayMatchTauriExamples() {
        assertEquals("1,23,45,678", MoneyEngine.groupIndian(12_345_678))
        assertEquals("1,23,456", MoneyEngine.groupIndian(123_456))
        assertEquals("1,234", MoneyEngine.groupIndian(1_234))
        assertEquals("123", MoneyEngine.groupIndian(123))
        assertEquals("0", MoneyEngine.groupIndian(0))
        assertEquals("-1,23,45,678", MoneyEngine.groupIndian(-12_345_678))
        assertEquals("-5", MoneyEngine.groupIndian(-5))
        assertEquals("-1,235", MoneyEngine.groupIndian(-1234.9))
        assertEquals("₹ 1,23,45,678.00", MoneyEngine.formatRupees(bi("123456780000")))
        assertEquals("₹ 1,23,45,678", MoneyEngine.formatRupees(bi("123456780000"), includePaise = false))
        assertEquals("₹ 0.00", MoneyEngine.formatRupees(0))
        assertEquals("₹ 0", MoneyEngine.formatRupees(0, includePaise = false))
        assertEquals("₹ 1.00", MoneyEngine.formatRupees(10_050))
        assertEquals("₹ 1.01", MoneyEngine.formatRupees(10_150))
        assertEquals("₹ -19.30", MoneyEngine.formatRupees(-193_000))
        assertEquals("₹ -19", MoneyEngine.formatRupees(-193_000, includePaise = false))
        assertEquals("₹ -12,345.67", MoneyEngine.formatRupees(bi("-123456789")))
        assertEquals("₹ 3,898.75", MoneyEngine.formatRupees(38_987_500))
        assertEquals("₹ 1,234.56", MoneyEngine.formatRupees("1,23,45,678"))
        assertEquals("₹ 0.00", MoneyEngine.formatRupees("12.5"))
        assertThrows(NumberFormatException::class.java) { MoneyEngine.formatRupees("abc") }
    }

    @Test fun compactAndChangeMatchTauri() {
        assertEquals("2.47 crore", MoneyEngine.formatCompact(bi("247000000000")))
        assertEquals("1.23 crore", MoneyEngine.formatCompact(bi("123456780000")))
        assertEquals("1.00 crore", MoneyEngine.formatCompact(bi("100000000000")))
        assertEquals("18.60 lakh", MoneyEngine.formatCompact(bi("18600000000")))
        assertEquals("1.00 lakh", MoneyEngine.formatCompact(bi("1000000000")))
        assertEquals("100.00 lakh", MoneyEngine.formatCompact(bi("99999990000")))
        assertEquals("₹ 75,200", MoneyEngine.formatCompact(bi("752000000")))
        assertEquals("₹ 99,999", MoneyEngine.formatCompact(bi("999990000")))
        assertEquals("₹ 10,050", MoneyEngine.formatCompact(bi("100500000")))
        assertEquals("-2.47 crore", MoneyEngine.formatCompact(bi("-247000000000")))
        assertEquals("-₹ 19", MoneyEngine.formatCompact(-193_000))
        assertEquals("₹ 0", MoneyEngine.formatCompact(0))
        assertThrows(NumberFormatException::class.java) { MoneyEngine.formatCompact("nope") }

        val up = MoneyEngine.formatChange(bi("452000000"), 1.25)
        assertEquals("▲", up.arrow)
        assertEquals("Up", up.word)
        assertEquals("₹ 45,200", up.amountStr)
        assertEquals("+1.25%", up.percentStr)
        assertEquals("▲ Up ₹ 45,200 (+1.25%)", up.fullText)
        assertEquals(true, up.isPositive)

        val down = MoneyEngine.formatChange(bi("-123000000"), -0.45)
        assertEquals("▼ Down ₹ 12,300 (-0.45%)", down.fullText)
        assertEquals(true, down.isNegative)

        val flat = MoneyEngine.formatChange(0, 0.0)
        assertEquals("—", flat.arrow)
        assertEquals("Unchanged", flat.word)
        assertEquals("0.00%", flat.percentStr)
        assertEquals("— Unchanged (0.00%)", flat.fullText)
        assertEquals(true, flat.isZero)

        assertEquals("▲ Up ₹ 4 (+1.20%)", MoneyEngine.formatChange("45,200", 1.2).fullText)
        assertEquals("▲ Up ₹ 0 (+0.00%)", MoneyEngine.formatChange(150, -0.0).fullText)
    }

    @Test fun quantityDisplayUsesIndianGrouping() {
        assertEquals("139", MoneyEngine.formatQty(bi("13900000000")))
        assertEquals("14.52", MoneyEngine.formatQty(bi("1452000000")))
        assertEquals("324.891", MoneyEngine.formatQty(bi("32489100000")))
        assertEquals("14,250.842", MoneyEngine.formatQty(bi("1425084200000")))
        assertEquals("1", MoneyEngine.formatQty(100_000_000))
        assertEquals("1.005", MoneyEngine.formatQty(100_500_000))
        assertEquals("1.2346", MoneyEngine.formatQty(bi("123456789"), 4))
        assertEquals("1.23", MoneyEngine.formatQty(122_500_000, 2))
        assertEquals("139", MoneyEngine.formatQty("13900000000"))
        assertEquals("1.235", MoneyEngine.formatQty(1.2345 * 1e8))
        assertEquals("-139", MoneyEngine.formatQty(bi("-13900000000")))
        assertEquals("12,34,567", MoneyEngine.formatQty(bi("123456700000000")))
    }

    private fun bi(text: String) = BigInteger(text)
}

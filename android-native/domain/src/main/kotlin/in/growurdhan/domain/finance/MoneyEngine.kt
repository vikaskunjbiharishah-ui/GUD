package `in`.growurdhan.domain.finance

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow

/**
 * Kotlin port of src/lib/money.ts.
 *
 * Display and Tauri-compatible conversion only. SQLite book values stay
 * [in.growurdhan.core.model.MoneyScaled], which rounds a decimal exactly.
 * This object keeps the JavaScript float and string quirks on purpose.
 */
object MoneyEngine {
    const val MONEY_SCALE: Long = 10_000L
    const val QTY_SCALE: Long = 100_000_000L

    private val moneyScale = BigInteger.valueOf(MONEY_SCALE)
    private val qtyScale = BigInteger.valueOf(QTY_SCALE)
    private val jsFloat = Regex("""^[+-]?(?:\d+\.?\d*|\.\d+)(?:[eE][+-]?\d+)?""")
    private val jsInteger = Regex("""[+-]?\d+""")

    fun fromRupees(rupees: Double): BigInteger {
        if (rupees.isNaN()) return BigInteger.ZERO
        return BigInteger.valueOf(java.lang.Math.round(rupees * MONEY_SCALE.toDouble()))
    }

    fun fromRupees(rupees: Int): BigInteger = fromRupees(rupees.toDouble())
    fun fromRupees(rupees: Long): BigInteger = fromRupees(rupees.toDouble())

    fun fromRupees(rupees: String): BigInteger {
        val num = jsParseFloat(stripCommas(rupees))
        if (num.isNaN()) return BigInteger.ZERO
        return fromRupees(num)
    }

    fun toRupees(scaled: BigInteger): Double = scaled.toDouble() / MONEY_SCALE.toDouble()
    fun toRupees(scaled: Long): Double = toRupees(BigInteger.valueOf(scaled))
    fun toRupees(scaled: Int): Double = toRupees(scaled.toLong())
    fun toRupees(scaled: Double): Double = scaled / MONEY_SCALE.toDouble()

    fun toRupees(scaled: String): Double {
        val clean = stripCommas(scaled)
        if (clean.isEmpty()) return 0.0
        val asInt = strictInteger(clean)
        if (asInt != null) return toRupees(asInt)
        val num = jsParseFloat(clean)
        return if (num.isNaN()) 0.0 else num / MONEY_SCALE.toDouble()
    }

    fun fromQty(units: Double): BigInteger {
        if (units.isNaN()) return BigInteger.ZERO
        return BigInteger.valueOf(java.lang.Math.round(units * QTY_SCALE.toDouble()))
    }

    fun fromQty(units: Int): BigInteger = fromQty(units.toDouble())
    fun fromQty(units: Long): BigInteger = fromQty(units.toDouble())

    fun fromQty(units: String): BigInteger {
        val num = jsParseFloat(stripCommas(units))
        if (num.isNaN()) return BigInteger.ZERO
        return fromQty(num)
    }

    fun toQty(scaled: BigInteger): Double = scaled.toDouble() / QTY_SCALE.toDouble()
    fun toQty(scaled: Long): Double = toQty(BigInteger.valueOf(scaled))
    fun toQty(scaled: Int): Double = toQty(scaled.toLong())
    fun toQty(scaled: Double): Double = scaled / QTY_SCALE.toDouble()

    fun toQty(scaled: String): Double {
        val clean = stripCommas(scaled)
        if (clean.isEmpty()) return 0.0
        val asInt = strictInteger(clean)
        if (asInt != null) return toQty(asInt)
        val num = jsParseFloat(clean)
        if (num.isNaN()) return 0.0
        return if (num > 10_000_000.0) num / QTY_SCALE.toDouble() else num
    }

    fun groupIndian(n: Double): String {
        val negative = n < 0
        val floored = floor(n)
        val digits = BigDecimal.valueOf(abs(floored)).toBigInteger().toString()
        val grouped = groupDigits(digits)
        return if (negative && floored != 0.0) "-$grouped" else grouped
    }

    fun groupIndian(n: BigInteger): String = groupIndian(n.toDouble())
    fun groupIndian(n: Long): String = groupIndian(n.toDouble())
    fun groupIndian(n: Int): String = groupIndian(n.toDouble())

    fun formatRupees(scaled: BigInteger, includePaise: Boolean = true): String =
        formatMinor(scaled, includePaise)

    fun formatRupees(scaled: Long, includePaise: Boolean = true): String =
        formatMinor(BigInteger.valueOf(scaled), includePaise)

    fun formatRupees(scaled: Int, includePaise: Boolean = true): String =
        formatRupees(scaled.toLong(), includePaise)

    fun formatRupees(scaled: Double, includePaise: Boolean = true): String =
        formatMinor(BigInteger.valueOf(java.lang.Math.round(scaled)), includePaise)

    fun formatRupees(scaled: String, includePaise: Boolean = true): String =
        formatMinor(minorUnits(scaled), includePaise)

    fun formatCompact(scaled: BigInteger): String = formatCompactMinor(scaled)
    fun formatCompact(scaled: Long): String = formatCompactMinor(BigInteger.valueOf(scaled))
    fun formatCompact(scaled: Int): String = formatCompact(scaled.toLong())
    fun formatCompact(scaled: Double): String = formatCompactMinor(BigInteger.valueOf(java.lang.Math.round(scaled)))
    fun formatCompact(scaled: String): String = formatCompactMinor(minorUnits(scaled))

    fun formatChange(changeMinor: BigInteger, percent: Double): MoneyChange =
        formatChangeMinor(changeMinor, percent)

    fun formatChange(changeMinor: Long, percent: Double): MoneyChange =
        formatChangeMinor(BigInteger.valueOf(changeMinor), percent)

    fun formatChange(changeMinor: Int, percent: Double): MoneyChange =
        formatChange(changeMinor.toLong(), percent)

    fun formatChange(changeMinor: Double, percent: Double): MoneyChange =
        formatChangeMinor(BigInteger.valueOf(java.lang.Math.round(changeMinor)), percent)

    fun formatChange(changeMinor: String, percent: Double): MoneyChange =
        formatChangeMinor(minorUnits(changeMinor), percent)

    fun formatQty(scaled: BigInteger, decimals: Int = 3): String = formatQtyNumber(toQty(scaled), decimals)
    fun formatQty(scaled: Long, decimals: Int = 3): String = formatQty(BigInteger.valueOf(scaled), decimals)
    fun formatQty(scaled: Int, decimals: Int = 3): String = formatQty(scaled.toLong(), decimals)
    fun formatQty(scaled: Double, decimals: Int = 3): String = formatQtyNumber(toQty(scaled), decimals)
    fun formatQty(scaled: String, decimals: Int = 3): String = formatQtyNumber(toQty(scaled), decimals)

    private fun formatMinor(minor: BigInteger, includePaise: Boolean): String {
        val negative = minor.signum() < 0
        val absMinor = minor.abs()
        val whole = absMinor.divide(moneyScale)
        val paise = absMinor.remainder(moneyScale).divide(BigInteger.valueOf(100))
        val grouped = groupIndian(whole)
        val sign = if (negative) "-" else ""
        if (!includePaise) return "₹ $sign$grouped"
        return "₹ $sign$grouped.${paise.toString().padStart(2, '0')}"
    }

    private fun formatCompactMinor(minor: BigInteger): String {
        val rupees = minor.divide(moneyScale).toDouble()
        val negative = rupees < 0
        val magnitude = abs(rupees)
        val sign = if (negative) "-" else ""
        return when {
            magnitude >= 10_000_000.0 -> "$sign${jsToFixed(magnitude / 10_000_000.0, 2)} crore"
            magnitude >= 100_000.0 -> "$sign${jsToFixed(magnitude / 100_000.0, 2)} lakh"
            else -> "$sign₹ ${groupIndian(magnitude)}"
        }
    }

    private fun formatChangeMinor(minor: BigInteger, percent: Double): MoneyChange {
        val positive = minor.signum() > 0
        val negative = minor.signum() < 0
        val zero = minor.signum() == 0
        val arrow: String
        val word: String
        when {
            positive -> {
                arrow = "▲"
                word = "Up"
            }
            negative -> {
                arrow = "▼"
                word = "Down"
            }
            else -> {
                arrow = "—"
                word = "Unchanged"
            }
        }
        val amountStr = formatRupees(minor.abs(), includePaise = false)
        val percentStr = buildString {
            if (positive) append('+') else if (negative) append('-')
            append(jsToFixed(abs(percent), 2))
            append('%')
        }
        val fullText = if (zero) {
            "— Unchanged (0.00%)"
        } else {
            "$arrow $word $amountStr ($percentStr)"
        }
        return MoneyChange(arrow, word, amountStr, percentStr, fullText, positive, negative, zero)
    }

    private fun formatQtyNumber(raw: Double, decimals: Int): String {
        if (jsIsInteger(raw)) return groupIndian(raw)
        val negative = raw < 0
        val rounded = BigDecimal(abs(raw).toString()).setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros()
        val plain = rounded.toPlainString()
        val dot = plain.indexOf('.')
        val head = if (dot < 0) plain else plain.substring(0, dot)
        val frac = if (dot < 0) "" else plain.substring(dot + 1)
        val body = if (frac.isEmpty()) groupDigits(head) else groupDigits(head) + "." + frac
        return if (negative && rounded.signum() != 0) "-$body" else body
    }

    private fun minorUnits(scaled: String): BigInteger {
        val clean = stripCommas(scaled)
        val asInt = strictInteger(if (clean.isEmpty()) "0" else clean)
        if (asInt != null) return asInt
        val parsed = jsParseFloat(if (clean.isEmpty()) "0" else clean)
        if (!parsed.isFinite()) {
            throw NumberFormatException("The number cannot be converted to a BigInt because it is not an integer")
        }
        return BigInteger.valueOf(java.lang.Math.round(parsed))
    }

    private fun strictInteger(clean: String): BigInteger? {
        if (!jsInteger.matches(clean)) return null
        return BigInteger(clean)
    }

    private fun stripCommas(raw: String): String = raw.replace(",", "").trim()

    private fun jsParseFloat(raw: String): Double {
        val match = jsFloat.find(raw.trim()) ?: return Double.NaN
        if (match.range.first != 0) return Double.NaN
        return match.value.toDouble()
    }

    private fun jsIsInteger(raw: Double): Boolean =
        raw.isFinite() && raw == floor(raw) && abs(raw) <= 9_007_199_254_740_992.0

    private fun jsToFixed(n: Double, digits: Int): String {
        val factor = 10.0.pow(digits)
        val rounded = java.lang.Math.round(abs(n) * factor)
        val digitsText = rounded.toString().padStart(digits, '0')
        val split = digitsText.length - digits
        val head = if (split == 0) "0" else digitsText.substring(0, split)
        return head + "." + digitsText.substring(split)
    }

    private fun groupDigits(absoluteInteger: String): String {
        if (absoluteInteger.length <= 3) return absoluteInteger
        val lastThree = absoluteInteger.takeLast(3)
        val head = absoluteInteger.dropLast(3)
        val groupedHead = head.reversed().chunked(2).joinToString(",").reversed()
        return "$groupedHead,$lastThree"
    }
}

data class MoneyChange(
    val arrow: String,
    val word: String,
    val amountStr: String,
    val percentStr: String,
    val fullText: String,
    val isPositive: Boolean,
    val isNegative: Boolean,
    val isZero: Boolean,
)

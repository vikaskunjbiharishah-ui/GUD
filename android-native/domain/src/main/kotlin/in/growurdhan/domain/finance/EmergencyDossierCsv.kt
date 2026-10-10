package `in`.growurdhan.domain.finance

import java.math.BigDecimal

/**
 * User-directed, UNENCRYPTED emergency handover report. Never auto-upload.
 * Refuse unreconciled books; don't fabricate prices, historical costs or gains.
 */
object EmergencyDossierCsv {
    const val MAX_ROWS = 10_000
    const val MAX_BYTES = 4 * 1024 * 1024

    private fun quoted(value: String): String {
        val clean = value.replace(Regex("[\\r\\n\\t\\u0000-\\u001f]"), " ")
            .trim().take(300)
        // Prevent CSV formula injection in common spreadsheet readers.
        val safeValue = if (clean.firstOrNull() in listOf('=', '+', '-', '@')) "'$clean"
            else clean
        return "\"" + safeValue.replace("\"", "\"\"") + "\""
    }

    private fun line(vararg fields: String) = fields.joinToString(",") { quoted(it) } + "\r\n"

    fun export(dashboard: HouseholdDashboard): ByteArray {
        require(dashboard.recordedHoldings.size <= MAX_ROWS) {
            "Too many rows for an on-device emergency summary."
        }
        check(PortfolioInsightsProjector.from(dashboard).reconciled) {
            "Unreconciled financial records cannot be exported as verified totals."
        }
        val out = StringBuilder()
        out.append(line("GrowUrDhan", "UNENCRYPTED USER-REQUESTED EMERGENCY SUMMARY"))
        out.append(line("As of", dashboard.asOf.toString()))
        out.append(line("Source", "Stored on-device values, NOT live market quotations"))
        out.append(line("Warning", "Do not treat unknown historical cost as investment gains"))
        out.append(line("Assets INR", dashboard.assets.rupees().toPlainString()))
        out.append(line("Liabilities INR", dashboard.liabilities.rupees().toPlainString()))
        out.append(line("Recorded net worth INR", dashboard.netWorth.rupees().toPlainString()))
        out.append(line("Member", "Category", "Holding", "Quantity units",
            "Recorded value INR", "Cost verified", "Liability"))
        for (position in dashboard.recordedHoldings) {
            out.append(line(
                position.ownerName ?: "Family member (unnamed)",
                position.category, position.name,
                BigDecimal.valueOf(position.quantityScaled, 8).toPlainString(),
                position.recordedValue.rupees().toPlainString(),
                if (position.unknownHistoricalCost) "No" else "Not marked unknown",
                if (position.isLiability) "Yes" else "No",
            ))
            require(out.length <= MAX_BYTES) { "Emergency summary is oversized." }
        }
        val bytes = out.toString().toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_BYTES)
        return bytes
    }
}

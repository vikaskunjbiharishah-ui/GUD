package `in`.growurdhan.domain.finance

/**
 * Text stand-in for a CDSL or NPS statement. This is not a PDF and it does
 * not call the frozen parser. Real registrar PDFs stay on the owner's phone.
 *
 * SYNTHETIC-ISSUER 1
 * issuer: CDSL|NPS
 * statement-date: YYYY-MM-DD
 * source-sha256: 64 lowercase hex
 * holding: name|category|quantity|unit price|invested|isin|account
 *
 * invested may be empty. account is the CDSL BO/client id or the 12-digit PRAN.
 */
object SyntheticIssuerStatement {
    private val sha = Regex("^[a-f0-9]{64}$")
    private val date = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    fun parse(text: String): CasParsedReport {
        require(text.length <= 1_000_000) { "Synthetic statement is too large." }
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        require(lines.firstOrNull() == "SYNTHETIC-ISSUER 1") { "Not a synthetic issuer statement." }
        val fields = linkedMapOf<String, String>()
        val holdings = mutableListOf<CasParsedHolding>()
        for (line in lines.drop(1)) {
            if (line.startsWith("holding:")) {
                holdings += holding(line.removePrefix("holding:").trim())
            } else {
                val cut = line.indexOf(':')
                require(cut > 0) { "Synthetic statement line is not a field." }
                val key = line.substring(0, cut).trim()
                require(key in setOf("issuer", "statement-date", "source-sha256")) {
                    "Unexpected synthetic statement field."
                }
                require(fields.put(key, line.substring(cut + 1).trim()) == null) {
                    "Repeated synthetic statement field."
                }
            }
        }
        val issuer = fields["issuer"]
        require(issuer == "CDSL" || issuer == "NPS") { "Synthetic issuer must be CDSL or NPS." }
        val end = fields["statement-date"]
        require(end != null && date.matches(end)) { "Synthetic statement date is invalid." }
        val fingerprint = fields["source-sha256"]
        require(fingerprint != null && sha.matches(fingerprint)) { "Synthetic fingerprint is invalid." }
        require(holdings.isNotEmpty()) { "Synthetic statement has no holdings." }
        return CasParsedReport(
            engine = "PYTHON_CASPARSER",
            fileType = issuer,
            complete = true,
            statementDate = end,
            warnings = emptyList(),
            sourceSha256 = fingerprint,
            holdings = holdings,
        )
    }

    private fun holding(body: String): CasParsedHolding {
        val parts = body.split('|')
        require(parts.size == 7) { "Synthetic holding must have seven fields." }
        val invested = parts[4].trim().ifEmpty { null }
        return CasParsedHolding(
            name = parts[0].trim(),
            category = parts[1].trim(),
            quantityDecimal = parts[2].trim(),
            unitPriceDecimal = parts[3].trim(),
            investedDecimal = invested,
            isin = parts[5].trim(),
            folio = parts[6].trim(),
        )
    }
}

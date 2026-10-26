package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.MoneyScaled

/**
 * Last close and NAV lookup from src/lib/eodPrices.ts.
 * A catalog price replaces the rupee price only. Quantity is left alone.
 * Downloading the files stays with the price reader on the device.
 */
object EodPrices {
    private val eodSource = Regex("""^(NSE|BSE|AMFI)\b""", RegexOption.IGNORE_CASE)
    private val growth = Regex("""\bgrowth\b""")
    private val direct = Regex("""\bdirect\b""")
    private val payout = Regex("""\bidcw\b|\bdividend\b|\breinvestment\b""")

    fun isEodSource(source: String?): Boolean = !source.isNullOrBlank() && eodSource.containsMatchIn(source)

    fun listed(row: CatalogRow): ListedClose? {
        val isin = row.isin.trim().uppercase()
        if (isin.length < 12 || !row.priceRupees.isFinite() || row.priceRupees <= 0.0) return null
        val category = if (row.category == "MUTUAL_FUND" || row.category == "GOLD_SGB") row.category else "EQUITY"
        return ListedClose(
            isin = isin,
            name = row.name.ifBlank { row.symbol ?: row.nse ?: row.bse ?: isin },
            symbol = row.symbol ?: row.nse ?: row.bse,
            nseSymbol = row.nse,
            bseSymbol = row.bse,
            amfiCode = row.amfi?.takeIf { it.isNotBlank() },
            category = category,
            close = MoneyScaled(MoneyEngine.fromRupees(row.priceRupees).toLong()),
            priceDate = row.date,
            source = row.source.ifBlank { "EOD close" },
        )
    }

    fun apply(holdings: List<HeldPrice>, catalog: List<ListedClose>): PricedBook {
        val byIsin = catalog.associateBy { it.isin.uppercase() }
        var updated = 0
        val next = holdings.map { holding ->
            val isin = holding.isin?.uppercase().orEmpty()
            if (isin.isEmpty()) return@map holding
            if (holding.category == "LIABILITY" || holding.category == "CASH_BANK") return@map holding
            if (holding.category == "REAL_ESTATE" && !reitLike(holding.subCategory)) return@map holding
            val row = byIsin[isin] ?: return@map holding
            val stamped = holding.copy(
                price = row.close,
                priceDate = "${row.priceDate}T16:00:00+05:30",
                priceSource = row.source,
                stale = false,
            )
            if (holding.price.raw == row.close.raw && isEodSource(holding.priceSource)) stamped
            else {
                updated += 1
                stamped
            }
        }
        return PricedBook(next, updated)
    }

    fun search(catalog: List<ListedClose>, query: String, category: String? = null, limit: Int = 20): List<ListedClose> {
        val needle = query.trim().lowercase()
        if (needle.length < 2) return emptyList()
        return catalog.mapNotNull { row ->
            if (category != null && row.category != category) return@mapNotNull null
            val name = row.name.lowercase()
            val isin = row.isin.lowercase()
            val amfi = row.amfiCode.orEmpty().lowercase()
            val symbols = "${row.symbol.orEmpty()} ${row.nseSymbol.orEmpty()} ${row.bseSymbol.orEmpty()}".lowercase()
            if (!name.contains(needle) && !isin.contains(needle) && !amfi.contains(needle) && !symbols.contains(needle)) {
                return@mapNotNull null
            }
            var score = 0
            if (isin == needle) score += 1000
            else if (isin.startsWith(needle)) score += 600
            if (amfi == needle) score += 500
            if (name.startsWith(needle)) score += 400
            else if (name.contains(needle)) score += 200
            if (growth.containsMatchIn(name)) score += 40
            if (direct.containsMatchIn(name)) score += 25
            if (payout.containsMatchIn(name)) score -= 50
            row to score
        }.sortedWith(compareByDescending<Pair<ListedClose, Int>> { it.second }.thenBy { it.first.name })
            .take(limit)
            .map { it.first }
    }

    private fun reitLike(subCategory: String): Boolean =
        Regex("""\breit\b|\binvit\b""", RegexOption.IGNORE_CASE).containsMatchIn(subCategory)
}

data class CatalogRow(
    val isin: String,
    val name: String,
    val priceRupees: Double,
    val date: String,
    val source: String,
    val symbol: String? = null,
    val nse: String? = null,
    val bse: String? = null,
    val amfi: String? = null,
    val category: String = "EQUITY",
)

data class ListedClose(
    val isin: String,
    val name: String,
    val symbol: String?,
    val nseSymbol: String?,
    val bseSymbol: String?,
    val amfiCode: String?,
    val category: String,
    val close: MoneyScaled,
    val priceDate: String,
    val source: String,
)

data class HeldPrice(
    val id: String,
    val category: String,
    val quantityScaled: Long,
    val price: MoneyScaled,
    val subCategory: String = "",
    val isin: String? = null,
    val priceSource: String? = null,
    val priceDate: String? = null,
    val stale: Boolean = true,
)

data class PricedBook(val holdings: List<HeldPrice>, val updated: Int)

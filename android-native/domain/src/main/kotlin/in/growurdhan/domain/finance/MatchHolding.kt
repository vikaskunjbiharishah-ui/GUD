package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.OwnerId

/**
 * Match a statement row to a holding, from src/lib/matchHolding.ts.
 * A folio is the account, not the fund. Two schemes on one folio stay two holdings.
 */
object MatchHolding {
    private val droppedWords = Regex(
        """\b(fund|direct|regular|growth|plan|ltd|limited|equity|shares|idcw|dividend|class|series|option|institutional|flexi cap|large cap|small cap|mid cap)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val genericName = Regex(
        """^(mutual fund( units| folios?)?|mf (units|folios?)|cas|equity shares?|units|portfolio|holdings?)$""",
        RegexOption.IGNORE_CASE,
    )

    fun normalizeName(value: String): String =
        value.lowercase().replace(droppedWords, "").replace(Regex("[^a-z0-9]"), "")

    fun folioPrefix(folio: String?): String {
        if (folio.isNullOrBlank() || folio == "MF_FOLIO" || folio == "PORTFOLIO") return ""
        return folio.substringBefore('/').replace(Regex("[^A-Za-z0-9]"), "").uppercase()
    }

    fun isPlaceholderName(name: String?): Boolean {
        val trimmed = (name ?: "").trim()
        if (trimmed.isEmpty()) return true
        val compact = trimmed.replace(" ", "")
        if (Regex("""^\d{5,}(/\d+)?$""").matches(compact)) return true
        if (Regex("""^[A-Z0-9]{1,8}$""", RegexOption.IGNORE_CASE).matches(trimmed)) return true
        val body = schemeBody(trimmed)
        if (body.isEmpty()) return true
        if (genericName.matches(body)) return true
        return normalizeName(body).length < 4
    }

    fun preferSchemeName(current: String, incoming: String): String {
        val kept = current.trim()
        val next = incoming.trim()
        if (next.isEmpty()) return kept
        if (kept.isEmpty()) return next
        val currentPlaceholder = isPlaceholderName(kept)
        val incomingPlaceholder = isPlaceholderName(next)
        if (currentPlaceholder && !incomingPlaceholder) return next
        if (!currentPlaceholder && incomingPlaceholder) return kept
        val currentBody = schemeBody(kept)
        val incomingBody = schemeBody(next)
        if (incomingBody.length > currentBody.length + 10 && !incomingPlaceholder) return next
        return kept
    }

    fun find(existing: List<KnownHolding>, row: StatementRow, ownerId: OwnerId): KnownHolding? {
        val mine = existing.filter { it.ownerId == ownerId }
        val rowIsin = row.isin?.takeIf { it.length >= 10 }?.uppercase().orEmpty()
        if (rowIsin.isNotEmpty()) {
            mine.find { it.isin?.uppercase() == rowIsin && it.isin.length >= 10 }?.let { return it }
        }
        if (!row.amfiCode.isNullOrBlank()) {
            mine.find { it.amfiCode == row.amfiCode }?.let { return it }
        }
        val rowFolio = folioPrefix(row.folio)
        val rowName = normalizeName(row.holdingName)
        if (rowFolio.length >= 5 && rowName.length >= 4 && !isPlaceholderName(row.holdingName)) {
            mine.find { holding ->
                val holdingIsin = holding.isin?.takeIf { it.length >= 10 }?.uppercase().orEmpty()
                if (rowIsin.isNotEmpty() && holdingIsin.isNotEmpty()) return@find false
                val holdingFolio = folioPrefix(holding.folio) .ifEmpty { folioPrefix(holding.accountNumber) }
                holdingFolio == rowFolio && namesLikelySame(normalizeName(holding.name), rowName)
            }?.let { return it }
        }
        if (rowIsin.isNotEmpty()) return null
        if (rowName.length >= 4 && !isPlaceholderName(row.holdingName)) {
            mine.find { holding ->
                val identified = holding.isin != null && holding.isin.length >= 10
                !identified && namesLikelySame(normalizeName(holding.name), rowName)
            }?.let { return it }
        }
        if (rowFolio.length >= 5 && isPlaceholderName(row.holdingName)) {
            val stubs = mine.filter { holding ->
                val holdingFolio = folioPrefix(holding.folio).ifEmpty { folioPrefix(holding.accountNumber) }
                holdingFolio == rowFolio && (holding.isin == null || holding.isin.length < 10)
            }
            if (stubs.size == 1) return stubs[0]
        }
        return null
    }

    fun identityKey(holding: KnownHolding): String {
        val isin = holding.isin
        if (isin != null && isin.length >= 10) return "${holding.ownerId.raw}_ISIN_${isin.uppercase()}"
        val norm = normalizeName(holding.name)
        val folio = folioPrefix(holding.folio).ifEmpty { folioPrefix(holding.accountNumber) }
        val name = norm.ifEmpty { holding.name.lowercase() }
        return "${holding.ownerId.raw}_NAME_${name}_FOLIO_$folio"
    }

    private fun schemeBody(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return ""
        val hash = trimmed.indexOf('#')
        val afterHash = if (hash >= 0 && hash < trimmed.length - 3) trimmed.substring(hash + 1).trim() else trimmed
        val match = Regex("""^([A-Z0-9]{1,8})\s*-\s+(.+)$""", RegexOption.IGNORE_CASE).matchEntire(afterHash)
        return (match?.groupValues?.get(2) ?: afterHash).trim()
    }

    private fun namesLikelySame(left: String, right: String): Boolean {
        if (left.isEmpty() || right.isEmpty()) return false
        if (left == right) return true
        return left.length >= 8 && right.length >= 8 && (left.contains(right) || right.contains(left))
    }
}

data class KnownHolding(
    val id: String,
    val ownerId: OwnerId,
    val name: String,
    val isin: String? = null,
    val amfiCode: String? = null,
    val folio: String? = null,
    val accountNumber: String? = null,
)

data class StatementRow(
    val holdingName: String,
    val isin: String? = null,
    val folio: String? = null,
    val amfiCode: String? = null,
)

package `in`.growurdhan.domain.finance

import java.time.LocalDate

/**
 * Add-holding fields from src/lib/holdingForm.ts.
 * The fields follow the Indian instrument, not one generic share form.
 */
object HoldingForm {
    fun usesUnits(category: String, subCategory: String = "", details: InstrumentDetails? = null): Boolean {
        if (category == "EQUITY" || category == "MUTUAL_FUND" || category == "GOLD_SGB") return true
        val sub = subCategory.lowercase()
        if (category == "REAL_ESTATE") {
            if (!reitLike(subCategory)) return false
            return details?.altHoldMode != "LUMPSUM"
        }
        if (category == "SPECIALISED") {
            if (Regex("unlisted|esop").containsMatchIn(sub)) return true
            if (aifLike(subCategory)) return details?.altHoldMode == "DEMAT"
            return false
        }
        if (category == "FIXED_INCOME") return Regex("bond|goi").containsMatchIn(sub)
        return false
    }

    fun reitLike(subCategory: String?): Boolean =
        Regex("""\breit\b|\binvit\b""", RegexOption.IGNORE_CASE).containsMatchIn(subCategory.orEmpty())

    fun aifLike(subCategory: String?): Boolean =
        Regex("""\baif\b""", RegexOption.IGNORE_CASE).containsMatchIn(subCategory.orEmpty())

    fun nameLooksLikeReit(name: String?): Boolean =
        Regex("""\bREITs?\b|REAL ESTATE INVESTMENT TRUST|REAL ESTATE TRUST""", RegexOption.IGNORE_CASE).containsMatchIn(name.orEmpty())

    fun nameLooksLikeInvit(name: String?): Boolean =
        Regex("""\bINVITs?\b|INFRASTRUCTURE INVESTMENT TRUST""", RegexOption.IGNORE_CASE).containsMatchIn(name.orEmpty())

    fun nameLooksLikeAif(name: String?): Boolean =
        Regex("""\bAIF\b|ALTERNATIVE INVESTMENT|DISCOVERY FUND|CATEGORY III|\bCAT\s*III\b""", RegexOption.IGNORE_CASE)
            .containsMatchIn(name.orEmpty())

    fun nameLooksLikeEtf(name: String?): Boolean =
        Regex("""\bETF\b""", RegexOption.IGNORE_CASE).containsMatchIn(name.orEmpty())

    fun recategorize(name: String, category: String, schemeType: String? = null): String {
        val kind = schemeType.orEmpty().uppercase()
        if (looksLikeSgb(name)) return "GOLD_SGB"
        if (nameLooksLikeReit(name) || nameLooksLikeInvit(name) || kind == "REIT" || kind == "INVIT") return "REAL_ESTATE"
        if (kind == "AIF" || nameLooksLikeAif(name)) return "SPECIALISED"
        return category
    }

    fun subCategoryFor(category: String, name: String, schemeType: String? = null): String {
        val kind = schemeType.orEmpty().uppercase()
        if (category == "REAL_ESTATE") return if (nameLooksLikeInvit(name) || kind == "INVIT") "InvIT" else "REIT"
        if (category == "SPECIALISED" || kind == "AIF") return "AIF"
        if (category == "GOLD_SGB") return "Sovereign Gold Bond"
        if (category == "FIXED_INCOME") return if (Regex("nps", RegexOption.IGNORE_CASE).containsMatchIn(name) || kind == "NPS") "NPS" else "Other"
        if (category == "MUTUAL_FUND" && (nameLooksLikeEtf(name) || kind == "ETF")) return "Index / ETF"
        return "Other"
    }

    fun importDetails(category: String, casFileType: String? = null): InstrumentDetails? {
        if (category != "REAL_ESTATE" && category != "SPECIALISED") return null
        val depository = when (casFileType.orEmpty().uppercase()) {
            "NSDL" -> "NSDL"
            "CDSL" -> "CDSL"
            else -> null
        }
        return InstrumentDetails(altHoldMode = "DEMAT", depository = depository)
    }

    fun identifierLabel(category: String, subCategory: String = "", heldAs: String? = null, details: InstrumentDetails? = null): String {
        if (category == "EQUITY") return "NSE/BSE Symbol"
        if (category == "MUTUAL_FUND") {
            if (heldAs == "DEMAT") return if (indexOrEtf(subCategory)) "NSE/BSE Symbol" else "Folio (optional)"
            if (heldAs == "FOLIO") return "Folio Number"
            if (indexOrEtf(subCategory)) return "Symbol or folio"
            return "Folio Number"
        }
        if (category == "GOLD_SGB") {
            if (subCategory.startsWith("Physical")) return "Invoice / hallmark no."
            if (subCategory.startsWith("Digital")) return "Provider account (optional)"
            return "SGB series"
        }
        if (category == "FIXED_INCOME") {
            if (subCategory == "PPF") return "PPF account number"
            if (subCategory == "EPF") return "UAN (12 digits)"
            if (subCategory == "NPS") return "PRAN (12 digits)"
            if (subCategory.startsWith("Sukanya")) return "SSY account number"
            if (subCategory == "Bank FD") return "Deposit receipt number"
            if (subCategory.startsWith("Bond")) return "ISIN"
            return "Account number"
        }
        if (category == "REAL_ESTATE") {
            if (reitLike(subCategory)) return if (details?.altHoldMode == "LUMPSUM") "Agreement / folio (optional)" else "ISIN"
            return "Survey / door number (optional)"
        }
        if (category == "SPECIALISED") {
            if (subCategory == "PMS") return "PMS client code"
            if (aifLike(subCategory)) return if (details?.altHoldMode == "DEMAT") "ISIN" else "Scheme / folio"
            if (Regex("unlisted|esop", RegexOption.IGNORE_CASE).containsMatchIn(subCategory)) return "Share certificate / folio"
            return "Account number"
        }
        if (category == "CASH_BANK") return if (Regex("liquid", RegexOption.IGNORE_CASE).containsMatchIn(subCategory)) "Folio (if a liquid fund)" else "Account number"
        if (category == "LIABILITY") return "Loan account number"
        return "Account number"
    }

    fun namePlaceholder(category: String, subCategory: String = ""): String {
        if (category == "EQUITY") return "e.g. State Bank of India"
        if (category == "MUTUAL_FUND") {
            if (indexOrEtf(subCategory)) return "e.g. Nippon India ETF Nifty BeES"
            if (elss(subCategory)) return "e.g. Axis ELSS Tax Saver"
            return "e.g. Parag Parikh Flexi Cap"
        }
        if (category == "GOLD_SGB") {
            if (subCategory.startsWith("Physical")) return "e.g. 24K jewellery / coins"
            if (subCategory.startsWith("Digital")) return "e.g. SafeGold vault"
            return "e.g. SGB 2020-21 Series VI"
        }
        if (category == "FIXED_INCOME") {
            if (subCategory == "PPF") return "e.g. SBI PPF"
            if (subCategory == "EPF") return "e.g. EPFO — current employer"
            if (subCategory == "NPS") return "e.g. NPS Tier I"
            if (subCategory.startsWith("Sukanya")) return "e.g. SSY — daughter"
            if (subCategory == "Bank FD") return "e.g. HDFC 3-year FD"
            if (subCategory.startsWith("Bond")) return "e.g. 7.10% GS 2034"
            return "e.g. SBI PPF"
        }
        if (category == "REAL_ESTATE") {
            if (Regex("""\binvit\b""", RegexOption.IGNORE_CASE).containsMatchIn(subCategory)) return "e.g. IndiGrid InvIT"
            if (Regex("""\breit\b""", RegexOption.IGNORE_CASE).containsMatchIn(subCategory)) return "e.g. Embassy Office Parks REIT"
            return "e.g. 2BHK Flat Indiranagar"
        }
        if (category == "LIABILITY") return "e.g. HDFC Home Loan"
        if (category == "CASH_BANK") {
            return if (Regex("liquid", RegexOption.IGNORE_CASE).containsMatchIn(subCategory)) "e.g. HDFC Liquid — Direct Growth" else "e.g. HDFC Savings"
        }
        if (subCategory == "PMS") return "e.g. Ask PMS — Indian Entrepreneur"
        if (subCategory == "AIF") return "e.g. Blume Ventures Fund IV"
        if (Regex("esop", RegexOption.IGNORE_CASE).containsMatchIn(subCategory)) return "e.g. Company ESOPs"
        if (Regex("unlisted", RegexOption.IGNORE_CASE).containsMatchIn(subCategory)) return "e.g. Unlisted shares — ABC Pvt Ltd"
        return "e.g. PMS mandate"
    }

    fun acquiredOnLabel(category: String, subCategory: String = "", details: InstrumentDetails? = null): String {
        if (category == "EQUITY" || category == "MUTUAL_FUND") return "Trade date"
        if (category == "LIABILITY") return "Loan start date"
        if (category == "REAL_ESTATE") {
            if (reitLike(subCategory) && details?.altHoldMode != "LUMPSUM") return "Trade date"
            return "Purchase date"
        }
        if (category == "CASH_BANK" || category == "FIXED_INCOME") {
            return if (Regex("bond|nps|fd", RegexOption.IGNORE_CASE).containsMatchIn(subCategory)) "Bought on / opened on" else "Balance as of"
        }
        if (category == "SPECIALISED" && aifLike(subCategory) && details?.altHoldMode == "DEMAT") return "Trade date"
        if (category == "SPECIALISED" && Regex("pms|aif", RegexOption.IGNORE_CASE).containsMatchIn(subCategory)) return "Incept / first close"
        return "Bought on"
    }

    fun defaultSgbCoupon(): Double = 2.5

    fun hasStatementHistory(priceSource: String?, hasImportedEvent: Boolean): Boolean =
        Regex("CAS Statement|Tradebook", RegexOption.IGNORE_CASE).containsMatchIn(priceSource.orEmpty()) || hasImportedEvent

    fun suggestLockIn(category: String, subCategory: String, acquiredOn: String): LockIn {
        if (acquiredOn.isBlank()) return LockIn()
        val start = acquiredOn.take(10)
        fun add(months: Int): String {
            if (!Regex("""^\d{4}-\d{2}-\d{2}$""").matches(start)) return acquiredOn
            return EmiPlan.addMonths(LocalDate.parse(start), months).toString()
        }
        if (category == "GOLD_SGB" && subCategory.startsWith("Sovereign")) return LockIn(add(96), add(60))
        if (category == "FIXED_INCOME" && subCategory == "PPF") return LockIn(add(180))
        if (category == "FIXED_INCOME" && subCategory.startsWith("Sukanya")) return LockIn(add(252))
        if (category == "MUTUAL_FUND" && elss(subCategory)) return LockIn(add(36))
        return LockIn()
    }

    fun looksLikeUan(raw: String): Boolean = Regex("""^\d{12}$""").matches(raw.replace(" ", ""))
    fun looksLikePran(raw: String): Boolean = Regex("""^\d{12}$""").matches(raw.replace(" ", ""))
    fun looksLikeIsin(raw: String): Boolean = Regex("""^[A-Z]{2}[A-Z0-9]{9}[0-9]$""", RegexOption.IGNORE_CASE).matches(raw.trim())
    fun looksLikeNsdlDpId(raw: String): Boolean = Regex("""^IN\d{6}$""", RegexOption.IGNORE_CASE).matches(raw.replace(" ", ""))
    fun looksLikeNsdlClientId(raw: String): Boolean = Regex("""^\d{8}$""").matches(raw.replace(" ", ""))
    fun looksLikeBoid(raw: String): Boolean = Regex("""^\d{16}$""").matches(raw.replace(" ", ""))

    fun inferPlan(name: String?): String? {
        val text = name.orEmpty().uppercase()
        if (Regex("""\bDIRECT\b""").containsMatchIn(text)) return "DIRECT"
        if (Regex("""\bREGULAR\b""").containsMatchIn(text)) return "REGULAR"
        return null
    }

    fun inferOption(name: String?): String? {
        val text = name.orEmpty().uppercase()
        if (Regex("""\bIDCW\b""").containsMatchIn(text) || Regex("""\bDIVIDEND\b""").containsMatchIn(text)) return "IDCW"
        if (Regex("""\bGROWTH\b""").containsMatchIn(text)) return "GROWTH"
        return null
    }

    fun dematMask(details: InstrumentDetails): String? {
        val bo = details.boId.orEmpty().replace(" ", "")
        if (bo.isNotEmpty()) return bo
        val dp = details.dpId.orEmpty().replace(" ", "").uppercase()
        val client = details.clientId.orEmpty().replace(" ", "")
        if (dp.isNotEmpty() && client.isNotEmpty()) return "$dp $client"
        return null
    }

    fun detailLines(acquiredOn: String?, details: InstrumentDetails): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        if (!acquiredOn.isNullOrBlank()) out += "From" to acquiredOn
        details.altHoldMode?.let { out += "Held as" to if (it == "LUMPSUM") "Direct lumpsum" else "Demat (CAS)" }
        details.mfPlan?.let { out += "Plan" to if (it == "DIRECT") "Direct" else "Regular" }
        details.mfOption?.let { out += "Option" to if (it == "IDCW") "IDCW" else "Growth" }
        details.mfHeldAs?.let { out += "Held as" to if (it == "DEMAT") "Demat" else "SOA folio" }
        details.dpId?.let { out += "DP ID" to it.uppercase() }
        details.clientId?.let { out += "Client ID" to it }
        details.boId?.let { out += "BOID" to it }
        details.goldHoldMode?.let { out += "Held as" to if (it == "DEMAT") "Demat" else "RBI / bank certificate" }
        details.goldPurity?.let { out += "Purity" to it }
        details.goldHallmark?.let { out += "Hallmark / invoice" to it }
        details.earlyExitDate?.let { out += "First exit (after year 5)" to it }
        rupees(details.annualContributionMinor)?.let { out += "Yearly contribution" to it }
        details.contributionOn?.let { out += "Contribution date" to it }
        details.compounding?.let { mode ->
            val pay = when (mode) {
                "CUMULATIVE" -> "Cumulative, compounded quarterly"
                "HALF_YEARLY" -> "Half-yearly payout"
                "QUARTERLY" -> "Quarterly payout"
                "MONTHLY" -> "Monthly payout"
                else -> "Yearly payout"
            }
            out += "Interest" to pay
        }
        details.npsTier?.let { out += "NPS tier" to "Tier $it" }
        details.npsCra?.let { out += "CRA" to if (it == "PROT") "Protean" else it }
        details.npsPfm?.let { out += "Pension fund" to it }
        if (details.tdsApplicable == true) out += "TDS" to "Yes (Form 16A / 26AS)"
        rupees(details.faceValueMinor)?.let { out += "Face value" to it }
        details.areaSqFt?.let { out += "Area" to "$it sq ft" }
        details.ownership?.let { who ->
            val share = details.ownershipPercent?.let { " ($it%)" }.orEmpty()
            val label = when (who) {
                "JOINT" -> "Joint"
                "HUF" -> "HUF"
                "SPOUSE" -> "Spouse"
                else -> "Self"
            }
            out += "Ownership" to label + share
        }
        details.aifCategory?.let { out += "AIF category" to "Category $it" }
        rupees(details.commitmentMinor)?.let { out += "Commitment" to it }
        rupees(details.capitalCalledMinor)?.let { out += "Capital called" to it }
        details.strategyName?.let { out += "Strategy" to it }
        details.custodian?.let { out += "Custodian" to it }
        details.ifsc?.let { out += "IFSC" to it.uppercase() }
        details.rateType?.let { out += "Rate" to if (it == "FLOATING") "Floating" else "Fixed" }
        details.originalTenorMonths?.let { out += "Original tenor" to "$it months" }
        return out
    }

    fun isEmpty(details: InstrumentDetails?): Boolean {
        if (details == null) return true
        return details == InstrumentDetails()
    }

    private fun looksLikeSgb(name: String?): Boolean {
        val text = name.orEmpty().uppercase()
        return Regex("""\bSGB\b""").containsMatchIn(text) || "SOVEREIGN GOLD" in text
    }

    private fun elss(subCategory: String): Boolean = Regex("elss", RegexOption.IGNORE_CASE).containsMatchIn(subCategory)
    private fun indexOrEtf(subCategory: String): Boolean = Regex("index|etf", RegexOption.IGNORE_CASE).containsMatchIn(subCategory)

    private fun rupees(minor: String?): String? {
        if (minor.isNullOrBlank()) return null
        val amount = minor.toDoubleOrNull() ?: return ""
        if (!amount.isFinite()) return ""
        val whole = java.lang.Math.round(amount / 10_000.0)
        val digits = kotlin.math.abs(whole).toString()
        val grouped = if (digits.length <= 3) digits else {
            val head = digits.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
            "$head,${digits.takeLast(3)}"
        }
        return "${if (whole < 0) "-" else ""}₹ $grouped"
    }
}

data class InstrumentDetails(
    val altHoldMode: String? = null,
    val depository: String? = null,
    val mfPlan: String? = null,
    val mfOption: String? = null,
    val mfHeldAs: String? = null,
    val dpId: String? = null,
    val clientId: String? = null,
    val boId: String? = null,
    val goldHoldMode: String? = null,
    val goldPurity: String? = null,
    val goldHallmark: String? = null,
    val earlyExitDate: String? = null,
    val annualContributionMinor: String? = null,
    val contributionOn: String? = null,
    val compounding: String? = null,
    val npsTier: String? = null,
    val npsCra: String? = null,
    val npsPfm: String? = null,
    val tdsApplicable: Boolean? = null,
    val faceValueMinor: String? = null,
    val areaSqFt: String? = null,
    val ownership: String? = null,
    val ownershipPercent: Double? = null,
    val aifCategory: String? = null,
    val commitmentMinor: String? = null,
    val capitalCalledMinor: String? = null,
    val strategyName: String? = null,
    val custodian: String? = null,
    val ifsc: String? = null,
    val rateType: String? = null,
    val originalTenorMonths: Int? = null,
)

data class LockIn(val maturity: String? = null, val earlyExit: String? = null)

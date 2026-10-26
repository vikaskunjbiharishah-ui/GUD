package `in`.growurdhan.domain.finance

/**
 * Two-axis placement from src/lib/taxonomy.ts.
 * Exposure is what the money is in. Wrapper is how it is held.
 * A name is only a fallback. A choice saved on the holding wins.
 */
object Taxonomy {
    val exposures = listOf(
        Exposure("EQUITY", "Equity", listOf(
            Level("LARGE_CAP", "Large cap"), Level("MID_CAP", "Mid cap"), Level("SMALL_CAP", "Small cap"),
            Level("MULTI_FLEXI", "Multi & flexi cap"), Level("STYLE", "Style"),
            Level("SECTOR", "Sector & thematic"), Level("INTERNATIONAL", "International"),
            Level("ELSS", "Tax-saving (ELSS)"),
        )),
        Exposure("DEBT", "Debt & fixed income", listOf(
            Level("GOVERNMENT", "Government"), Level("CORPORATE", "Corporate"),
            Level("DEPOSITS", "Bank & company deposits"), Level("SMALL_SAVINGS", "Small savings & provident"),
            Level("MONEY_MARKET", "Money market & liquid"), Level("DURATION", "Duration & dynamic"),
        )),
        Exposure("GOLD", "Gold & commodities", listOf(
            Level("GOLD", "Gold"), Level("SILVER", "Silver"), Level("OTHER_COMMODITY", "Other commodities"),
        )),
        Exposure("PROPERTY", "Property", listOf(
            Level("RESIDENTIAL", "Residential"), Level("COMMERCIAL", "Commercial"), Level("LAND", "Land"),
            Level("REIT", "Listed property (REIT)"), Level("INVIT", "Infrastructure (InvIT)"),
        )),
        Exposure("CASH", "Cash & liquid", listOf(
            Level("BANK", "Bank balances"), Level("LIQUID", "Liquid & overnight"), Level("ARBITRAGE", "Arbitrage"),
        )),
        Exposure("PRIVATE", "Private & unlisted", listOf(
            Level("UNLISTED", "Unlisted equity"), Level("ESOP", "Employee shares"),
            Level("PRIVATE_CREDIT", "Private credit"), Level("DIGITAL", "Digital assets"),
        )),
        Exposure("PROTECTION", "Protection", listOf(
            Level("PURE", "Pure protection"), Level("SAVINGS_LINKED", "Savings-linked policies"),
            Level("ANNUITY", "Annuity"),
        )),
        Exposure("LIABILITY", "What you owe", listOf(
            Level("HOME_LOAN", "Home loans"), Level("AGAINST_ASSETS", "Loans against assets"),
            Level("PERSONAL", "Personal & other"), Level("CARDS", "Cards"),
        )),
    )
    val wrappers = listOf(
        Wrapper("DIRECT", "Direct"), Wrapper("MF", "Mutual fund"), Wrapper("INDEX_ETF", "Index & ETF"),
        Wrapper("SIF", "SIF"), Wrapper("AIF", "AIF"), Wrapper("PMS", "PMS"),
        Wrapper("DEPOSIT", "Deposit"), Wrapper("SMALL_SAVINGS", "Small savings"),
        Wrapper("RETIREMENT", "Retirement"), Wrapper("INSURANCE", "Insurance"), Wrapper("PHYSICAL", "Physical"),
    )

    fun exposureLabel(id: String): String = exposures.find { it.id == id }?.label ?: id
    fun levelLabel(exposure: String, level: String): String =
        exposures.find { it.id == exposure }?.levels?.find { it.id == level }?.label ?: level
    fun wrapperLabel(id: String): String = wrappers.find { it.id == id }?.label ?: id

    fun classify(
        holding: FinancialHolding,
        folio: String = "",
        explicit: ExplicitPlacement? = null,
    ): Placement {
        if (explicit != null) {
            return Placement(explicit.exposure, explicit.level, explicit.wrapper, inferred = false)
        }
        val text = "${holding.name} ${holding.subCategory} $folio".lowercase()
        val has: (Array<out String>) -> Boolean = { words -> words.any { text.contains(it) } }
        val exposure = inferExposure(holding.category, has)
        return Placement(exposure.first, exposure.second, inferWrapper(holding.category, has), inferred = true)
    }

    private fun inferWrapper(category: String, has: (Array<out String>) -> Boolean): String {
        if (has(arrayOf("sif ", "specialised investment", "specialized investment"))) return "SIF"
        if (has(arrayOf(" aif", "alternative investment", "category iii", "cat iii"))) return "AIF"
        if (has(arrayOf("pms", "portfolio management"))) return "PMS"
        if (has(arrayOf("ulip", "endowment", "money back", "money-back", "term plan", "term insurance", "health insurance", "annuity", "policy"))) return "INSURANCE"
        if (has(arrayOf("nps", "pension", "provident", "epf", "vpf", "gratuity", "superannuation"))) return "RETIREMENT"
        if (has(arrayOf("ppf", "sukanya", "nsc", "kvp", "kisan vikas", "scss", "senior citizen", "post office", "mahila samman", "small savings"))) return "SMALL_SAVINGS"
        if (has(arrayOf("etf", "index fund", "nifty", "sensex", "bees"))) return "INDEX_ETF"
        if (has(arrayOf("fixed deposit", "term deposit", "recurring deposit", " fd ", "corporate deposit"))) return "DEPOSIT"
        if (has(arrayOf("physical", "bullion", "jewell", "coin", "apartment", "plot", "land", "villa", "house", "flat"))) return "PHYSICAL"
        if (category == "MUTUAL_FUND" || has(arrayOf("fund", "scheme"))) return "MF"
        return "DIRECT"
    }

    private fun inferExposure(category: String, has: (Array<out String>) -> Boolean): Pair<String, String> {
        if (category == "LIABILITY") {
            if (has(arrayOf("home", "housing", "mortgage"))) return "LIABILITY" to "HOME_LOAN"
            if (has(arrayOf("against", "las", "gold loan", "margin"))) return "LIABILITY" to "AGAINST_ASSETS"
            if (has(arrayOf("card"))) return "LIABILITY" to "CARDS"
            return "LIABILITY" to "PERSONAL"
        }
        if (category == "GOLD_SGB" || has(arrayOf("gold", "sovereign gold", "sgb"))) {
            if (has(arrayOf("silver"))) return "GOLD" to "SILVER"
            return "GOLD" to "GOLD"
        }
        if (has(arrayOf("silver"))) return "GOLD" to "SILVER"
        if (category == "REAL_ESTATE" || has(arrayOf("apartment", "residential", "commercial", "plot", "land", "reit", "invit", "property"))) {
            if (has(arrayOf("invit", "infrastructure trust"))) return "PROPERTY" to "INVIT"
            if (has(arrayOf("reit"))) return "PROPERTY" to "REIT"
            if (has(arrayOf("commercial", "office", "shop"))) return "PROPERTY" to "COMMERCIAL"
            if (has(arrayOf("plot", "land", "agricultur"))) return "PROPERTY" to "LAND"
            return "PROPERTY" to "RESIDENTIAL"
        }
        if (has(arrayOf("ulip", "endowment", "money back", "money-back", "annuity", "term plan", "term insurance", "health insurance"))) {
            if (has(arrayOf("annuity"))) return "PROTECTION" to "ANNUITY"
            if (has(arrayOf("term plan", "term insurance", "health insurance"))) return "PROTECTION" to "PURE"
            return "PROTECTION" to "SAVINGS_LINKED"
        }
        if (has(arrayOf("unlisted", "pre-ipo", "pre ipo", "private placement"))) return "PRIVATE" to "UNLISTED"
        if (has(arrayOf("esop", "rsu", "employee stock"))) return "PRIVATE" to "ESOP"
        if (has(arrayOf("p2p", "peer to peer", "private credit", "invoice discount"))) return "PRIVATE" to "PRIVATE_CREDIT"
        if (has(arrayOf("crypto", "bitcoin", "ethereum", "vda", "digital asset"))) return "PRIVATE" to "DIGITAL"
        if (category == "CASH_BANK") {
            if (has(arrayOf("liquid", "overnight"))) return "CASH" to "LIQUID"
            if (has(arrayOf("arbitrage"))) return "CASH" to "ARBITRAGE"
            return "CASH" to "BANK"
        }
        if (has(arrayOf("arbitrage"))) return "CASH" to "ARBITRAGE"
        if (has(arrayOf("liquid", "overnight"))) return "CASH" to "LIQUID"
        if (has(arrayOf("gilt", "g-sec", "gsec", "government security", "sdl", "treasury bill", "t-bill", "state development"))) return "DEBT" to "GOVERNMENT"
        if (has(arrayOf("ppf", "sukanya", "nsc", "kvp", "kisan vikas", "scss", "senior citizen", "post office", "mahila samman", "provident", "epf", "vpf", "small savings"))) return "DEBT" to "SMALL_SAVINGS"
        if (has(arrayOf("nps", "pension"))) return "DEBT" to "SMALL_SAVINGS"
        if (has(arrayOf("fixed deposit", "term deposit", "recurring deposit", "corporate deposit"))) return "DEBT" to "DEPOSITS"
        if (has(arrayOf("ncd", "debenture", "corporate bond", "credit risk", "banking and psu"))) return "DEBT" to "CORPORATE"
        if (has(arrayOf("duration", "dynamic bond", "bond fund", "income fund"))) return "DEBT" to "DURATION"
        if (has(arrayOf("money market", "ultra short", "low duration", "short duration"))) return "DEBT" to "MONEY_MARKET"
        if (category == "FIXED_INCOME") return "DEBT" to "DEPOSITS"
        if (has(arrayOf(" debt"))) return "DEBT" to "DURATION"
        if (has(arrayOf("elss", "tax saver", "tax saving"))) return "EQUITY" to "ELSS"
        if (has(arrayOf("international", "us equity", "nasdaq", "s&p 500", "global", "overseas"))) return "EQUITY" to "INTERNATIONAL"
        if (has(arrayOf(
                "flexi cap", "multi cap", "multicap", "flexicap", "large & mid", "large and mid",
                "balanced advantage", "dynamic asset allocation", "aggressive hybrid", "conservative hybrid",
                "balanced hybrid", "multi asset", "multi-asset", "equity savings", "hybrid",
            ))) return "EQUITY" to "MULTI_FLEXI"
        if (has(arrayOf("small cap", "smallcap"))) return "EQUITY" to "SMALL_CAP"
        if (has(arrayOf("mid cap", "midcap"))) return "EQUITY" to "MID_CAP"
        if (has(arrayOf("value", "contra", "focused", "dividend yield"))) return "EQUITY" to "STYLE"
        if (has(arrayOf("sector", "thematic", "pharma", "banking", "infra", "technology", "capital goods", " it ", "psu", "consumption", "energy"))) return "EQUITY" to "SECTOR"
        if (has(arrayOf("large cap", "largecap", "bluechip", "top 100", "nifty 50", "sensex"))) return "EQUITY" to "LARGE_CAP"
        return "EQUITY" to "LARGE_CAP"
    }
}

data class Exposure(val id: String, val label: String, val levels: List<Level>)
data class Level(val id: String, val label: String)
data class Wrapper(val id: String, val label: String)
data class ExplicitPlacement(val exposure: String, val level: String, val wrapper: String)
data class Placement(val exposure: String, val level: String, val wrapper: String, val inferred: Boolean) {
    val code: String get() = "$exposure/$level/$wrapper"
}

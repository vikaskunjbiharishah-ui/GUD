package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.MoneyScaled

enum class SgbPriceBasis { RBI, MARKET }

/**
 * Certificate SGB can be valued at the RBI issue price or a market close.
 * A missing side stays missing. It is not replaced with the other side or with zero.
 */
object SgbPrice {
    const val RBI_MISSING = "No RBI price is saved. It is not zero."
    const val MARKET_MISSING = "No market close is saved. It is not zero."

    fun choose(basis: SgbPriceBasis, rbi: MoneyScaled?, market: MoneyScaled?): MoneyScaled? {
        val picked = when (basis) {
            SgbPriceBasis.RBI -> rbi
            SgbPriceBasis.MARKET -> market
        }
        if (picked == null || picked.raw <= 0L) return null
        return picked
    }

    fun label(basis: SgbPriceBasis, amount: MoneyScaled?): String = when {
        amount == null && basis == SgbPriceBasis.RBI -> RBI_MISSING
        amount == null -> MARKET_MISSING
        basis == SgbPriceBasis.RBI -> "Using the RBI price."
        else -> "Using the market close."
    }
}

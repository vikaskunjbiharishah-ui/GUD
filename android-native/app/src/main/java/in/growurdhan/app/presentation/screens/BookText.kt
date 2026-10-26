package `in`.growurdhan.app.presentation.screens

import `in`.growurdhan.core.model.LedgerKind
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.domain.finance.FinancialEventKind
import `in`.growurdhan.domain.finance.HoldingView
import `in`.growurdhan.domain.finance.MoneyEngine

/** Screen text for a stored rupee amount. Grouping comes from the domain, not the screen. */
internal fun MoneyScaled.asBook(): String = MoneyEngine.formatRupees(raw)

/** Screen text for a stored quantity. The raw value is scaled by 100,000,000. */
internal fun bookQuantity(scaled: Long): String = MoneyEngine.formatQty(scaled)

/** The same event words the family book uses. A legacy adjustment is a statement update. */
internal fun LedgerKind.asBookLabel(): String {
    val kind = if (this == LedgerKind.ADJUSTMENT) FinancialEventKind.SNAPSHOT_ADJUSTMENT
        else FinancialEventKind.valueOf(name)
    return HoldingView.eventLabel(kind)
}

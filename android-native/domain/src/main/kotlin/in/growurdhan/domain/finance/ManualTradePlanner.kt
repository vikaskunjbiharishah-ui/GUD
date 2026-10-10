package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.VaultBook
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/** Explicitly entered trades only; statement snapshots never use this path. */
enum class ManualTradeKind { BUY, SELL }

data class ManualTradeDraft(
    val holdingId: HoldingId,
    val kind: ManualTradeKind,
    val tradeDate: String,
    val quantity: String,
    val unitPriceInr: String,
    val chargesInr: String = "0",
)

data class ManualTradePlan(
    val event: FinancialEvent,
    val holdingName: String,
    val ownerName: String,
    val quantity: Long,
    val gross: MoneyScaled,
    val charges: MoneyScaled,
    /** Cash outflow for BUY; cash received for SELL. */
    val cashAmount: MoneyScaled,
    /** Tamper-resistant digest of the exact authorized book being reviewed. */
    val bookFingerprint: String,
)

/**
 * Validates a real, user-selected enrolled holding in an atomic read snapshot.
 * This is a financial draft, not permission to commit. Device authentication,
 * original-credential revalidation and SQLCipher transaction guards are separate.
 */
object ManualTradePlanner {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        .withResolverStyle(ResolverStyle.STRICT)
    private val quantityPattern = Regex("[0-9]{1,12}(\\.[0-9]{1,8})?")
    private val moneyPattern = Regex("[0-9]{1,12}(\\.[0-9]{1,4})?")

    private fun amount(value: String, scale: Int, pattern: Regex): Long {
        require(pattern.matches(value)) { "Enter a positive decimal with supported precision." }
        return BigDecimal(value).movePointRight(scale).longValueExact()
    }

    /**
     * Stable snapshot digest without storing another copy of family rows in
     * Compose. Encodes each field with length prefix to avoid ambiguity.
     * Comparisons happen again INSIDE SQLCipher's write transaction.
     */
    private fun fingerprint(book: VaultBook): String {
        val digest = MessageDigest.getInstance("SHA-256")
        fun part(text: String?) {
            val bytes = text?.toByteArray(Charsets.UTF_8)
            digest.update(ByteBuffer.allocate(4).putInt(bytes?.size ?: -1).array())
            if (bytes != null) digest.update(bytes)
        }
        book.members.sortedBy { it.id.raw }.forEach { member ->
            part("member"); part(member.id.raw); part(member.displayName)
            part(member.relationship); part(member.isPrimary.toString())
        }
        book.accounts.sortedBy { it.id }.forEach { account ->
            part("account"); part(account.id); part(account.ownerId.raw)
            part(account.accountType); part(account.identifierMasked)
        }
        book.holdings.sortedBy { it.id.raw }.forEach { holding ->
            part("holding"); part(holding.id.raw); part(holding.ownerId.raw)
            part(holding.accountId); part(holding.name); part(holding.assetCategory)
            part(holding.quantity.raw.toString()); part(holding.currentUnitPrice.raw.toString())
        }
        book.casIdentities.sortedBy { it.holdingId.raw }.forEach { entry ->
            part("identity"); part(entry.holdingId.raw); part(entry.ownerId.raw)
            part(entry.accountId); part(entry.isin); part(entry.folio)
            part(entry.accountType)
        }
        book.events.sortedBy { it.id }.forEach { event ->
            part("event"); part(event.id); part(event.holdingId.raw)
            part(event.ownerId.raw); part(event.kind.name); part(event.eventDate)
            part(event.quantityDeltaScaled.toString()); part(event.amount.raw.toString())
            part(event.sourceFingerprint); part(event.unitPrice?.raw?.toString())
            part(event.charges.raw.toString()); part(event.supersedesEventId)
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    fun prepare(
        book: VaultBook, draft: ManualTradeDraft, eventId: String,
        today: LocalDate = LocalDate.now(),
    ): ManualTradePlan {
        require(book.members.isNotEmpty()) { "Enrol a family member before recording a trade." }
        val holding = book.holdings.singleOrNull { it.id == draft.holdingId }
            ?: throw IllegalArgumentException("Select an enrolled holding.")
        val owner = book.members.singleOrNull { it.id == holding.ownerId }
            ?: throw IllegalArgumentException("Holding owner is no longer enrolled.")
        require(holding.assetCategory != "LIABILITY") {
            "Buy and sell transactions cannot be entered for liabilities."
        }
        require(eventId.isNotBlank() && eventId.length <= 128)
        require(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(draft.tradeDate))
        val day = LocalDate.parse(draft.tradeDate, dateFormatter)
        require(day >= LocalDate.of(1900, 1, 1) && !day.isAfter(today)) {
            "Trade date must be valid and cannot be in the future."
        }
        val quantity = amount(draft.quantity, 8, quantityPattern)
        val price = amount(draft.unitPriceInr, 4, moneyPattern)
        val fees = amount(draft.chargesInr, 4, moneyPattern)
        require(quantity > 0 && price > 0) { "Enter positive units and a positive unit price." }

        // Exactly round the product from INR[4] * units[8] into INR[4].
        // Intermediate multiplication uses BigInteger, not overflowing Long.
        val scale = BigInteger.valueOf(100_000_000L)
        val product = BigInteger.valueOf(quantity).multiply(BigInteger.valueOf(price))
        val (whole, rem) = product.divideAndRemainder(scale)
        val gross = whole.add(if (rem.shiftLeft(1) >= scale) BigInteger.ONE else BigInteger.ZERO)
            .longValueExact()
        require(gross > 0) { "Trade is too small to represent at the supported money precision." }
        if (draft.kind == ManualTradeKind.SELL) {
            require(fees < gross) { "Charges cannot consume all sale proceeds." }
            val historicalEvents = book.events.filter { it.holdingId == holding.id }.map {
                it.toFinancialEvent()
            }
            // A positive CAS snapshot adjustment is only a registrar observation,
            // not evidence of an acquisition or the purchase cost of those units.
            require(historicalEvents.none {
                it.kind == FinancialEventKind.SNAPSHOT_ADJUSTMENT && it.quantityScaled > 0
            }) { "Positive statement adjustments require verified acquisition review." }
            require(historicalEvents.none {
                it.kind in setOf(FinancialEventKind.BUY, FinancialEventKind.OPENING_BALANCE) &&
                    it.unitPrice == null
            }) { "Legacy unpriced acquisitions cannot establish verified sale cost." }
            val original = LotEngine.fold(holding.id, holding.assetCategory, historicalEvents)
            require(original.errors.isEmpty()) {
                "Historical lots need review before a disposal can be recorded."
            }
            require(original.quantityScaled >= quantity) {
                "Sale exceeds verified ledger units, or acquisition lots are missing."
            }
            val proposed = FinancialEvent(
                id = eventId, holdingId = holding.id, ownerId = owner.id,
                kind = FinancialEventKind.SELL, date = day,
                quantityScaled = -quantity,
                unitPrice = MoneyScaled(price), totalAmount = MoneyScaled(gross),
                charges = MoneyScaled(fees),
            )
            val after = LotEngine.fold(
                holding.id, holding.assetCategory, historicalEvents + proposed,
            )
            require(after.errors.isEmpty()) {
                "A backdated sale would make the historical lot sequence invalid."
            }
            return ManualTradePlan(
                proposed, holding.name, owner.displayName, quantity,
                MoneyScaled(gross), MoneyScaled(fees),
                MoneyScaled(Math.subtractExact(gross, fees)), fingerprint(book),
            )
        }

        val event = FinancialEvent(
            id = eventId, holdingId = holding.id, ownerId = owner.id,
            kind = FinancialEventKind.BUY, date = day,
            quantityScaled = quantity, unitPrice = MoneyScaled(price),
            totalAmount = MoneyScaled(gross), charges = MoneyScaled(fees),
        )
        return ManualTradePlan(
            event, holding.name, owner.displayName, quantity,
            MoneyScaled(gross), MoneyScaled(fees),
            MoneyScaled(Math.addExact(gross, fees)), fingerprint(book),
        )
    }
}

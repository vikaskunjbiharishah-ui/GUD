package `in`.growurdhan.domain.finance

import `in`.growurdhan.core.model.HoldingId
import `in`.growurdhan.core.model.MoneyScaled
import `in`.growurdhan.core.model.OwnerId
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/**
 * Home checks from src/lib/homeDashboard.ts.
 * A statement line stays flat until the next file. Cost that is still the
 * market price is reported, not treated as a real gain.
 */
object HomeBooks {
    private const val STALE_AMBER_DAYS = 45
    private const val STALE_RED_DAYS = 90
    private const val PRICE_LAG_AMBER_DAYS = 7
    private val issuerLabels = mapOf(
        "CDSL" to "CDSL (Demat + Mutual Funds)",
        "NSDL" to "NSDL (Demat + Mutual Funds)",
        "CAMS" to "CAMS Mutual Funds",
        "KFINTECH" to "KFintech Mutual Funds",
        "NPS" to "NPS (Protean / CRA statement)",
    )

    fun daysBetween(from: String, to: String): Long? {
        val start = parseDay(from) ?: return null
        val end = parseDay(to) ?: return null
        return ChronoUnit.DAYS.between(start, end)
    }

    fun previousFinancialYear(fy: String): String {
        val start = fy.substringBefore('-').toIntOrNull() ?: return ""
        if (start < 1990) return ""
        return "${start - 1}-${(start % 100).toString().padStart(2, '0')}"
    }

    fun financialYearEnd(fy: String): LocalDate? {
        val start = fy.substringBefore('-').toIntOrNull() ?: return null
        return LocalDate.of(start + 1, 3, 31)
    }

    fun freshness(
        statementAsOf: LocalDate?,
        pricesAsOf: LocalDate?,
        today: LocalDate,
        staleAfterDays: Int = STALE_AMBER_DAYS,
        priceLagDays: Int = PRICE_LAG_AMBER_DAYS,
    ): HealthTone {
        if (statementAsOf != null) {
            val age = ChronoUnit.DAYS.between(statementAsOf, today)
            if (age >= STALE_RED_DAYS) return HealthTone.RED
            if (age >= staleAfterDays) return HealthTone.AMBER
        }
        if (statementAsOf != null && pricesAsOf != null) {
            val lag = kotlin.math.abs(ChronoUnit.DAYS.between(statementAsOf, pricesAsOf))
            if (lag >= priceLagDays) return HealthTone.AMBER
        }
        return HealthTone.OK
    }

    fun concentration(rows: List<ValuedHolding>, assets: MoneyScaled, topN: Int = 5): Concentration {
        val open = rows.filter { it.value.raw > 0L }.sortedByDescending { it.value.raw }
        val top = open.take(topN)
        val share = top.map {
            ConcentrationRow(it.id, it.name, it.value, percent(it.value.raw, assets.raw))
        }
        val topSum = top.fold(0L) { sum, row -> Math.addExact(sum, row.value.raw) }
        return Concentration(share, if (assets.raw > 0L) percent(topSum, assets.raw) else 0.0)
    }

    fun booksHealth(input: BooksHealthInput): BooksHealth {
        val inScope = { owner: OwnerId -> input.selectedMemberId == null || owner == input.selectedMemberId }
        val holdings = input.holdings.filter { inScope(it.ownerId) }
        val events = input.events.filter { inScope(it.ownerId) }
        val findings = mutableListOf<HealthFinding>()

        val unknown = holdings.filter { holding ->
            holding.category != "LIABILITY" && openQuantity(holding) > 0L &&
                CostBasis.isUnknownInvested(
                    holding, events, investedOf(holding), marketValueOf(holding),
                )
        }
        if (unknown.isNotEmpty()) {
            val names = unknown.take(2).joinToString(", ") { displayName(it.name) }
            findings += HealthFinding(
                HealthKind.COST_EQUALS_VALUE, HealthTone.AMBER,
                if (unknown.size == 1) "Invested still matches current value"
                else "${unknown.size} holdings still use statement value as invested",
                if (unknown.size == 1) "$names has no separate purchase cost. CAS does not print that."
                else "$names${if (unknown.size > 2) " and others" else ""}. Edit invested on the holding if you know what you paid.",
                unknown.size, HealthGo.HOLDINGS,
            )
        }

        val owners = if (input.selectedMemberId == null) input.statements.map { it.ownerId }.distinct()
            else listOf(input.selectedMemberId)
        var worst: StatementStamp? = null
        var worstDays = -1L
        for (owner in owners) {
            for (stamp in input.statements.filter { it.ownerId == owner }) {
                val days = ChronoUnit.DAYS.between(stamp.periodTo, input.today)
                if (days < STALE_AMBER_DAYS) continue
                if (days > worstDays) {
                    worst = stamp
                    worstDays = days
                }
            }
        }
        if (worst != null) {
            val label = issuerLabels[worst.fileType] ?: worst.fileType
            findings += HealthFinding(
                HealthKind.STALE_STATEMENT,
                if (worstDays >= STALE_RED_DAYS) HealthTone.RED else HealthTone.AMBER,
                "${label.substringBefore(' ')} statement is $worstDays days old",
                "Last confirmed through ${worst.periodTo}. Read a newer CAS or NPS file to refresh.",
                null, HealthGo.DOCUMENTS,
            )
        }

        val currentFy = LotEngine.fiscalYear(input.today)
        val previousFy = previousFinancialYear(currentFy)
        val previousEnd = financialYearEnd(previousFy)
        if (previousFy.isNotEmpty() && previousEnd != null) {
            val missing = holdings.mapNotNull { holding ->
                val kind = SavingsInterestEngine.interestKindOf(holding)
                if (kind == null) return@mapNotNull null
                if (openQuantity(holding) <= 0L) return@mapNotNull null
                if (!existedBy(holding, events, previousEnd)) return@mapNotNull null
                val posted = events.any {
                    it.holdingId == holding.holdingId && it.kind == FinancialEventKind.INTEREST &&
                        LotEngine.fiscalYear(it.date) == previousFy
                }
                if (posted) null else displayName(holding.name)
            }
            if (missing.isNotEmpty()) {
                findings += HealthFinding(
                    HealthKind.MISSING_FY_INTEREST, HealthTone.AMBER,
                    if (missing.size == 1) "No FY $previousFy interest on ${missing[0]}"
                    else "${missing.size} deposits have no FY $previousFy interest",
                    "Open the holding and confirm the interest. Payout schemes post on their own dates; PPF, SSY and EPF post on 31 March.",
                    missing.size, HealthGo.HOLDINGS,
                )
            }
        }

        val panGaps = input.members.filter { member ->
            inScope(member.id) &&
                input.statements.any { it.ownerId == member.id } &&
                member.panForStatements.isNullOrBlank()
        }
        if (panGaps.isNotEmpty()) {
            findings += HealthFinding(
                HealthKind.MEMBER_PAN_MISSING, HealthTone.AMBER,
                if (panGaps.size == 1) "${panGaps[0].name} has no PAN on file"
                else "${panGaps.size} family members are missing a PAN",
                "CAS passwords and matching use PAN. Save it on the Family screen.",
                panGaps.size, HealthGo.FAMILY,
            )
        }

        val tone = when {
            findings.any { it.tone == HealthTone.RED } -> HealthTone.RED
            findings.any { it.tone == HealthTone.AMBER } -> HealthTone.AMBER
            else -> HealthTone.OK
        }
        val headline = when {
            tone == HealthTone.OK -> "Books look consistent"
            findings.size == 1 -> "1 thing to check"
            else -> "${findings.size} things to check"
        }
        return BooksHealth(tone, headline, findings)
    }

    fun statementSteps(rows: List<StatementSnap>, selectedMemberId: OwnerId?, today: LocalDate): List<StatementStep> {
        val inScope = { owner: OwnerId -> selectedMemberId == null || owner == selectedMemberId }
        val ordered = rows.filter { it.date != null && inScope(it.ownerId) }
            .sortedWith(compareBy({ it.date }, { it.issuer }))
        if (ordered.isEmpty()) return emptyList()
        val latest = linkedMapOf<String, StatementSnap>()
        val steps = mutableListOf<StatementStep>()
        for ((date, sameDay) in ordered.groupBy { it.date!! }.entries.sortedBy { it.key }) {
            for (row in sameDay) latest["${row.ownerId.raw}\u0000${row.issuer}"] = row
            val issuers = mutableListOf<String>()
            var value = 0L
            var invested = 0L
            for (slot in latest.values) {
                value = Math.addExact(value, slot.value.raw)
                invested = Math.addExact(invested, slot.invested.raw)
                if (slot.issuer !in issuers) issuers += slot.issuer
            }
            steps += StatementStep(date, MoneyScaled(value), MoneyScaled(invested), issuers.toList(), false)
        }
        val last = steps.last()
        if (last.date < today) {
            steps += last.copy(date = today, livePad = true)
        }
        return steps
    }

    fun investedSeries(steps: List<StatementStep>, liveInvested: MoneyScaled): List<StatementStep> {
        var carried = 0L
        return steps.map { step ->
            val raw = if (step.livePad && liveInvested.raw > 0L) liveInvested.raw else step.invested.raw
            val shown = if (raw > 0L) raw else carried
            if (shown > 0L) carried = shown
            step.copy(invested = MoneyScaled(shown))
        }
    }

    fun firstToLastPercent(values: List<Long>): Double? {
        val present = values.filter { it > 0L }
        if (present.size < 2) return null
        val first = present.first().toDouble()
        val last = present.last().toDouble()
        return (last - first) / first * 100.0
    }

    fun sinceLastStatement(input: SinceLastInput): SinceLastStatement {
        val inScope = { owner: OwnerId -> input.selectedMemberId == null || owner == input.selectedMemberId }
        val latest = linkedMapOf<HoldingId, HoldingSnap>()
        var asOf: LocalDate? = null
        for (snap in input.snaps) {
            if (!inScope(snap.ownerId)) continue
            val prev = latest[snap.holdingId]
            if (prev == null || snap.date >= prev.date) latest[snap.holdingId] = snap
            if (asOf == null || snap.date > asOf) asOf = snap.date
        }
        val marked = asOf ?: return SinceLastStatement(null, MoneyScaled(0), MoneyScaled(0), MoneyScaled(0), emptyList(), 0)
        var thenMinor = 0L
        var nowMinor = 0L
        val movers = mutableListOf<SinceMover>()
        for (snap in latest.values) {
            val holding = input.holdings.find { it.holdingId == snap.holdingId } ?: continue
            val now = marketValueOf(holding).raw
            thenMinor = Math.addExact(thenMinor, snap.value.raw)
            nowMinor = Math.addExact(nowMinor, now)
            val delta = Math.subtractExact(now, snap.value.raw)
            if (delta != 0L) {
                movers += SinceMover(holding.holdingId, displayName(holding.name), MoneyScaled(snap.value.raw), MoneyScaled(now), MoneyScaled(delta))
            }
        }
        movers.sortByDescending { kotlin.math.abs(it.delta.raw) }
        var fresh = 0
        for (holding in input.holdings) {
            if (!inScope(holding.ownerId) || holding.category == "LIABILITY") continue
            if (holding.holdingId in latest) continue
            if (openQuantity(holding) <= 0L) continue
            val firstEvent = input.events.filter { it.holdingId == holding.holdingId }.minByOrNull { it.date }?.date
            val start = holding.acquiredOn ?: firstEvent
            if (start != null && start > marked) fresh += 1
        }
        return SinceLastStatement(
            marked, MoneyScaled(thenMinor), MoneyScaled(nowMinor),
            MoneyScaled(Math.subtractExact(nowMinor, thenMinor)),
            movers.take(5), fresh,
        )
    }

    fun unknownCostRows(holdings: List<FinancialHolding>, events: List<FinancialEvent>): List<ValuedHolding> =
        holdings.filter { holding ->
            holding.category != "LIABILITY" && openQuantity(holding) > 0L &&
                CostBasis.isUnknownInvested(holding, events, investedOf(holding), marketValueOf(holding))
        }.map { ValuedHolding(it.holdingId, displayName(it.name), marketValueOf(it)) }
            .sortedByDescending { it.value.raw }

    fun displayName(name: String): String = HoldingView.displayName(name)

    private fun openQuantity(holding: FinancialHolding): Long =
        if (holding.category == "LIABILITY") {
            if (holding.unitPrice.raw > 0L) 1L else 0L
        } else holding.storedQuantityScaled

    private fun investedOf(holding: FinancialHolding): MoneyScaled {
        val cost = holding.openingCostPerUnit ?: return MoneyScaled(0)
        if (holding.storedQuantityScaled <= 0L || cost.raw <= 0L) return MoneyScaled(0)
        return MoneyScaled(timesQuantity(holding.storedQuantityScaled, cost.raw))
    }

    private fun marketValueOf(holding: FinancialHolding): MoneyScaled {
        if (holding.category == "LIABILITY") return holding.unitPrice
        if (holding.storedQuantityScaled <= 0L) return MoneyScaled(0)
        return MoneyScaled(timesQuantity(holding.storedQuantityScaled, holding.unitPrice.raw))
    }

    private fun existedBy(holding: FinancialHolding, events: List<FinancialEvent>, day: LocalDate): Boolean {
        if (holding.acquiredOn != null && holding.acquiredOn <= day) return true
        return events.any { it.holdingId == holding.holdingId && it.date <= day }
    }

    private fun percent(part: Long, whole: Long): Double {
        if (whole <= 0L) return 0.0
        return ((part.toBigInteger() * 10_000.toBigInteger()) / whole.toBigInteger()).toLong() / 100.0
    }

    private fun parseDay(iso: String): LocalDate? {
        if (!Regex("""\d{4}-\d{2}-\d{2}""").containsMatchIn(iso)) return null
        return try {
            LocalDate.parse(iso.take(10))
        } catch (_: DateTimeParseException) {
            null
        }
    }
}

enum class HealthTone { OK, AMBER, RED }
enum class HealthKind { COST_EQUALS_VALUE, STALE_STATEMENT, MISSING_FY_INTEREST, MEMBER_PAN_MISSING }
enum class HealthGo { DOCUMENTS, HOLDINGS, FAMILY }

data class HealthFinding(
    val kind: HealthKind,
    val tone: HealthTone,
    val title: String,
    val detail: String,
    val count: Int?,
    val go: HealthGo,
)

data class BooksHealth(val tone: HealthTone, val headline: String, val findings: List<HealthFinding>)

data class BooksHealthInput(
    val holdings: List<FinancialHolding>,
    val events: List<FinancialEvent>,
    val members: List<FamilyBooksMember>,
    val statements: List<StatementStamp>,
    val selectedMemberId: OwnerId?,
    val today: LocalDate,
)

data class FamilyBooksMember(val id: OwnerId, val name: String, val panForStatements: String? = null)
data class StatementStamp(val ownerId: OwnerId, val fileType: String, val periodTo: LocalDate)
data class ValuedHolding(val id: HoldingId, val name: String, val value: MoneyScaled)
data class ConcentrationRow(val id: HoldingId, val name: String, val value: MoneyScaled, val percent: Double)
data class Concentration(val rows: List<ConcentrationRow>, val topSharePercent: Double)

data class StatementSnap(
    val date: LocalDate?,
    val issuer: String,
    val ownerId: OwnerId,
    val value: MoneyScaled,
    val invested: MoneyScaled,
)

data class StatementStep(
    val date: LocalDate,
    val value: MoneyScaled,
    val invested: MoneyScaled,
    val issuers: List<String>,
    val livePad: Boolean,
)

data class HoldingSnap(val date: LocalDate, val holdingId: HoldingId, val ownerId: OwnerId, val value: MoneyScaled)
data class SinceMover(
    val id: HoldingId,
    val name: String,
    val thenValue: MoneyScaled,
    val nowValue: MoneyScaled,
    val delta: MoneyScaled,
)
data class SinceLastStatement(
    val asOf: LocalDate?,
    val thenValue: MoneyScaled,
    val nowValue: MoneyScaled,
    val delta: MoneyScaled,
    val movers: List<SinceMover>,
    val newSinceCount: Int,
)
data class SinceLastInput(
    val snaps: List<HoldingSnap>,
    val holdings: List<FinancialHolding>,
    val events: List<FinancialEvent>,
    val selectedMemberId: OwnerId?,
)

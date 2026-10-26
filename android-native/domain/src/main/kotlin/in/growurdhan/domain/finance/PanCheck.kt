package `in`.growurdhan.domain.finance

/**
 * Statement PAN checks from src/lib/pan.ts.
 * A full PAN and its masked form are the same person.
 * A statement for someone else is not saved onto the selected member.
 */
object PanCheck {
    private val full = Regex("""^[A-Z]{5}[0-9]{4}[A-Z]$""")
    private val masked = Regex("""^[A-Z]{5}\*{4}[A-Z]$""")

    fun normalize(pan: String?): String =
        (pan ?: "").replace(Regex("[^A-Za-z0-9*]"), "").uppercase()

    fun isShape(pan: String?): Boolean {
        val value = normalize(pan)
        return full.matches(value) || masked.matches(value)
    }

    /** ABCDE1234F and ABCDE****F both become ABCDE****F. */
    fun fingerprint(pan: String?): String? {
        val value = normalize(pan)
        if (!isShape(value)) return null
        return value.substring(0, 5) + "****" + value.substring(9)
    }

    fun matches(left: String?, right: String?): Boolean {
        val a = fingerprint(left)
        val b = fingerprint(right)
        return a != null && a == b
    }

    fun display(pan: String?): String = fingerprint(pan) ?: normalize(pan).ifEmpty { "not printed" }

    fun checkImport(selected: FamilyPan?, members: List<FamilyPan>, printedPan: String?): PanImport {
        val printed = normalize(printedPan)
        if (!isShape(printed)) return PanImport.NoPdfPan
        if (selected != null && memberMatches(selected, printed)) return PanImport.Match
        val other = members.find { it.id != selected?.id && memberMatches(it, printed) }
        if (other != null) return PanImport.Switch(other)
        if (selected != null && !memberHasPan(selected)) return PanImport.MemberHasNoPan(printed)
        return PanImport.Unknown(printed)
    }

    fun findDuplicate(members: List<FamilyPan>, pan: String, excludeId: String? = null): FamilyPan? {
        if (!isShape(pan)) return null
        return members.find { it.id != excludeId && memberMatches(it, pan) }
    }

    fun blocksCommit(check: PanImport): Boolean = when (check) {
        is PanImport.Switch, is PanImport.Unknown, is PanImport.MemberHasNoPan -> true
        PanImport.Match, PanImport.NoPdfPan -> false
    }

    private fun memberMatches(member: FamilyPan, printed: String): Boolean =
        matches(member.panForStatements, printed) || matches(member.panMasked, printed)

    private fun memberHasPan(member: FamilyPan): Boolean =
        fingerprint(member.panForStatements) != null || fingerprint(member.panMasked) != null
}

data class FamilyPan(
    val id: String,
    val name: String,
    val panForStatements: String? = null,
    val panMasked: String? = null,
)

sealed class PanImport {
    data object NoPdfPan : PanImport()
    data object Match : PanImport()
    data class Switch(val matched: FamilyPan) : PanImport()
    data class Unknown(val printed: String) : PanImport()
    data class MemberHasNoPan(val printed: String) : PanImport()
}

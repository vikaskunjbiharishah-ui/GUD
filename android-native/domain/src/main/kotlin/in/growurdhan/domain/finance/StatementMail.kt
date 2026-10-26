package `in`.growurdhan.domain.finance

/**
 * Which statement mail is safe to download, from src/lib/statementMail.ts.
 * CDSL and NPS put the person in the file name. NSDL, CAMS and KFintech do not.
 * Those are downloaded only when Gmail sync is on, then matched by PAN after parsing.
 * This does not open Gmail.
 */
object StatementMail {
    const val SYNC_FROM_MONTH = "2026-01"
    private val cdslName = Regex("""^([A-Za-z]{3}\d{4})_([A-Za-z0-9]{6,20})_(TXN|HLD)\.pdf$""", RegexOption.IGNORE_CASE)
    private val npsName = Regex("""^(\d{10,12})_([A-Za-z]{3}\d{4})\.pdf$""", RegexOption.IGNORE_CASE)
    private val monthLead = Regex("""^([A-Za-z]{3})(\d{4})_""", RegexOption.IGNORE_CASE)
    private val npsMonth = Regex("""^\d{10,12}_([A-Za-z]{3})(\d{4})\.pdf$""", RegexOption.IGNORE_CASE)
    private val notHoldings = Regex("""accountstatement|fatca|declaration|form\b|invoice|receipt|capitalgain|q[1-4]_20|dfp\d""", RegexOption.IGNORE_CASE)
    private val isoMonth = Regex("""^\d{4}-\d{2}""")
    private val months = mapOf(
        "jan" to "01", "feb" to "02", "mar" to "03", "apr" to "04", "may" to "05", "jun" to "06",
        "jul" to "07", "aug" to "08", "sep" to "09", "oct" to "10", "nov" to "11", "dec" to "12",
    )

    fun basename(name: String): String {
        val slash = maxOf(name.lastIndexOf('/'), name.lastIndexOf('\\'))
        return (if (slash >= 0) name.substring(slash + 1) else name).trim()
    }

    fun classify(name: String): StatementName? {
        val base = basename(name)
        cdslName.matchEntire(base)?.let {
            return StatementName("CDSL", casId = it.groupValues[2].uppercase(), variant = it.groupValues[3].uppercase())
        }
        npsName.matchEntire(base)?.let { return StatementName("NPS", pran = it.groupValues[1]) }
        return null
    }

    fun issuerFromSender(from: String): String? {
        val text = from.lowercase()
        if ("cdslstatement.com" in text) return "CDSL"
        if ("nsdl-cas@" in text || "nsdlcas.nsdl.com" in text || "@nsdl.co.in" in text) return "NSDL"
        if ("camsonline.com" in text) return "CAMS"
        if ("kfintech.com" in text) return "KFINTECH"
        if ("proteantech.in" in text) return "NPS"
        return null
    }

    fun decide(filename: String, from: String, members: List<MailMember>): MailDecision {
        val enabled = members.filter { it.gmailSync }
        if (enabled.isEmpty()) return MailDecision.Skip(false, "no member has Gmail sync on")
        val named = classify(filename)
        val sender = issuerFromSender(from)
        if (named?.issuer == "CDSL" || sender == "CDSL") {
            val casId = named?.casId
            if (casId == null) return MailDecision.Skip(false, "CDSL file name has no CAS ID")
            val owner = enabled.find { it.casId.orEmpty().uppercase() == casId }
            if (owner != null) return MailDecision.Download(owner.id)
            if (enabled.none { it.casId.orEmpty().isNotBlank() }) {
                return MailDecision.Skip(false, "import one CDSL statement by hand first")
            }
            return MailDecision.Skip(true, "CAS ID does not match a member")
        }
        if (named?.issuer == "NPS" || sender == "NPS") {
            val owner = named?.pran?.let { pran -> enabled.find { pranMatches(it.pran, pran) } }
            if (owner != null) return MailDecision.Download(owner.id)
            return MailDecision.Download(null)
        }
        if (sender == "NSDL" || sender == "CAMS" || sender == "KFINTECH") {
            if (notHoldings.containsMatchIn(basename(filename).lowercase())) {
                return MailDecision.Skip(true, "not a holdings statement")
            }
            return MailDecision.Download(null)
        }
        return MailDecision.Skip(true, "not a statement sender")
    }

    fun monthKey(name: String): String? {
        val base = basename(name)
        monthLead.find(base)?.let { match ->
            val month = months[match.groupValues[1].lowercase()] ?: return@let
            return "${match.groupValues[2]}-$month"
        }
        npsMonth.matchEntire(base)?.let { match ->
            val month = months[match.groupValues[1].lowercase()] ?: return@let
            return "${match.groupValues[2]}-$month"
        }
        return null
    }

    fun newest(files: List<NamedFile>): KeptFiles {
        val best = linkedMapOf<String, NamedFile>()
        val older = mutableListOf<NamedFile>()
        val unnamed = mutableListOf<NamedFile>()
        for (file in files) {
            val named = classify(file.filename)
            val id = when {
                named?.casId != null -> "c:${named.casId}"
                named?.pran != null -> "p:${named.pran}"
                else -> ""
            }
            if (id.isEmpty()) {
                unnamed += file
                continue
            }
            val previous = best[id]
            if (previous == null || rank(file.filename) > rank(previous.filename)) {
                if (previous != null) older += previous
                best[id] = file
            } else older += file
        }
        return KeptFiles(best.values.toList() + unnamed, older)
    }

    fun hasStatementId(member: MailMember): Boolean =
        member.casId.orEmpty().isNotBlank() || member.pran.orEmpty().isNotBlank()

    fun pranMatches(profile: String?, fileId: String?): Boolean {
        val left = profile.orEmpty().replace(Regex("\\D"), "")
        val right = fileId.orEmpty().replace(Regex("\\D"), "")
        if (left.isEmpty() || right.isEmpty()) return false
        return left == right || left.endsWith(right) || right.endsWith(left)
    }

    fun assignOwner(members: List<MailMember>, parsed: ParsedStatement, filename: String): String? {
        val enabled = members.filter { it.gmailSync }
        val named = classify(filename)
        val nps = parsed.fileType == "NPS" || named?.issuer == "NPS"
        if (nps) {
            enabled.find { pranMatches(it.pran, parsed.investorPran) }?.let { return it.id }
            enabled.find { named?.pran != null && pranMatches(it.pran, named.pran) }?.let { return it.id }
            return enabled.find { PanCheck.matches(parsed.investorPan, it.panForStatements) }?.id
        }
        if (named?.casId != null) {
            enabled.find { it.casId.orEmpty().uppercase() == named.casId }?.let { return it.id }
        }
        return enabled.find { PanCheck.matches(parsed.investorPan, it.panForStatements) }?.id
    }

    fun monthOf(periodOrName: String?): String {
        val raw = periodOrName.orEmpty().trim()
        if (isoMonth.containsMatchIn(raw) && raw.length >= 7 && isoMonth.matches(raw.take(7))) return raw.take(7)
        if (Regex("""^\d{4}-\d{2}""").containsMatchIn(raw)) return raw.take(7)
        return monthKey(raw).orEmpty()
    }

    fun onOrAfterSyncFloor(month: String): Boolean = month.isNotEmpty() && month >= SYNC_FROM_MONTH

    fun filesForMember(files: List<NamedFile>, member: MailMember): KeptFiles {
        val keep = mutableListOf<NamedFile>()
        val older = mutableListOf<NamedFile>()
        val best = linkedMapOf<String, NamedFile>()
        for (file in files) {
            val named = classify(file.filename)
            val month = monthKey(file.filename).orEmpty()
            if (named?.casId != null) {
                if (member.casId.orEmpty().uppercase() != named.casId) {
                    older += file
                    continue
                }
                if (month.isNotEmpty() && !onOrAfterSyncFloor(month)) {
                    older += file
                    continue
                }
                val cutoff = if (member.casIdSource == "import") monthOf(member.casSyncAfter) else ""
                if (cutoff.isNotEmpty() && month.isNotEmpty() && month <= cutoff) {
                    older += file
                    continue
                }
                keepBest(best, older, "c:${named.casId}", file)
                continue
            }
            if (named?.pran != null) {
                if (!pranMatches(member.pran, named.pran)) continue
                if (month.isNotEmpty() && !onOrAfterSyncFloor(month)) {
                    older += file
                    continue
                }
                val cutoff = if (member.pranSource == "import") monthOf(member.pranSyncAfter) else ""
                if (cutoff.isNotEmpty() && month.isNotEmpty() && month <= cutoff) {
                    older += file
                    continue
                }
                keepBest(best, older, "p:${named.pran}", file)
                continue
            }
            keep += file
        }
        keep += best.values
        return KeptFiles(keep, older)
    }

    private fun keepBest(best: MutableMap<String, NamedFile>, older: MutableList<NamedFile>, key: String, file: NamedFile) {
        val previous = best[key]
        if (previous == null || rank(file.filename) > rank(previous.filename)) {
            if (previous != null) older += previous
            best[key] = file
        } else older += file
    }

    private fun rank(name: String): String {
        val month = monthKey(name) ?: "0000-00"
        val txn = if (Regex("""_TXN\.pdf$""", RegexOption.IGNORE_CASE).containsMatchIn(basename(name))) "1" else "0"
        return month + txn
    }
}

data class StatementName(val issuer: String, val casId: String? = null, val pran: String? = null, val variant: String? = null)
data class MailMember(
    val id: String,
    val gmailSync: Boolean = false,
    val casId: String? = null,
    val pran: String? = null,
    val casIdSource: String? = null,
    val pranSource: String? = null,
    val casSyncAfter: String? = null,
    val pranSyncAfter: String? = null,
    val panForStatements: String? = null,
)
data class NamedFile(val filename: String)
data class ParsedStatement(val fileType: String? = null, val investorPan: String? = null, val investorPran: String? = null)
data class KeptFiles(val keep: List<NamedFile>, val older: List<NamedFile>)
sealed class MailDecision {
    data class Download(val ownerId: String?) : MailDecision()
    data class Skip(val remember: Boolean, val reason: String) : MailDecision()
}

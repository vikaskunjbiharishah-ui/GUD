package `in`.growurdhan.domain.finance

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Statement bookkeeping from src/lib/casPython.ts.
 * Passwords, dates and the last file per person. The PDF itself stays
 * with the frozen parser. This does not open a file or a network call.
 */
object CasStatements {
    private val longDate = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK)
    private val shortDate = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK)

    fun npsPassword(name: String?, dateOfBirth: String?): String? {
        if (name.isNullOrBlank() || dateOfBirth.isNullOrBlank()) return null
        val first = name.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
        val letters = first.replace(Regex("[^A-Za-z]"), "").take(4).lowercase()
        if (letters.isEmpty()) return null
        val iso = Regex("""^(\d{4})-(\d{2})-(\d{2})""").find(dateOfBirth.trim())
        val ddmm = if (iso != null) iso.groupValues[3] + iso.groupValues[2]
        else {
            val written = Regex("""^(\d{2})[/-](\d{2})[/-](\d{4})""").find(dateOfBirth.trim())
            if (written == null) return null else written.groupValues[1] + written.groupValues[2]
        }
        return letters + ddmm
    }

    /** Typed password, then the NPS convention, then the PAN. Duplicates are dropped. */
    fun passwordsToTry(typed: String? = null, pan: String? = null, name: String? = null, dateOfBirth: String? = null): List<String> {
        val out = mutableListOf<String>()
        fun add(value: String?) {
            val trimmed = value?.trim().orEmpty()
            if (trimmed.isNotEmpty() && trimmed !in out) out += trimmed
        }
        add(typed)
        add(npsPassword(name, dateOfBirth))
        add(pan)
        return out
    }

    fun parseLatest(raw: String?): CasLatest {
        if (raw.isNullOrBlank()) return CasLatest(null, emptyMap())
        val parsed = try {
            JsonValue.parse(raw)
        } catch (_: IllegalArgumentException) {
            return CasLatest(null, emptyMap())
        }
        val root = parsed as? Map<*, *> ?: return CasLatest(null, emptyMap())
        val owners = root["byOwner"] as? Map<*, *> ?: return CasLatest(null, emptyMap())
        val byOwner = owners.mapNotNull { (owner, files) ->
            val id = owner as? String ?: return@mapNotNull null
            val pack = files as? Map<*, *> ?: return@mapNotNull null
            id to pack.mapNotNull { (type, meta) ->
                val key = type as? String ?: return@mapNotNull null
                val row = meta as? Map<*, *> ?: return@mapNotNull null
                key to stamp(key, row)
            }.toMap()
        }.toMap()
        return CasLatest(root["asOf"] as? String, byOwner)
    }

    fun upsert(previous: CasLatest, ownerId: String, meta: CasFileStamp, importedAt: String): CasLatest {
        val owners = previous.byOwner.toMutableMap()
        val files = (owners[ownerId] ?: emptyMap()).toMutableMap()
        files[meta.fileType] = meta.copy(importedAt = meta.importedAt ?: importedAt)
        owners[ownerId] = files
        var asOf: String? = null
        for (pack in owners.values) {
            for (file in pack.values) {
                val period = file.periodTo ?: continue
                if (asOf == null || period > asOf) asOf = period
            }
        }
        return CasLatest(asOf, owners)
    }

    fun latestForOwner(state: CasLatest, ownerId: String): String? {
        val pack = state.byOwner[ownerId] ?: return null
        return pack.values.mapNotNull { it.periodTo }.maxOrNull()
    }

    fun asOfByMember(state: CasLatest, members: List<StatementMember>): List<MemberStatement> =
        members.mapNotNull { member ->
            val asOf = latestForOwner(state, member.id) ?: return@mapNotNull null
            MemberStatement(member.id, member.name, member.relation, asOf)
        }

    fun formatDate(iso: String?): String {
        if (iso.isNullOrEmpty()) return ""
        val day = Regex("""^(\d{4})-(\d{2})-(\d{2})""").find(iso) ?: return iso
        return LocalDate.parse(day.value).format(longDate)
    }

    fun formatDateShort(iso: String?): String {
        if (iso.isNullOrEmpty()) return ""
        val day = Regex("""^(\d{4})-(\d{2})-(\d{2})""").find(iso) ?: return iso
        return LocalDate.parse(day.value).format(shortDate)
    }

    fun formatInr(amount: Double): String {
        if (!amount.isFinite()) return "₹ 0.00"
        val scaled = BigDecimal.valueOf(kotlin.math.abs(amount)).setScale(2, RoundingMode.HALF_UP)
        val text = scaled.toPlainString()
        val whole = text.substringBefore('.')
        val frac = text.substringAfter('.')
        val grouped = groupIndian(whole)
        return "${if (amount < 0) "-" else ""}₹ $grouped.$frac"
    }

    private fun stamp(type: String, row: Map<*, *>): CasFileStamp = CasFileStamp(
        fileType = row["fileType"] as? String ?: type,
        filename = row["filename"] as? String ?: "",
        periodTo = row["periodTo"] as? String,
        pdfGrandTotal = (row["pdfGrandTotal"] as? Number)?.toDouble(),
        parsedGrandTotal = (row["parsedGrandTotal"] as? Number)?.toDouble(),
        complete = row["complete"] == true,
        importedAt = row["importedAt"] as? String,
    )

    private fun groupIndian(digits: String): String {
        if (digits.length <= 3) return digits
        val lastThree = digits.takeLast(3)
        val head = digits.dropLast(3)
        return head.reversed().chunked(2).joinToString(",").reversed() + "," + lastThree
    }
}

data class CasFileStamp(
    val fileType: String,
    val filename: String,
    val periodTo: String?,
    val pdfGrandTotal: Double? = null,
    val parsedGrandTotal: Double? = null,
    val complete: Boolean = false,
    val importedAt: String? = null,
)

data class CasLatest(val asOf: String?, val byOwner: Map<String, Map<String, CasFileStamp>>)
data class StatementMember(val id: String, val name: String, val relation: String? = null)
data class MemberStatement(val id: String, val name: String, val relation: String?, val asOf: String)

/** Just enough JSON for the stored statement book. Not a general parser. */
internal object JsonValue {
    fun parse(raw: String): Any? = Reader(raw).parse()

    private class Reader(val text: String) {
        var index = 0
        fun parse(): Any? {
            skip()
            if (index >= text.length) throw IllegalArgumentException("Empty JSON.")
            return when (text[index]) {
                '{' -> objectValue()
                '[' -> arrayValue()
                '"' -> stringValue()
                't' -> literal("true", true)
                'f' -> literal("false", false)
                'n' -> literal("null", null)
                else -> numberValue()
            }
        }
        private fun objectValue(): Map<String, Any?> {
            index++
            val out = linkedMapOf<String, Any?>()
            skip()
            if (peek('}')) return out
            while (true) {
                skip()
                val key = stringValue()
                skip()
                expect(':')
                out[key] = parse()
                skip()
                if (peek('}')) return out
                expect(',')
            }
        }
        private fun arrayValue(): List<Any?> {
            index++
            val out = mutableListOf<Any?>()
            skip()
            if (peek(']')) return out
            while (true) {
                out += parse()
                skip()
                if (peek(']')) return out
                expect(',')
            }
        }
        private fun stringValue(): String {
            expect('"')
            val out = StringBuilder()
            while (index < text.length) {
                val char = text[index++]
                when (char) {
                    '"' -> return out.toString()
                    '\\' -> out.append(escape())
                    else -> out.append(char)
                }
            }
            throw IllegalArgumentException("Unclosed string.")
        }
        private fun escape(): Char {
            if (index >= text.length) throw IllegalArgumentException("Bad escape.")
            return when (val char = text[index++]) {
                '"', '\\', '/' -> char
                'b' -> '\b'
                'f' -> '\u000C'
                'n' -> '\n'
                'r' -> '\r'
                't' -> '\t'
                'u' -> text.substring(index, index + 4).also { index += 4 }.toInt(16).toChar()
                else -> throw IllegalArgumentException("Bad escape.")
            }
        }
        private fun numberValue(): Number {
            val start = index
            if (peek('-')) index++
            while (index < text.length && text[index].isDigit()) index++
            if (index < text.length && text[index] == '.') {
                index++
                while (index < text.length && text[index].isDigit()) index++
            }
            val token = text.substring(start, index)
            return if ('.' in token) token.toDouble() else token.toLong()
        }
        private fun literal(word: String, value: Any?): Any? {
            if (!text.startsWith(word, index)) throw IllegalArgumentException("Bad JSON.")
            index += word.length
            return value
        }
        private fun skip() {
            while (index < text.length && text[index].isWhitespace()) index++
        }
        private fun peek(char: Char): Boolean {
            if (index < text.length && text[index] == char) {
                index++
                return true
            }
            return false
        }
        private fun expect(char: Char) {
            if (!peek(char)) throw IllegalArgumentException("Expected $char.")
        }
    }
}

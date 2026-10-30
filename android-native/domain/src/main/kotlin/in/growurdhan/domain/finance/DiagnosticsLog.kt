package `in`.growurdhan.domain.finance

/**
 * Opt-in local notes. Off by default. PAN, email, tokens and PDF-looking
 * text are dropped. Nothing here is sent off the phone.
 */
object DiagnosticsLog {
    const val REDACTED = "[redacted]"
    private val pan = Regex("[A-Z]{5}[0-9]{4}[A-Z]")
    private val email = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+")

    fun record(enabled: Boolean, existing: List<String>, line: String): List<String> {
        if (!enabled) return existing
        val clean = redact(line) ?: return existing
        return existing + clean
    }

    fun clear(): List<String> = emptyList()

    fun redact(line: String): String? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.contains("pdf", ignoreCase = true) || trimmed.contains("BEGIN ")) return null
        val cleaned = email.replace(pan.replace(trimmed, REDACTED), REDACTED)
        if (cleaned.contains("token", ignoreCase = true) || cleaned.contains("password", ignoreCase = true)) {
            return null
        }
        return cleaned.take(200)
    }
}

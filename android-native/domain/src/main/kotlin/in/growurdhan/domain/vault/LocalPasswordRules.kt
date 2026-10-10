package `in`.growurdhan.domain.vault

/** Original GrowUrDhan local password rules, without a cloud identity service. */
object LocalPasswordRules {
    const val MIN_LENGTH = 8
    const val MAX_LENGTH = 128
    const val ROTATION_DAYS = 180L
    const val KDF_ITERATIONS = 210_000

    fun issues(password: CharArray): List<String> = buildList {
        if (password.size < MIN_LENGTH) add("Use at least eight characters.")
        if (password.size > MAX_LENGTH) add("Password exceeds 128 characters.")
        if (password.none { it.isUpperCase() }) add("Include an uppercase letter.")
        if (password.none { it.isLowerCase() }) add("Include a lowercase letter.")
        if (password.none { it in '0'..'9' }) add("Include a digit.")
        if (password.none { !it.isLetterOrDigit() }) add("Include a special character.")
        if (password.any { it.isISOControl() }) add("Control characters are not supported.")
    }

    fun requiresRotation(changedAtEpochMillis: Long, nowEpochMillis: Long): Boolean {
        if (changedAtEpochMillis <= 0L || nowEpochMillis < changedAtEpochMillis) return true
        return (nowEpochMillis - changedAtEpochMillis) >= ROTATION_DAYS * 86_400_000L
    }
}

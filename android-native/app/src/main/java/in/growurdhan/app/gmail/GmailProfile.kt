package `in`.growurdhan.app.gmail

/**
 * Public Gmail profile response only. The access token is a header, never
 * part of the URL, and this object does not open a socket.
 */
internal object GmailProfile {
    const val PROFILE_URL = "https://gmail.googleapis.com/gmail/v1/users/me/profile"
    private val emailField = Regex(""""emailAddress"\s*:\s*"([^"\\]{3,254})"""")

    fun emailAddress(body: String): String {
        require(body.length in 1..8192) { "Gmail profile is empty or too large." }
        require(!body.contains("<html", ignoreCase = true)) { "Gmail profile was HTML." }
        val email = emailField.find(body)?.groupValues?.get(1)
            ?: throw IllegalArgumentException("Gmail profile has no email.")
        require(email.count { it == '@' } == 1 &&
            email.none { it.isISOControl() || it.isWhitespace() }) {
            "Gmail profile email is not usable."
        }
        return email
    }

    fun readEmail(token: String, get: (url: String, bearer: String) -> String): String {
        require(token.length in 1..4096 && token.none { it.isISOControl() }) {
            "Gmail access token is missing or unsafe."
        }
        require(!PROFILE_URL.contains(token) && !PROFILE_URL.contains('?')) {
            "Gmail profile URL must not carry the token."
        }
        return emailAddress(get(PROFILE_URL, token))
    }
}

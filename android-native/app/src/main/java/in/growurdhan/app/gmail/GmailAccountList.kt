package `in`.growurdhan.app.gmail

/** Connected inboxes only. The access token is never written down. */
internal object GmailAccountList {
    fun remember(current: List<String>, email: String): List<String> {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return current
        return (current + trimmed).distinctBy { it.lowercase() }
    }

    fun forget(current: List<String>, email: String): List<String> =
        current.filter { !it.equals(email.trim(), ignoreCase = true) }
}

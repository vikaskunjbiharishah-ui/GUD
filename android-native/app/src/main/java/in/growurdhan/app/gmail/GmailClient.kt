package `in`.growurdhan.app.gmail

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.net.HttpURLConnection
import java.net.URL

/**
 * Remembers which Gmail inboxes were allowed to be read.
 * The OAuth access token stays in memory for the profile call only.
 */
class GmailClient(private val context: Context) {
    private fun prefs() = EncryptedSharedPreferences.create(
        context,
        "gud_gmail",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun emails(): List<String> {
        val raw = prefs().getString("emails", "") ?: ""
        return raw.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun remember(email: String) {
        val next = GmailAccountList.remember(emails(), email)
        prefs().edit().putString("emails", next.joinToString("\n")).apply()
    }

    fun forget(email: String) {
        val next = GmailAccountList.forget(emails(), email)
        prefs().edit().putString("emails", next.joinToString("\n")).apply()
    }

    fun profileEmail(token: String): String = GmailProfile.readEmail(token, ::httpGet)

    private fun httpGet(url: String, token: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.connectTimeout = 15000
        conn.readTimeout = 20000
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) {
            throw IllegalStateException("Gmail refused the request ($code).")
        }
        return text
    }
}

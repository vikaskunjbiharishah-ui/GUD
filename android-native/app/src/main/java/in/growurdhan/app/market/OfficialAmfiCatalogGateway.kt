package `in`.growurdhan.app.market

import android.content.Context
import android.util.AtomicFile
import `in`.growurdhan.domain.market.CachedListedSecurity
import `in`.growurdhan.domain.market.OfficialAmfiNavParser
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.time.LocalDate
import javax.net.ssl.HttpsURLConnection

/**
 * Public market-data only. The request NEVER carries wealth holdings, ISINs,
 * PAN, PRAN, login credentials, a family ID or app telemetry.
 * Refresh is strictly an explicit user action, never a background sync.
 *
 * AMFI has announced a new report format; this legacy text feed may disappear.
 * Unknown HTML/format errors fail closed and preserve the last validated cache.
 */
class OfficialAmfiCatalogGateway(private val context: Context) {
    private val cacheFile: File
        get() = File(context.noBackupFilesDir, "amfi-public-nav-cache.txt")

    fun cached(today: LocalDate = LocalDate.now()): List<CachedListedSecurity> =
        runCatching {
            if (cacheFile.isFile && cacheFile.length() in 1..MAX_BYTES.toLong()) {
                OfficialAmfiNavParser.parse(cacheFile.readText(Charsets.UTF_8), today)
            } else emptyList()
        }.getOrDefault(emptyList())

    /** Run only on Dispatchers.IO; caller must never block Compose's UI thread. */
    fun refresh(today: LocalDate = LocalDate.now()): List<CachedListedSecurity> {
        val url = URL("https://portal.amfiindia.com/spages/NAVAll.txt")
        require(url.protocol == "https" && url.host == "portal.amfiindia.com" &&
            url.path == "/spages/NAVAll.txt" && url.query == null)
        val connection = (url.openConnection() as HttpsURLConnection)
        try {
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 9_000
            connection.readTimeout = 12_000
            connection.useCaches = false
            connection.setRequestProperty("Accept", "text/plain")
            require(connection.responseCode == 200) {
                "The official AMFI report is unavailable."
            }
            val bytes = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buf = ByteArray(16_384)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    require(bytes.size() + n <= MAX_BYTES) {
                        "AMFI response exceeded the public catalog size limit."
                    }
                    bytes.write(buf, 0, n)
                }
            }
            val raw = bytes.toByteArray()
            val decoded = raw.toString(Charsets.UTF_8)
            val validated = OfficialAmfiNavParser.parse(decoded, today)
            // Only validated, public, non-sensitive reports are cached.
            val atomic = AtomicFile(cacheFile)
            val output = atomic.startWrite()
            try {
                output.write(raw)
                atomic.finishWrite(output)
            } catch (failure: Exception) {
                atomic.failWrite(output)
                throw failure
            }
            return validated
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val MAX_BYTES = 8_000_000
    }
}

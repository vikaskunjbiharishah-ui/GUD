package `in`.growurdhan.app.market

import android.content.Context
import android.util.AtomicFile
import `in`.growurdhan.domain.market.CachedListedSecurity
import `in`.growurdhan.domain.market.OfficialBhavcopyParser
import `in`.growurdhan.domain.market.OfficialMarketSource
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream
import javax.net.ssl.HttpsURLConnection

/**
 * Public NSE/BSE close files only. The request is a dated bhavcopy URL with
 * no ISIN, holding, PAN or family id. A failed refresh leaves the last
 * validated cache in place.
 */
class OfficialExchangeCloseGateway(
    private val context: Context,
    private val fetch: (url: String, referer: String) -> ByteArray? = ::download,
) {
    fun cached(source: OfficialMarketSource, today: LocalDate = LocalDate.now()): List<CachedListedSecurity> =
        runCatching {
            val file = cacheFile(source)
            if (!file.isFile || file.length() !in 1..MAX_BYTES.toLong()) return emptyList()
            OfficialBhavcopyParser.parse(file.readText(Charsets.UTF_8), source, today).rows
        }.getOrDefault(emptyList())

    /** Run only on Dispatchers.IO. */
    fun refresh(source: OfficialMarketSource, today: LocalDate = LocalDate.now(INDIA)): List<CachedListedSecurity> {
        require(source == OfficialMarketSource.NSE || source == OfficialMarketSource.BSE)
        var last = "not published for the last sessions"
        for (ago in 0 until 6) {
            val day = today.minusDays(ago.toLong())
            val url = urlFor(source, day)
            requireOfficial(url, source)
            val bytes = try {
                fetch(url, referer(source))
            } catch (failure: Exception) {
                last = failure.message ?: last
                continue
            } ?: continue
            val csv = try {
                if (source == OfficialMarketSource.NSE) unzipCsv(bytes) else bytes.toString(Charsets.UTF_8)
            } catch (_: Exception) {
                continue
            }
            if (csv.isBlank() || csv.trimStart().startsWith("<")) continue
            val parsed = runCatching { OfficialBhavcopyParser.parse(csv, source, today) }.getOrNull()
                ?: continue
            if (!parsed.understood) continue
            if (parsed.rows.isEmpty()) {
                throw IllegalStateException("Official $source file had no usable closes.")
            }
            write(cacheFile(source), csv)
            return parsed.rows
        }
        throw IllegalStateException(last)
    }

    private fun cacheFile(source: OfficialMarketSource) =
        File(context.noBackupFilesDir, "exchange-close-${source.name}.csv")

    private fun write(file: File, csv: String) {
        val atomic = AtomicFile(file)
        val output = atomic.startWrite()
        try {
            output.write(csv.toByteArray(Charsets.UTF_8))
            atomic.finishWrite(output)
        } catch (failure: Exception) {
            atomic.failWrite(output)
            throw failure
        }
    }

    companion object {
        private const val MAX_BYTES = 8_000_000
        private val INDIA: ZoneId = ZoneId.of("Asia/Kolkata")
        private val ymd: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE

        fun urlFor(source: OfficialMarketSource, day: LocalDate): String {
            val stamp = day.format(ymd)
            return when (source) {
                OfficialMarketSource.NSE ->
                    "https://nsearchives.nseindia.com/content/cm/BhavCopy_NSE_CM_0_0_0_${stamp}_F_0000.csv.zip"
                OfficialMarketSource.BSE ->
                    "https://www.bseindia.com/download/BhavCopy/Equity/BhavCopy_BSE_CM_0_0_0_${stamp}_F_0000.CSV"
                else -> throw IllegalArgumentException("AMFI is a separate gateway.")
            }
        }

        private fun referer(source: OfficialMarketSource) = when (source) {
            OfficialMarketSource.NSE -> "https://www.nseindia.com/all-reports"
            else -> "https://www.bseindia.com/"
        }

        private fun requireOfficial(url: String, source: OfficialMarketSource) {
            val parsed = URL(url)
            require(parsed.protocol == "https" && parsed.query == null && parsed.userInfo == null)
            when (source) {
                OfficialMarketSource.NSE -> require(
                    parsed.host == "nsearchives.nseindia.com" &&
                        parsed.path.startsWith("/content/cm/BhavCopy_NSE_CM_0_0_0_") &&
                        parsed.path.endsWith("_F_0000.csv.zip"),
                )
                OfficialMarketSource.BSE -> require(
                    parsed.host == "www.bseindia.com" &&
                        parsed.path.startsWith("/download/BhavCopy/Equity/BhavCopy_BSE_CM_0_0_0_") &&
                        parsed.path.endsWith("_F_0000.CSV"),
                )
                else -> throw IllegalArgumentException("AMFI is a separate gateway.")
            }
        }

        private fun download(url: String, referer: String): ByteArray? {
            val connection = (URL(url).openConnection() as HttpsURLConnection)
            try {
                connection.requestMethod = "GET"
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 9_000
                connection.readTimeout = 12_000
                connection.useCaches = false
                connection.setRequestProperty("Accept", "text/csv,application/zip")
                connection.setRequestProperty("Referer", referer)
                if (connection.responseCode == 404) return null
                require(connection.responseCode == 200) { "Official close file is unavailable." }
                val bytes = ByteArrayOutputStream()
                connection.inputStream.use { input ->
                    val buf = ByteArray(16_384)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        require(bytes.size() + n <= MAX_BYTES) { "Close file exceeded the size limit." }
                        bytes.write(buf, 0, n)
                    }
                }
                return bytes.toByteArray()
            } finally {
                connection.disconnect()
            }
        }

        private fun unzipCsv(bytes: ByteArray): String {
            require(bytes.size >= 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte())
            ZipInputStream(bytes.inputStream()).use { zip ->
                val entry = zip.nextEntry ?: throw IllegalArgumentException("Empty zip.")
                require(!entry.isDirectory)
                val out = ByteArrayOutputStream()
                val buf = ByteArray(16_384)
                while (true) {
                    val n = zip.read(buf)
                    if (n < 0) break
                    require(out.size() + n <= MAX_BYTES) { "Unzipped bhavcopy exceeded the size limit." }
                    out.write(buf, 0, n)
                }
                return out.toString(Charsets.UTF_8)
            }
        }
    }
}

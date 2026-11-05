package `in`.growurdhan.app.cas

/** True only when the on-device Python runtime is packaged. No parser is invented. */
object CasParserAvailability {
    fun packaged(): Boolean = try {
        Class.forName("com.chaquo.python.Python")
        true
    } catch (_: Throwable) {
        false
    }
}
